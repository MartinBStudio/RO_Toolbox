import { useEffect, useMemo, useState } from "react";
import { openUrl } from "@tauri-apps/plugin-opener";
import {
  ArrowDownTrayIcon,
  ArrowPathIcon,
  ArrowTopRightOnSquareIcon,
  MagnifyingGlassIcon,
  FolderIcon,
  FolderOpenIcon,
  TrashIcon
} from "@heroicons/react/24/outline";
import type { AppStatus } from "../types";
import {
  checkBuffIconsResourcesUpdate,
  clearBuffIconsInstalled,
  clearBuffIconsResources,
  downloadBuffIconsProfiles,
  installBuffIconsProfile,
  openBuffIconsItemFolder,
  openBuffIconsResourcesFolder
} from "../backendConnector/api.ts";
import { useApplicationContext } from "../context/ApplicationContext.tsx";
import {
  buildProfileMeta,
  buildProfileOptionGroups,
  findInstalledAvailableProfileId,
  formatManifestVersion,
  isProfileAlreadyInstalled,
  resolveProfileName
} from "../formatting.ts";
import { ProfileDropdown } from "./ProfileDropdown.tsx";
import { ConfirmationModal } from "../elements/ConfirmationModal.tsx";
import { ServiceDetailModal } from "../elements/ServiceDetailModal.tsx";
import { ServiceAccordionTitle } from "../elements/ServiceAccordionTitle.tsx";

type BuffIconsManagerProps = {
  status: AppStatus | null;
  loading: boolean;
  onBusyChange: (busy: boolean, message?: string) => void;
  onStatusRefresh: () => Promise<void>;
  onMessage: (message: string) => void;
};

export function BuffIconsManager({
  status,
  loading,
  onBusyChange,
  onStatusRefresh,
  onMessage
}: BuffIconsManagerProps) {
  const { backendReady, debugMode } = useApplicationContext();
  const [collapsed, setCollapsed] = useState(true);
  const [selectedProfile, setSelectedProfile] = useState("");
  const [expandedPreview, setExpandedPreview] = useState<string | null>(null);
  const [resourcesUpdateAvailable, setResourcesUpdateAvailable] = useState(false);
  const [resourcesUpdateChecking, setResourcesUpdateChecking] = useState(false);
  const [resourcesUpdateVersion, setResourcesUpdateVersion] = useState<string | undefined>(undefined);
  const [clearInstalledConfirmOpen, setClearInstalledConfirmOpen] = useState(false);
  const availableProfiles = status?.buffIconsAvailableProfiles ?? [];
  const profileOptionGroups = useMemo(
    () => buildProfileOptionGroups(availableProfiles),
    [availableProfiles]
  );
  const canInstall = Boolean(selectedProfile);
  const selectedProfileData = availableProfiles.find((profile) => profile.id === selectedProfile) ?? null;
  const selectedProfilePreviewImages = (selectedProfileData?.previewImages ?? []).filter((image) => image.trim().length > 0);
  const hasSelectedProfilePreviewImages = selectedProfilePreviewImages.length > 0;
  const selectedProfileMeta = buildProfileMeta({
    version: selectedProfileData?.version,
    author: selectedProfileData?.author,
    createdAt: selectedProfileData?.createdAt,
    separator: " · "
  });
  const installedProfileUrl = status?.buffIconsInstalledProfile?.url ?? null;
  const installedAvailableProfileId = findInstalledAvailableProfileId(availableProfiles, status?.buffIconsInstalledProfile);
  const hasInstalledProfile = Boolean(status?.buffIconsInstalledProfile);
  const activeProfileName = resolveProfileName(status?.buffIconsInstalledProfile?.name, "No active package");
  const activeProfileAuthor = status?.buffIconsInstalledProfile?.author ? `by ${status.buffIconsInstalledProfile.author}` : null;
  const activeProfileVersion = formatManifestVersion(status?.buffIconsInstalledProfile?.version);

  useEffect(() => {
    if (availableProfiles.length === 0) {
      if (selectedProfile) {
        setSelectedProfile("");
      }
      return;
    }
    const selectedStillExists = availableProfiles.some((profile) => profile.id === selectedProfile);
    if (!selectedStillExists) {
      setSelectedProfile(installedAvailableProfileId ?? availableProfiles[0].id);
    }
  }, [availableProfiles, installedAvailableProfileId, selectedProfile]);

  function openPackageBrowser() {
    setSelectedProfile(installedAvailableProfileId ?? availableProfiles[0]?.id ?? "");
    setCollapsed(false);
  }

  useEffect(() => {
    if (!backendReady) return;
    void checkResourcesUpdate();
  }, [backendReady, status?.buffIconsDownloadedProfiles?.length]);

  useEffect(() => {
    setExpandedPreview(null);
  }, [selectedProfile]);

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
      const result = await checkBuffIconsResourcesUpdate();
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

  async function runAction(action: () => Promise<unknown>, successMessage?: string, busyMessage?: string) {
    onBusyChange(true, busyMessage);
    try {
      await action();
      await onStatusRefresh();
      if (successMessage) {
        onMessage(successMessage);
      }
      return true;
    } catch (err) {
      onMessage(toErrorMessage(err, "Request failed."));
      return false;
    } finally {
      onBusyChange(false);
    }
  }

  async function onResourcesUpdateAction() {
    if (resourcesUpdateAvailable) {
      await runAction(downloadBuffIconsProfiles, "Buff icon packages downloaded.", "Downloading buff icon packages...");
      setResourcesUpdateAvailable(false);
      setResourcesUpdateVersion(undefined);
    } else {
      setResourcesUpdateChecking(true);
      try {
        const result = await checkBuffIconsResourcesUpdate();
        if (result.success && result.updateAvailable) {
          setResourcesUpdateAvailable(true);
          setResourcesUpdateVersion(result.remoteVersion);
        } else if (result.success) {
          setResourcesUpdateAvailable(false);
          setResourcesUpdateVersion(undefined);
          onMessage("Buff icon resources are up to date.");
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
    if (!status?.buffIconsSelectedGameItemFolder) {
      onMessage("Set the game folder first. It must contain trose.exe.");
      return;
    }
    const confirmed = window.confirm(
      `Install buff icon package "${selectedProfile}"? This will replace the current state icon file.`
    );
    if (!confirmed) return;
    const success = await runAction(
      () => installBuffIconsProfile(selectedProfile),
      `Installed buff icon package: ${selectedProfile}.`
    );
    if (success) {
      setCollapsed(true);
    }
  }

  async function onClearInstalled() {
    setClearInstalledConfirmOpen(true);
  }

  async function confirmClearInstalled() {
    setClearInstalledConfirmOpen(false);
    await runAction(clearBuffIconsInstalled, "Installed buff icons cleared.");
  }

  async function onOpenInstalledProfile(url: string) {
    try {
      await openUrl(url);
    } catch (err) {
      onMessage(toErrorMessage(err, "Failed to open package link."));
    }
  }

  const resourcesUpdateTitle = resourcesUpdateAvailable
    ? `Download buff icons update${resourcesUpdateVersion ? ` v${resourcesUpdateVersion}` : ""}`
    : "Check for buff icons updates";
  const hasProfiles = (status?.buffIconsDownloadedProfiles?.length ?? 0) > 0;
  const canClearInstalled = Boolean(status?.buffIconsInstalledProfile);
  const selectedProfileAlreadyInstalled = isProfileAlreadyInstalled(selectedProfileData, status?.buffIconsInstalledProfile);
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
            <ServiceAccordionTitle
              title="Icons"
              activeProfileName={activeProfileName}
              activeProfileAuthor={activeProfileAuthor}
              activeProfileVersion={activeProfileVersion}
              hasInstalledProfile={hasInstalledProfile}
            />
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
                    onClick={() => runAction(openBuffIconsResourcesFolder)}
                    title="Browse downloaded"
                    aria-label="Open downloaded"
                  >
                    <FolderOpenIcon className="heroIcon" />
                  </button>
                  <button
                    type="button"
                    className="iconBtn iconBtnSubtle iconBtnDim"
                    disabled={loading}
                    onClick={() => runAction(clearBuffIconsResources, "Downloaded buff icon resources cleared.")}
                    title="Clear downloaded"
                    aria-label="Clear downloaded"
                  >
                    <TrashIcon className="heroIcon" />
                  </button>
                  <button
                    type="button"
                    className="iconBtn iconBtnSubtle iconBtnDim"
                    disabled={loading}
                    onClick={() => runAction(openBuffIconsItemFolder)}
                    title="Browse installed"
                    aria-label="Browse installed"
                  >
                    <FolderIcon className="heroIcon" />
                  </button>
                </>
              ) : null}
              {canClearInstalled ? (
                <>
                  <button
                    type="button"
                    className="iconBtn iconBtnDanger iconBtnDim"
                    disabled={loading}
                    onClick={onClearInstalled}
                    title="Clear installed"
                    aria-label="Clear installed"
                  >
                    <TrashIcon className="heroIcon" />
                  </button>
                  <span className="headerSep" />
                </>
              ) : null}
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
                disabled={loading || !hasProfiles}
                title={!hasProfiles ? "No downloaded packages yet" : "Browse packages"}
                aria-label="Browse packages"
                onClick={openPackageBrowser}
              >
                <MagnifyingGlassIcon className="heroIcon" />
              </button>
            </div>
          </div>

        <ServiceDetailModal
          open={!collapsed}
          title="Buff icons"
          description="Choose a package, preview it, and install it into the selected game folder."
          onClose={() => setCollapsed(true)}
        >
          <div className="accordionSection">
            <div className="profilePickerRow">
              <p className="settingsSectionLabel">Choose package</p>
              <ProfileDropdown
                groups={profileOptionGroups}
                disabled={loading || availableProfiles.length === 0}
                value={selectedProfile}
                onChange={setSelectedProfile}
              />
            </div>
            {selectedProfileData ? (
              <div className={`profileCard${hasSelectedProfilePreviewImages ? "" : " profileCardNoPreview"}`}>
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
                      title="Open package page"
                      aria-label="Open package page"
                    >
                      <ArrowTopRightOnSquareIcon className="heroIcon" />
                    </button>
                  ) : null}
                </div>
                {hasSelectedProfilePreviewImages ? (
                  <div className="profileCardPreviewGrid">
                    {selectedProfilePreviewImages.map((image, index) => (
                      <img
                        key={`${selectedProfileData.id}-preview-${index}`}
                        src={image}
                        alt={`${resolveProfileName(selectedProfileData.name, selectedProfileData.id)} preview ${index + 1}`}
                        onClick={() => setExpandedPreview(image)}
                        className="profileCardPreviewImage"
                      />
                    ))}
                  </div>
                ) : null}
                {selectedProfileData.description ? (
                  <p className="profileCardDesc">{selectedProfileData.description}</p>
                ) : null}
                <button
                  className={`buttonStrong profileInstallBtn${hasSelectedProfilePreviewImages ? "" : " profileInstallBtnCompact"}`}
                  disabled={installButtonDisabled}
                  onClick={onInstallProfile}
                >
                  {installButtonLabel}
                </button>
              </div>
            ) : (
              <>
                <p className="profileCardEmpty">No package selected</p>
                <button className="buttonStrong profileInstallBtn" disabled={installButtonDisabled} onClick={onInstallProfile}>
                  {installButtonLabel}
                </button>
              </>
            )}
          </div>
        </ServiceDetailModal>
        </div>
      </section>
      <ConfirmationModal
        open={clearInstalledConfirmOpen}
        title="Clear installed buff icons"
        message="Clear the installed buff icon file from the selected game folder?"
        confirmLabel="Clear"
        onConfirm={confirmClearInstalled}
        onClose={() => setClearInstalledConfirmOpen(false)}
      />
    </>
  );
}
