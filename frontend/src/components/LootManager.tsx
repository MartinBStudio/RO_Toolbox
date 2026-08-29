import { useEffect, useState } from "react";
import { openUrl } from "@tauri-apps/plugin-opener";
import type { AppStatus } from "../types";
import {
  checkLootResourcesUpdate,
  clearInstalled,
  clearResources,
  downloadProfiles,
  installProfile,
  openItemFolder,
  openResourcesFolder
} from "../backendConnector/api.ts";
import { useApplicationContext } from "../context/ApplicationContext.tsx";
import { capitalizeFirstLetter } from "../formatting.ts";

type LootManagerProps = {
  status: AppStatus | null;
  loading: boolean;
  onBusyChange: (busy: boolean) => void;
  onStatusRefresh: () => Promise<void>;
  onMessage: (message: string) => void;
};

export function LootManager({
  status,
  loading,
  onBusyChange,
  onStatusRefresh,
  onMessage
}: LootManagerProps) {
  const { backendReady } = useApplicationContext();
  const [collapsed, setCollapsed] = useState(true);
  const [selectedProfile, setSelectedProfile] = useState("");
  const [resourcesUpdateAvailable, setResourcesUpdateAvailable] = useState(false);
  const [resourcesUpdateChecking, setResourcesUpdateChecking] = useState(false);
  const [resourcesUpdateVersion, setResourcesUpdateVersion] = useState<string | undefined>(undefined);
  const availableProfiles = status?.availableProfiles ?? [];
  const canInstall = Boolean(selectedProfile);
  const selectedProfileData = availableProfiles.find((profile) => profile.id === selectedProfile) ?? null;
  const installedProfileUrl = status?.installedProfile?.url ?? null;
  const activeProfileName = capitalizeFirstLetter(status?.installedProfile?.name) ?? "No active profile";
  const activeProfileAuthor = status?.installedProfile?.author
    ? ` • by ${status.installedProfile.author}`
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

  useEffect(() => {
    if (!backendReady) return;
    checkResourcesUpdate();
  }, [backendReady]);

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

  async function checkResourcesUpdate() {
    setResourcesUpdateChecking(true);
    try {
      const result = await checkLootResourcesUpdate();
      if (result.success && result.updateAvailable) {
        setResourcesUpdateAvailable(true);
        setResourcesUpdateVersion(result.remoteVersion);
      } else {
        setResourcesUpdateAvailable(false);
        setResourcesUpdateVersion(undefined);
      }
    } catch (_err) {
      setResourcesUpdateAvailable(false);
      setResourcesUpdateVersion(undefined);
    } finally {
      setResourcesUpdateChecking(false);
    }
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

  async function onResourcesUpdateAction() {
    if (resourcesUpdateAvailable) {
      await runAction(downloadProfiles, "Profiles downloaded.");
      setResourcesUpdateAvailable(false);
      setResourcesUpdateVersion(undefined);
    } else {
      setResourcesUpdateChecking(true);
      try {
        const result = await checkLootResourcesUpdate();
        if (result.success && result.updateAvailable) {
          setResourcesUpdateAvailable(true);
          setResourcesUpdateVersion(result.remoteVersion);
        } else if (result.success) {
          setResourcesUpdateAvailable(false);
          setResourcesUpdateVersion(undefined);
          onMessage("Loot models are up to date.");
        } else {
          onMessage(result.message || "Update check failed.");
        }
      } catch (err) {
        onMessage(toErrorMessage(err, "Update check failed."));
      } finally {
        setResourcesUpdateChecking(false);
      }
    }
  }

  async function onInstallProfile() {
    if (!selectedProfile) return;
    if (!status?.selectedGameItemFolder) {
      onMessage("Set game installation folder first (must point to a base containing 3ddata/item).");
      return;
    }
    const confirmed = window.confirm(
      `Install profile "${selectedProfile}"? This will clear current installed models first.`
    );
    if (!confirmed) return;
    await runAction(() => installProfile(selectedProfile), `Installed profile: ${selectedProfile}.`);
  }

  async function onClearInstalled() {
    const confirmed = window.confirm("Clear all installed models from 3ddata/item?");
    if (!confirmed) return;
    await runAction(clearInstalled, "Installed models cleared.");
  }

  async function onOpenInstalledProfile(url: string) {
    try {
      await openUrl(url);
    } catch (err) {
      onMessage(toErrorMessage(err, "Failed to open profile link."));
    }
  }

  const resourcesUpdateTitle = resourcesUpdateAvailable
    ? `Download loot models update${resourcesUpdateVersion ? ` v${resourcesUpdateVersion}` : ""}`
    : "Check for loot models updates";
  const hasProfiles = (status?.downloadedProfiles.length ?? 0) > 0;

  return (
    <section className="lootManager">
      <div className="lootAccordion">
        <div className="accordionHeader">
          <div>
            <p className="sectionTitle">Loot models</p>
            <p className="activeProfileMeta">
              Active profile: <span className="activeProfileValue">{activeProfileName}{activeProfileAuthor}</span>
            </p>
          </div>
          <div className="headerActions">
            {installedProfileUrl ? (
              <button
                type="button"
                className="iconBtn iconBtnSubtle iconBtnDim"
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
              className="iconBtn iconBtnSubtle iconBtnDim"
              disabled={loading}
              onClick={() => runAction(openResourcesFolder)}
              title="Browse downloaded"
              aria-label="Browse downloaded"
            >
              📂
            </button>
            <button
              type="button"
              className="iconBtn iconBtnSubtle iconBtnDim"
              disabled={loading}
              onClick={() => runAction(clearResources, "Downloaded resources cleared.")}
              title="Clear downloaded"
              aria-label="Clear downloaded"
            >
              🗑
            </button>
            <button
              type="button"
              className="iconBtn iconBtnSubtle iconBtnDim"
              disabled={loading}
              onClick={() => runAction(openItemFolder)}
              title="Browse installed"
              aria-label="Browse installed"
            >
              📁
            </button>
            <button
              type="button"
              className="iconBtn iconBtnSubtle iconBtnDim"
              disabled={loading}
              onClick={onClearInstalled}
              title="Clear installed"
              aria-label="Clear installed"
            >
              🗑
            </button>
            <span className="headerSep" />
            <button
              type="button"
              className={`iconBtn updateCog${resourcesUpdateAvailable ? " updateAvailable" : ""}`}
              disabled={loading || resourcesUpdateChecking}
              onClick={onResourcesUpdateAction}
              title={resourcesUpdateTitle}
              aria-label={resourcesUpdateTitle}
            >
              {resourcesUpdateAvailable ? "↓" : "⟳"}
            </button>
            <button
              type="button"
              className="iconBtn iconBtnToggle"
              aria-label={collapsed ? "Expand Loot models" : "Collapse Loot models"}
              aria-expanded={!collapsed}
              disabled={loading || !hasProfiles}
              title={!hasProfiles ? "Download profiles first" : undefined}
              onClick={() => setCollapsed((value) => !value)}
            >
              {collapsed ? "▾" : "▴"}
            </button>
          </div>
        </div>

        {!collapsed ? (
          <div className="accordionBody">
            <div className="accordionSection">
              <div className="profilePickerRow">
                <select
                  disabled={loading || availableProfiles.length === 0}
                  value={selectedProfile}
                  onChange={(event) => setSelectedProfile(event.target.value)}
                >
                  {availableProfiles.map((profile) => (
                    <option key={profile.id} value={profile.id}>
                      {capitalizeFirstLetter(profile.name ?? profile.id)}
                    </option>
                  ))}
                </select>
                <button className="buttonStrong" disabled={loading || !canInstall} onClick={onInstallProfile}>
                  Install
                </button>
              </div>
              {selectedProfileData ? (
                <div className="profileCard">
                  <div className="profileCardHeader">
                    <div>
                      <p className="profileCardName">{capitalizeFirstLetter(selectedProfileData.name ?? selectedProfileData.id)}</p>
                      {(selectedProfileData.author || selectedProfileData.createdAt) && (
                        <p className="profileCardMeta">
                          {selectedProfileData.author ? `by ${selectedProfileData.author}` : ""}
                          {selectedProfileData.author && selectedProfileData.createdAt ? " · " : ""}
                          {selectedProfileData.createdAt ?? ""}
                        </p>
                      )}
                    </div>
                    {selectedProfileData.url ? (
                      <button
                        type="button"
                        className="iconBtn iconBtnSubtle iconBtnDim"
                        onClick={() => openUrl(selectedProfileData.url!)}
                        title="Open profile page"
                        aria-label="Open profile page"
                      >
                        🔗
                      </button>
                    ) : null}
                  </div>
                  {selectedProfileData.description ? (
                    <p className="profileCardDesc">{selectedProfileData.description}</p>
                  ) : null}
                </div>
              ) : (
                <p className="profileCardEmpty">No profile selected</p>
              )}
            </div>
          </div>
        ) : null}
      </div>
    </section>
  );
}
