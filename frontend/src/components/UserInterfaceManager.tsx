import { useEffect, useMemo, useState } from "react";
import { openUrl } from "@tauri-apps/plugin-opener";
import {
  ArrowDownTrayIcon,
  ArrowPathIcon,
  ArrowTopRightOnSquareIcon,
  ChevronDownIcon,
  ChevronUpIcon,
  FolderIcon,
  FolderOpenIcon,
  TrashIcon
} from "@heroicons/react/24/outline";
import type { AppStatus } from "../types";
import {
  checkUserInterfaceResourcesUpdate,
  clearUserInterfaceInstalled,
  clearUserInterfaceResources,
  downloadUserInterfaceProfiles,
  installUserInterfaceProfile,
  openUserInterfaceItemFolder,
  openUserInterfaceResourcesFolder
} from "../backendConnector/api.ts";
import { useApplicationContext } from "../context/ApplicationContext.tsx";
import {
  buildProfileMeta,
  buildProfileOptionGroups,
  formatManifestVersion,
  isProfileAlreadyInstalled,
  resolveProfileName
} from "../formatting.ts";
import { ProfileDropdown } from "./ProfileDropdown.tsx";

type UserInterfaceManagerProps = {
  status: AppStatus | null;
  loading: boolean;
  onBusyChange: (busy: boolean) => void;
  onStatusRefresh: () => Promise<void>;
  onMessage: (message: string) => void;
};

export function UserInterfaceManager({
  status,
  loading,
  onBusyChange,
  onStatusRefresh,
  onMessage
}: UserInterfaceManagerProps) {
  const { backendReady, debugMode } = useApplicationContext();
  const [collapsed, setCollapsed] = useState(true);
  const [selectedProfile, setSelectedProfile] = useState("");
  const [expandedPreview, setExpandedPreview] = useState<string | null>(null);
  const [resourcesUpdateAvailable, setResourcesUpdateAvailable] = useState(false);
  const [resourcesUpdateChecking, setResourcesUpdateChecking] = useState(false);
  const [resourcesUpdateVersion, setResourcesUpdateVersion] = useState<string | undefined>(undefined);
  const availableProfiles = status?.userInterfaceAvailableProfiles ?? [];
  const profileOptionGroups = useMemo(
    () => buildProfileOptionGroups(availableProfiles),
    [availableProfiles]
  );
  const canInstall = Boolean(selectedProfile);
  const selectedProfileData = availableProfiles.find((profile) => profile.id === selectedProfile) ?? null;
  const selectedProfilePreviewImages = selectedProfileData?.previewImages ?? [];
  const selectedProfileMeta = buildProfileMeta({
    version: selectedProfileData?.version,
    author: selectedProfileData?.author,
    createdAt: selectedProfileData?.createdAt,
    separator: " · "
  });
  const installedProfileUrl = status?.userInterfaceInstalledProfile?.url ?? null;
  const activeProfileName = resolveProfileName(status?.userInterfaceInstalledProfile?.name, "No active profile");
  const activeProfileAuthor = status?.userInterfaceInstalledProfile?.author ? `by ${status.userInterfaceInstalledProfile.author}` : null;
  const activeProfileVersion = formatManifestVersion(status?.userInterfaceInstalledProfile?.version);

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
    void checkResourcesUpdate();
  }, [backendReady, status?.userInterfaceDownloadedProfiles]);

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
      const result = await checkUserInterfaceResourcesUpdate();
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
      await runAction(downloadUserInterfaceProfiles, "User interface profiles downloaded.");
      setResourcesUpdateAvailable(false);
      setResourcesUpdateVersion(undefined);
    } else {
      setResourcesUpdateChecking(true);
      try {
        const result = await checkUserInterfaceResourcesUpdate();
        if (result.success && result.updateAvailable) {
          setResourcesUpdateAvailable(true);
          setResourcesUpdateVersion(result.remoteVersion);
        } else if (result.success) {
          setResourcesUpdateAvailable(false);
          setResourcesUpdateVersion(undefined);
          onMessage("User interface resources are up to date.");
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
    if (!status?.userInterfaceSelectedGameItemFolder) {
      onMessage("Set the game folder first. It must contain trose.exe.");
      return;
    }
    const confirmed = window.confirm(
      `Install user interface profile "${selectedProfile}"? This will clear current installed models first.`
    );
    if (!confirmed) return;
    await runAction(
      () => installUserInterfaceProfile(selectedProfile),
      `Installed user interface profile: ${selectedProfile}.`
    );
  }

  async function onClearInstalled() {
    const confirmed = window.confirm("Clear all installed user interface models from the selected game folder?");
    if (!confirmed) return;
    await runAction(clearUserInterfaceInstalled, "Installed user interface models cleared.");
  }

  async function onOpenInstalledProfile(url: string) {
    try {
      await openUrl(url);
    } catch (err) {
      onMessage(toErrorMessage(err, "Failed to open profile link."));
    }
  }

  const resourcesUpdateTitle = resourcesUpdateAvailable
    ? `Download user interface update${resourcesUpdateVersion ? ` v${resourcesUpdateVersion}` : ""}`
    : "Check for user interface updates";
  const hasProfiles = (status?.userInterfaceDownloadedProfiles.length ?? 0) > 0;
  const selectedProfileAlreadyInstalled = isProfileAlreadyInstalled(selectedProfileData, status?.userInterfaceInstalledProfile);
  const installButtonLabel = selectedProfileAlreadyInstalled ? "Already installed" : "Install";
  const installButtonDisabled = loading || !canInstall || selectedProfileAlreadyInstalled;

  return (
    <>
      {expandedPreview ? (
        <div
          onClick={() => setExpandedPreview(null)}
          style={{
            position: "fixed",
            inset: 0,
            zIndex: 2147483647,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            background: "rgba(0, 0, 0, 0.78)",
            padding: 24,
            isolation: "isolate"
          }}
        >
          <img
            src={expandedPreview}
            alt="Expanded preview"
            onClick={(event) => event.stopPropagation()}
            style={{
              maxWidth: "90vw",
              maxHeight: "90vh",
              borderRadius: 12,
              boxShadow: "0 20px 50px rgba(0,0,0,0.45)",
              border: "1px solid rgba(255,255,255,0.2)",
              background: "rgba(255,255,255,0.04)"
            }}
          />
        </div>
      ) : null}
      <section className="lootManager">
        <div className="lootAccordion">
        <div className="accordionHeader">
          <div>
            <p className="sectionTitle">User interface</p>
            <p className="activeProfileMeta">
              Active profile: <span className="activeProfileValue">{activeProfileName}</span>
              {activeProfileAuthor ? <> • <span className="activeProfileValue">{activeProfileAuthor}</span></> : null}
              {activeProfileVersion ? <> • <span className="activeProfileVersion">{activeProfileVersion}</span></> : null}
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
            {debugMode ? (
              <>
                <button
                  type="button"
                  className="iconBtn iconBtnSubtle iconBtnDim"
                  disabled={loading}
                  onClick={() => runAction(openUserInterfaceResourcesFolder)}
                  title="Browse downloaded"
                  aria-label="Open downloaded"
                >
                  <FolderOpenIcon className="heroIcon" />
                </button>
                <button
                  type="button"
                  className="iconBtn iconBtnSubtle iconBtnDim"
                  disabled={loading}
                  onClick={() => runAction(clearUserInterfaceResources, "Downloaded user interface resources cleared.")}
                  title="Clear downloaded"
                  aria-label="Clear downloaded"
                >
                  <TrashIcon className="heroIcon" />
                </button>
                <button
                  type="button"
                  className="iconBtn iconBtnSubtle iconBtnDim"
                  disabled={loading}
                  onClick={() => runAction(openUserInterfaceItemFolder)}
                  title="Browse installed"
                  aria-label="Browse installed"
                >
                  <FolderIcon className="heroIcon" />
                </button>
              </>
            ) : null}
            <button
              type="button"
              className="iconBtn iconBtnSubtle iconBtnDim"
              disabled={loading}
              onClick={onClearInstalled}
              title="Clear installed"
              aria-label="Clear installed"
            >
              <TrashIcon className="heroIcon" />
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
              {resourcesUpdateAvailable ? <ArrowDownTrayIcon className="heroIcon" /> : <ArrowPathIcon className="heroIcon" />}
            </button>
            <button
              type="button"
              className="iconBtn iconBtnToggle"
              aria-label={collapsed ? "Expand User interface" : "Collapse User interface"}
              aria-expanded={!collapsed}
              disabled={loading || !hasProfiles}
              title={!hasProfiles ? "Download profiles first" : undefined}
              onClick={() => setCollapsed((value) => !value)}
            >
              {collapsed ? <ChevronDownIcon className="heroIcon" /> : <ChevronUpIcon className="heroIcon" />}
            </button>
          </div>
        </div>

        {!collapsed ? (
          <div className="accordionBody">
            <div className="accordionSection">
              <div className="profilePickerRow">
                <ProfileDropdown
                  groups={profileOptionGroups}
                  disabled={loading || availableProfiles.length === 0}
                  value={selectedProfile}
                  onChange={setSelectedProfile}
                />
              </div>
              {selectedProfileData ? (
                <div className="profileCard">
                  <div className="profileCardHeader">
                    <div>
                      <p className="profileCardName">{resolveProfileName(selectedProfileData.name, selectedProfileData.id)}</p>
                      {selectedProfileMeta && (
                        <p className="profileCardMeta">
                          {selectedProfileMeta}
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
                        <ArrowTopRightOnSquareIcon className="heroIcon" />
                      </button>
                    ) : null}
                  </div>
                  {selectedProfilePreviewImages.length > 0 ? (
                    <div style={{ display: "flex", flexWrap: "wrap", gap: 8, marginTop: 12, marginBottom: 12 }}>
                      {selectedProfilePreviewImages.map((image, index) => (
                        <img
                          key={`${selectedProfileData.id}-preview-${index}`}
                          src={image}
                          alt={`${resolveProfileName(selectedProfileData.name, selectedProfileData.id)} preview ${index + 1}`}
                          onClick={() => setExpandedPreview(image)}
                          style={{
                            width: 120,
                            height: 90,
                            objectFit: "cover",
                            borderRadius: 6,
                            border: "1px solid rgba(255,255,255,0.15)",
                            background: "rgba(255,255,255,0.03)",
                            cursor: "pointer"
                          }}
                        />
                      ))}
                    </div>
                  ) : null}
                  {selectedProfileData.description ? (
                    <p className="profileCardDesc">{selectedProfileData.description}</p>
                  ) : null}
                  {selectedProfileAlreadyInstalled ? (
                    <p className="profileCardInstalled">Already installed</p>
                  ) : null}
                  <button className="buttonStrong profileInstallBtn" disabled={installButtonDisabled} onClick={onInstallProfile}>
                    {installButtonLabel}
                  </button>
                </div>
              ) : (
                <>
                  <p className="profileCardEmpty">No profile selected</p>
                  <button className="buttonStrong profileInstallBtn" disabled={installButtonDisabled} onClick={onInstallProfile}>
                    {installButtonLabel}
                  </button>
                </>
              )}
            </div>
          </div>
        ) : null}
        </div>
      </section>
    </>
  );
}
