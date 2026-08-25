import { useState } from "react";
import type { AppStatus } from "../types";

type LootManagerProps = {
  status: AppStatus | null;
  loading: boolean;
  selectedProfile: string;
  canInstall: boolean;
  onSelectedProfileChange: (profileId: string) => void;
  onDownloadProfiles: () => void;
  onOpenResourcesFolder: () => void;
  onClearResources: () => void;
  onInstallProfile: () => void;
  onOpenItemFolder: () => void;
  onClearInstalled: () => void;
  onOpenInstalledProfile: (url: string) => void;
};

export function LootManager({
  status,
  loading,
  selectedProfile,
  canInstall,
  onSelectedProfileChange,
  onDownloadProfiles,
  onOpenResourcesFolder,
  onClearResources,
  onInstallProfile,
  onOpenItemFolder,
  onClearInstalled,
  onOpenInstalledProfile
}: LootManagerProps) {
  const [collapsed, setCollapsed] = useState(true);
  const availableProfiles = status?.availableProfiles ?? [];
  const selectedProfileData = availableProfiles.find((profile) => profile.id === selectedProfile) ?? null;
  const installedProfileUrl = status?.installedProfile?.url ?? null;
  const activeProfileName = status?.installedProfile?.name ?? "No active profile";
  const activeProfileAuthor = status?.installedProfile?.author
    ? ` • by ${status.installedProfile.author}`
    : "";

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
              onClick={onOpenItemFolder}
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
              aria-label={collapsed ? "Expand Loot models" : "Collapse Loot models"}
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
              <p>Download profiles first. Downloaded: {status?.downloadedProfiles.length ?? 0}</p>
              <div className="headerActions">
                <button
                  type="button"
                  className="iconBtn iconBtnSubtle"
                  disabled={loading}
                  onClick={onDownloadProfiles}
                  title="Download profiles"
                  aria-label="Download profiles"
                >
                  ⬇
                </button>
                <button
                  type="button"
                  className="iconBtn iconBtnSubtle"
                  disabled={loading}
                  onClick={onOpenResourcesFolder}
                  title="Open downloaded"
                  aria-label="Open downloaded"
                >
                  📂
                </button>
                <button
                  type="button"
                  className="iconBtn iconBtnSubtle"
                  disabled={loading}
                  onClick={onClearResources}
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
                  onChange={(event) => onSelectedProfileChange(event.target.value)}
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
