import { useEffect, useState } from "react";
import { openUrl } from "@tauri-apps/plugin-opener";
import type { AppStatus } from "../types";
import {
  clearCombatTextInstalled,
  clearCombatTextResources,
  downloadCombatTextProfiles,
  installCombatTextProfile,
  openCombatTextItemFolder,
  openCombatTextResourcesFolder
} from "../backendConnector/api.ts";

type CombatTextManagerProps = {
  status: AppStatus | null;
  loading: boolean;
  onBusyChange: (busy: boolean) => void;
  onStatusRefresh: () => Promise<void>;
  onMessage: (message: string) => void;
};

export function CombatTextManager({
  status,
  loading,
  onBusyChange,
  onStatusRefresh,
  onMessage
}: CombatTextManagerProps) {
  const [collapsed, setCollapsed] = useState(true);
  const [selectedProfile, setSelectedProfile] = useState("");
  const availableProfiles = status?.combatTextAvailableProfiles ?? [];
  const canInstall = Boolean(selectedProfile);
  const selectedProfileData = availableProfiles.find((profile) => profile.id === selectedProfile) ?? null;
  const installedProfileUrl = status?.combatTextInstalledProfile?.url ?? null;
  const activeProfileName = status?.combatTextInstalledProfile?.name ?? "No active profile";
  const activeProfileAuthor = status?.combatTextInstalledProfile?.author
    ? ` • by ${status.combatTextInstalledProfile.author}`
    : "";

  useEffect(() => {
    if (availableProfiles.length === 0) {
      if (selectedProfile) {
        setSelectedProfile("");
      }
      return;
    }
    const selectedStillExists = availableProfiles.some((profile) => profile.id === selectedProfile);
    if (!selectedStillExists) {
      setSelectedProfile(availableProfiles[0].id);
    }
  }, [availableProfiles, selectedProfile]);

  function toErrorMessage(err: unknown, fallback: string) {
    if (err instanceof Error && err.message) {
      return err.message;
    }
    if (typeof err === "string" && err.trim()) {
      return err;
    }
    if (err && typeof err === "object" && "message" in err && typeof err.message === "string") {
      return err.message;
    }
    return fallback;
  }

  async function runAction(action: () => Promise<unknown>, successMessage?: string) {
    onBusyChange(true);
    try {
      await action();
      await onStatusRefresh();
      if (successMessage) {
        onMessage(successMessage);
      }
    } catch (err) {
      onMessage(toErrorMessage(err, "Request failed."));
    } finally {
      onBusyChange(false);
    }
  }

  async function onInstallProfile() {
    if (!selectedProfile) return;
    if (!status?.combatTextSelectedGameItemFolder) {
      onMessage("Set game installation folder first (must point to a base containing 3ddata/item).");
      return;
    }
    const confirmed = window.confirm(
      `Install combat text profile "${selectedProfile}"? This will clear current installed models first.`
    );
    if (!confirmed) return;
    await runAction(
      () => installCombatTextProfile(selectedProfile),
      `Installed combat text profile: ${selectedProfile}.`
    );
  }

  async function onClearInstalled() {
    const confirmed = window.confirm("Clear all installed combat text models from 3ddata/item?");
    if (!confirmed) return;
    await runAction(clearCombatTextInstalled, "Installed combat text models cleared.");
  }

  async function onOpenInstalledProfile(url: string) {
    try {
      await openUrl(url);
    } catch (err) {
      onMessage(toErrorMessage(err, "Failed to open profile link."));
    }
  }

  return (
    <section className="lootManager">
      <div className="lootAccordion">
        <div className="accordionHeader">
          <div>
            <p className="sectionTitle">Combat text</p>
            <p className="activeProfileMeta">
              Active profile: <span className="activeProfileValue">{activeProfileName}{activeProfileAuthor}</span>
            </p>
          </div>
          <div className="headerActions">
            {installedProfileUrl ? (
              <button
                type="button"
                className="iconBtn iconBtnSubtle"
                disabled={loading}
                onClick={() => onOpenInstalledProfile(installedProfileUrl)}
                title="Visit author's profile"
                aria-label="Visit author's profile"
              >
                🔗
              </button>
            ) : null}
            <button
              type="button"
              className="iconBtn iconBtnSubtle"
              disabled={loading}
              onClick={() => runAction(openCombatTextItemFolder)}
              title="Open installed"
              aria-label="Open installed"
            >
              📁
            </button>
            <button
              type="button"
              className="iconBtn iconBtnSubtle"
              disabled={loading}
              onClick={onClearInstalled}
              title="Clear installed"
              aria-label="Clear installed"
            >
              🗑
            </button>
            <button
              type="button"
              className="iconBtn iconBtnToggle"
              aria-label={collapsed ? "Expand Combat text" : "Collapse Combat text"}
              aria-expanded={!collapsed}
              onClick={() => setCollapsed((value) => !value)}
            >
              {collapsed ? "▾" : "▴"}
            </button>
          </div>
        </div>

        {!collapsed ? (
          <div className="accordionBody">
            <div className="accordionSection">
              <p className="sectionTitle">1) Download</p>
              <p>Download profiles first. Downloaded: {status?.combatTextDownloadedProfiles.length ?? 0}</p>
              <div className="headerActions">
                <button
                  type="button"
                  className="iconBtn iconBtnSubtle"
                  disabled={loading}
                  onClick={() => runAction(downloadCombatTextProfiles, "Combat text profiles downloaded.")}
                  title="Download profiles"
                  aria-label="Download profiles"
                >
                  ⬇
                </button>
                <button
                  type="button"
                  className="iconBtn iconBtnSubtle"
                  disabled={loading}
                  onClick={() => runAction(openCombatTextResourcesFolder)}
                  title="Open downloaded"
                  aria-label="Open downloaded"
                >
                  📂
                </button>
                <button
                  type="button"
                  className="iconBtn iconBtnSubtle"
                  disabled={loading}
                  onClick={() => runAction(clearCombatTextResources, "Downloaded combat text resources cleared.")}
                  title="Clear downloaded"
                  aria-label="Clear downloaded"
                >
                  🗑
                </button>
              </div>
            </div>

            <div className="accordionSection">
              <p className="sectionTitle">2) Install</p>
              <p>Pick one downloaded profile and install it.</p>
              <div className="compactRow">
                <select
                  disabled={loading || availableProfiles.length === 0}
                  value={selectedProfile}
                  onChange={(event) => setSelectedProfile(event.target.value)}
                >
                  {availableProfiles.map((profile) => (
                    <option key={profile.id} value={profile.id}>
                      {profile.id}
                    </option>
                  ))}
                </select>
                <button className="buttonStrong" disabled={loading || !canInstall} onClick={onInstallProfile}>
                  Install
                </button>
              </div>
              <p>
                {selectedProfileData
                  ? `${selectedProfileData.name ?? selectedProfileData.id}${
                      selectedProfileData.author ? ` • by ${selectedProfileData.author}` : ""
                    }${selectedProfileData.createdAt ? ` • ${selectedProfileData.createdAt}` : ""}`
                  : "No profile selected"}
              </p>
              <p>{selectedProfileData?.description ?? "-"}</p>
            </div>
          </div>
        ) : null}
      </div>
    </section>
  );
}
