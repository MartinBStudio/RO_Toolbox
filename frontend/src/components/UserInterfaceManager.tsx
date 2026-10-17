import { useEffect, useState } from "react";
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
  findInstalledAvailableProfileId,
  formatManifestVersion,
  isProfileAlreadyInstalled,
  resolveProfileName
} from "../formatting.ts";
import { ConfirmationModal } from "../elements/ConfirmationModal.tsx";
import { ServiceDetailModal } from "../elements/ServiceDetailModal.tsx";
import { ServiceAccordionTitle } from "../elements/ServiceAccordionTitle.tsx";

type UserInterfaceManagerProps = {
  status: AppStatus | null;
  loading: boolean;
  onBusyChange: (busy: boolean, message?: string, progress?: number | null) => void;
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
  const [packageSearch, setPackageSearch] = useState("");
  const [previewGalleryOpen, setPreviewGalleryOpen] = useState(false);
  const [previewGalleryImages, setPreviewGalleryImages] = useState<string[]>([]);
  const [previewGalleryIndex, setPreviewGalleryIndex] = useState(0);
  const [resourcesUpdateAvailable, setResourcesUpdateAvailable] = useState(false);
  const [resourcesUpdateChecking, setResourcesUpdateChecking] = useState(false);
  const [resourcesUpdateVersion, setResourcesUpdateVersion] = useState<string | undefined>(undefined);
  const [clearInstalledConfirmOpen, setClearInstalledConfirmOpen] = useState(false);

  const availableProfiles = status?.userInterfaceAvailableProfiles ?? [];
  const matchingProfiles = availableProfiles.filter(profile =>
    resolveProfileName(profile.name, profile.id).toLowerCase().includes(packageSearch.trim().toLowerCase())
  );
  const installedProfileUrl = status?.userInterfaceInstalledProfile?.url ?? null;
  const installedAvailableProfileId = findInstalledAvailableProfileId(availableProfiles, status?.userInterfaceInstalledProfile);
  const hasInstalledProfile = Boolean(status?.userInterfaceInstalledProfile);
  const activeProfileName = resolveProfileName(status?.userInterfaceInstalledProfile?.name, "No active package");
  const activeProfileAuthor = status?.userInterfaceInstalledProfile?.author ? `by ${status.userInterfaceInstalledProfile.author}` : null;
  const activeProfileVersion = formatManifestVersion(status?.userInterfaceInstalledProfile?.version);
  const galleryPreviewImage = previewGalleryImages[previewGalleryIndex] ?? null;

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

  function openPackageBrowser() {
    setPackageSearch("");
    setSelectedProfile(installedAvailableProfileId ?? availableProfiles[0]?.id ?? "");
    setCollapsed(false);
  }

  function openPreviewGallery(images: string[], startIndex = 0) {
    const sanitized = images.filter((image) => image && image.trim().length > 0);
    if (!sanitized.length) {
      setPreviewGalleryOpen(false);
      setPreviewGalleryImages([]);
      setPreviewGalleryIndex(0);
      return;
    }
    setPreviewGalleryImages(sanitized);
    setPreviewGalleryIndex(startIndex >= 0 && startIndex < sanitized.length ? startIndex : 0);
    setPreviewGalleryOpen(true);
  }

  function closePreviewGallery() {
    setPreviewGalleryOpen(false);
    setPreviewGalleryImages([]);
    setPreviewGalleryIndex(0);
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

  async function onResourcesUpdateAction() {
    if (resourcesUpdateAvailable) {
      onBusyChange(true, "Downloading user interface packages...", 0);
      try {
        await downloadUserInterfaceProfiles((progress) => {
          if (progress.type === "complete") {
            onBusyChange(true, progress.message || "Download complete.", 100);
            return;
          }
          if (progress.type === "started") {
            onBusyChange(true, progress.message || "Downloading user interface packages...", 0);
            return;
          }
          if (progress.type === "progress") {
            const percentage = progress.totalBytes > 0
              ? (progress.downloadedBytes / progress.totalBytes) * 100
              : null;
            onBusyChange(true, "Downloading user interface packages...", percentage ?? 0);
          }
        });
        await onStatusRefresh();
        onMessage("User interface packages downloaded.");
        setResourcesUpdateAvailable(false);
        setResourcesUpdateVersion(undefined);
      } catch (err) {
        onMessage(toErrorMessage(err, "Request failed."));
      } finally {
        onBusyChange(false);
      }
      return;
    }

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

  async function onInstallProfile(profileIdOverride?: string) {
    const targetProfileId = profileIdOverride ?? selectedProfile;
    if (!targetProfileId) return;

    const targetProfile = availableProfiles.find((profile) => profile.id === targetProfileId) ?? null;
    if (!status?.userInterfaceSelectedGameItemFolder) {
      onMessage("Set the game folder first. It must contain trose.exe.");
      return;
    }

    const targetName = resolveProfileName(targetProfile?.name ?? targetProfileId, targetProfileId);
    const confirmed = window.confirm(
      `Install user interface package "${targetName}"? This will clear current installed models first.`
    );
    if (!confirmed) return;

    const success = await runAction(
      () => installUserInterfaceProfile(targetProfileId),
      `Installed user interface package: ${targetName}.`
    );

    if (success) {
      setSelectedProfile(targetProfileId);
      setCollapsed(true);
    }
  }

  async function onClearInstalled() {
    setClearInstalledConfirmOpen(true);
  }

  async function confirmClearInstalled() {
    setClearInstalledConfirmOpen(false);
    await runAction(clearUserInterfaceInstalled, "Installed user interface models cleared.");
  }

  async function onOpenInstalledProfile(url: string) {
    try {
      await openUrl(url);
    } catch (err) {
      onMessage(toErrorMessage(err, "Failed to open package link."));
    }
  }

  const resourcesUpdateTitle = resourcesUpdateAvailable
    ? `Download user interface update${resourcesUpdateVersion ? ` v${resourcesUpdateVersion}` : ""}`
    : "Check for user interface updates";
  const hasProfiles = (status?.userInterfaceDownloadedProfiles?.length ?? 0) > 0;
  const canClearInstalled = Boolean(status?.userInterfaceInstalledProfile);

  return (
    <>
      {previewGalleryOpen && galleryPreviewImage ? (
        <div
          className="profilePreviewOverlay"
          onClick={closePreviewGallery}
          role="dialog"
          aria-modal="true"
          aria-label="Package preview gallery"
          style={{
            position: "fixed",
            inset: 0,
            zIndex: 2147483647,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            background: "rgba(4, 9, 15, 0.82)",
            backdropFilter: "blur(2px)",
            padding: "1.25rem"
          }}
        >
          <div
            className="profilePreviewShell"
            onClick={(event) => event.stopPropagation()}
            style={{
              position: "relative",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              width: "min(92vw, 1200px)",
              maxHeight: "92vh"
            }}
          >
            {previewGalleryImages.length > 1 ? (
              <button
                type="button"
                className="profilePreviewNav profilePreviewNavPrev"
                aria-label="Previous preview"
                onClick={() => setPreviewGalleryIndex((index) => (index === 0 ? previewGalleryImages.length - 1 : index - 1))}
                style={{
                  position: "absolute",
                  top: "50%",
                  left: "-3.1rem",
                  transform: "translateY(-50%)",
                  width: "2.6rem",
                  height: "2.6rem",
                  borderRadius: "999px",
                  border: "1px solid rgba(145, 175, 228, 0.35)",
                  background: "rgba(12, 18, 28, 0.72)",
                  color: "#edf5ff",
                  fontSize: "1.5rem",
                  lineHeight: 1,
                  cursor: "pointer"
                }}
              >
                ‹
              </button>
            ) : null}

            <div
              className="profilePreviewFrame"
              style={{
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                width: "min(92vw, 1100px)",
                maxHeight: "90vh",
                borderRadius: 14,
                overflow: "hidden",
                border: "1px solid rgba(146, 175, 226, 0.35)",
                background: "rgba(255, 255, 255, 0.04)",
                boxShadow: "0 26px 60px rgba(0, 0, 0, 0.45)"
              }}
            >
              <img
                src={galleryPreviewImage}
                alt="Package preview"
                style={{
                  display: "block",
                  maxWidth: "100%",
                  maxHeight: "90vh",
                  width: "auto",
                  height: "auto",
                  objectFit: "contain",
                  background: "rgba(5, 9, 15, 0.7)"
                }}
              />
            </div>

            {previewGalleryImages.length > 1 ? (
              <button
                type="button"
                className="profilePreviewNav profilePreviewNavNext"
                aria-label="Next preview"
                onClick={() => setPreviewGalleryIndex((index) => (index + 1) % previewGalleryImages.length)}
                style={{
                  position: "absolute",
                  top: "50%",
                  right: "-3.1rem",
                  transform: "translateY(-50%)",
                  width: "2.6rem",
                  height: "2.6rem",
                  borderRadius: "999px",
                  border: "1px solid rgba(145, 175, 228, 0.35)",
                  background: "rgba(12, 18, 28, 0.72)",
                  color: "#edf5ff",
                  fontSize: "1.5rem",
                  lineHeight: 1,
                  cursor: "pointer"
                }}
              >
                ›
              </button>
            ) : null}

            <button
              type="button"
              className="profilePreviewClose"
              aria-label="Close preview"
              onClick={closePreviewGallery}
              style={{
                position: "absolute",
                top: "-0.9rem",
                right: "-0.9rem",
                width: "2.6rem",
                height: "2.6rem",
                borderRadius: "999px",
                border: "1px solid rgba(145, 175, 228, 0.35)",
                background: "rgba(12, 18, 28, 0.72)",
                color: "#edf5ff",
                fontSize: "1.5rem",
                lineHeight: 1,
                cursor: "pointer"
              }}
            >
              ✕
            </button>
          </div>
        </div>
      ) : null}

      <section className="lootManager">
        <div className="lootAccordion">
          <div className="accordionHeader">
            <ServiceAccordionTitle
              title="User interface"
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
            title="User interface"
            headerContent={
              <input
                type="search"
                className="profileBrowserSearch"
                aria-label="Search UI packages by name"
                placeholder="Search packages by name..."
                value={packageSearch}
                onChange={event => setPackageSearch(event.target.value)}
                autoFocus
              />
            }
            onClose={() => setCollapsed(true)}
          >
            <div className="accordionSection">
              {availableProfiles.length === 0 ? (
                <div className="profileCard profileCardNoPreview">
                  <p className="profileCardEmpty">No user interface packages downloaded yet.</p>
                </div>
              ) : matchingProfiles.length === 0 ? (
                <p className="profileCardEmpty" role="status">No packages match your search.</p>
              ) : (
                <div className="profileGridBrowser">
                  {matchingProfiles.map((profile) => {
                    const previewImages = (profile.previewImages ?? []).filter((image) => image.trim().length > 0);
                    const isInstalled = isProfileAlreadyInstalled(profile, status?.userInterfaceInstalledProfile);
                    const isSelected = profile.id === selectedProfile;
                    const profileMeta = buildProfileMeta({
                      version: profile.version,
                      author: profile.author,
                      separator: " · "
                    });

                    return (
                      <article key={profile.id} className={`profileBrowseCard${isSelected ? " isSelected" : ""}`}>
                        <div className="profileBrowsePreviewWrap">
                          {previewImages.length > 0 ? (
                            <button
                              type="button"
                              className="profileBrowsePreviewButton"
                              aria-label={`Preview ${resolveProfileName(profile.name, profile.id)}`}
                              onClick={() => {
                                setSelectedProfile(profile.id);
                                openPreviewGallery(previewImages, 0);
                              }}
                            >
                              <img src={previewImages[0]} alt={`${resolveProfileName(profile.name, profile.id)}`} />
                            </button>
                          ) : (
                            <button
                              type="button"
                              className="profileBrowsePlaceholder"
                              aria-label={`No preview available for ${resolveProfileName(profile.name, profile.id)}`}
                              onClick={() => setSelectedProfile(profile.id)}
                            >
                              <span>No preview</span>
                            </button>
                          )}
                          {previewImages.length > 1 ? (
                            <span className="profileBrowseCounter">{previewImages.length} shots</span>
                          ) : null}
                        </div>

                        <div className="profileBrowseBody">
                          <div className="profileBrowseHeader">
                            <div>
                              <p className="profileBrowseName">{resolveProfileName(profile.name, profile.id)}</p>
                              {profileMeta ? <p className="profileCardMeta">{profileMeta}</p> : null}
                            </div>
                            {profile.url ? (
                              <button
                                type="button"
                                className="iconBtn iconBtnSubtle iconBtnDim"
                                onClick={() => openUrl(profile.url!)}
                                title="Open package page"
                                aria-label="Open package page"
                              >
                                <ArrowTopRightOnSquareIcon className="heroIcon" />
                              </button>
                            ) : null}
                          </div>

                          {profile.description ? <p className="profileBrowseDesc">{profile.description}</p> : null}

                          <div className="profileBrowseActions">
                            <button
                              type="button"
                              className="buttonStrong profileBrowseActionBtn"
                              disabled={loading || !status?.userInterfaceSelectedGameItemFolder || isInstalled}
                              onClick={() => onInstallProfile(profile.id)}
                            >
                              {isInstalled ? "Installed" : "Install"}
                            </button>
                          </div>
                        </div>
                      </article>
                    );
                  })}
                </div>
              )}
            </div>
          </ServiceDetailModal>
        </div>
      </section>

      <ConfirmationModal
        open={clearInstalledConfirmOpen}
        title="Clear installed models"
        message="Clear all installed user interface models from the selected game folder?"
        confirmLabel="Clear"
        onConfirm={confirmClearInstalled}
        onClose={() => setClearInstalledConfirmOpen(false)}
      />
    </>
  );
}
