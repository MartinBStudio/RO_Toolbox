import { useEffect, useMemo, useState } from "react";
import { openUrl } from "@tauri-apps/plugin-opener";
import {
  ArrowDownTrayIcon,
  ArrowPathIcon,
  ArrowTopRightOnSquareIcon,
  ChevronDownIcon,
  ChevronUpIcon,
  Cog6ToothIcon,
  FolderOpenIcon,
  FolderIcon,
  TrashIcon
} from "@heroicons/react/24/outline";
import type { AppStatus } from "../types";
import {
  checkLootResourcesUpdate,
  clearInstalled,
  clearResources,
  downloadProfiles,
  installProfile,
  manageInstalledProfile,
  openItemFolder,
  openResourcesFolder
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
import { ManageInstalledLootModal } from "../elements/ManageInstalledLootModal.tsx";
import { IncludedFoldersTable } from "../elements/IncludedFoldersTable.tsx";
import { ConfirmationModal } from "../elements/ConfirmationModal.tsx";

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
  const { backendReady, debugMode } = useApplicationContext();
  const [collapsed, setCollapsed] = useState(true);
  const [selectedProfile, setSelectedProfile] = useState("");
  const [expandedPreview, setExpandedPreview] = useState<string | null>(null);
  const [resourcesUpdateAvailable, setResourcesUpdateAvailable] = useState(false);
  const [resourcesUpdateChecking, setResourcesUpdateChecking] = useState(false);
  const [resourcesUpdateVersion, setResourcesUpdateVersion] = useState<string | undefined>(undefined);
  const [manageModalOpen, setManageModalOpen] = useState(false);
  const [clearInstalledConfirmOpen, setClearInstalledConfirmOpen] = useState(false);
  const availableProfiles = status?.availableProfiles ?? [];
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
  const installedProfileUrl = status?.installedProfile?.url ?? null;
  const activeProfileName = resolveProfileName(status?.installedProfile?.name, "No active package");
  const activeProfileAuthor = status?.installedProfile?.author ? `by ${status.installedProfile.author}` : null;
  const activeProfileVersion = formatManifestVersion(status?.installedProfile?.version);

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
  }, [backendReady, status?.downloadedProfiles]);

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
      onMessage("Set the game folder first. It must contain trose.exe.");
      return;
    }
    const confirmed = window.confirm(
      `Install package "${selectedProfile}"? This will clear current installed models first.`
    );
    if (!confirmed) return;
    await runAction(() => installProfile(selectedProfile), `Installed package: ${selectedProfile}.`);
  }

  async function onClearInstalled() {
    setClearInstalledConfirmOpen(true);
  }

  async function confirmClearInstalled() {
    setClearInstalledConfirmOpen(false);
    await runAction(clearInstalled, "Installed models cleared.");
  }

  async function onSaveManagedFolders(disabledManagedSubfolders: string[]) {
    if (!status?.installedProfile) {
      onMessage("No installed profile available.");
      return;
    }

    const profileId = status.installedProfile.name || status.installedProfile.url || "installed";
    await runAction(
      () => manageInstalledProfile(profileId, disabledManagedSubfolders),
      "Managed folders updated."
    );
  }

  async function onOpenInstalledProfile(url: string) {
    try {
      await openUrl(url);
    } catch (err) {
      onMessage(toErrorMessage(err, "Failed to open package link."));
    }
  }

  const resourcesUpdateTitle = resourcesUpdateAvailable
    ? `Download loot models update${resourcesUpdateVersion ? ` v${resourcesUpdateVersion}` : ""}`
    : "Check for loot models updates";
  const hasProfiles = (status?.downloadedProfiles.length ?? 0) > 0;
  const canClearInstalled = Boolean(status?.installedProfile);
  const selectedProfileAlreadyInstalled = isProfileAlreadyInstalled(selectedProfileData, status?.installedProfile);
  const installButtonLabel = selectedProfileAlreadyInstalled ? "Already installed" : "Install";
  const installButtonDisabled = loading || !canInstall || selectedProfileAlreadyInstalled;
  const canManageInstalledFolders = Boolean(status?.installedProfile && status.selectedGameItemFolder);

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
            <p className="sectionTitle">Loot models</p>
            <p className="activeProfileMeta">
              Active package: <span className="activeProfileValue">{activeProfileName}</span>
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
                  onClick={() => runAction(openResourcesFolder)}
                  title="Browse downloaded"
                  aria-label="Browse downloaded"
                >
                  <FolderOpenIcon className="heroIcon" />
                </button>
                <button
                  type="button"
                  className="iconBtn iconBtnSubtle iconBtnDim"
                  disabled={loading}
                  onClick={() => runAction(clearResources, "Downloaded resources cleared.")}
                  title="Clear downloaded"
                  aria-label="Clear downloaded"
                >
                  <TrashIcon className="heroIcon" />
                </button>
                <button
                  type="button"
                  className="iconBtn iconBtnSubtle iconBtnDim"
                  disabled={loading}
                  onClick={() => runAction(openItemFolder)}
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
                {canManageInstalledFolders ? <span className="headerSep" /> : null}
              </>
            ) : null}
            {canManageInstalledFolders ? (
              <>
                <button
                  type="button"
                  className="iconBtn iconBtnSubtle iconBtnDim"
                  disabled={loading}
                  onClick={() => setManageModalOpen(true)}
                  title="Manage installed packages"
                  aria-label="Manage installed packages"
                >
                  <Cog6ToothIcon className="heroIcon" />
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
              aria-label={collapsed ? "Expand Loot models" : "Collapse Loot models"}
              aria-expanded={!collapsed}
              disabled={loading || !hasProfiles}
              title={!hasProfiles ? "Download packages first" : undefined}
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
                <p className="settingsSectionLabel">Available packages</p>
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
                        title="Open package page"
                        aria-label="Open package page"
                      >
                        <ArrowTopRightOnSquareIcon className="heroIcon" />
                      </button>
                    ) : null}
                  </div>
                  {selectedProfilePreviewImages.length > 0 ? (
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
                  <IncludedFoldersTable folders={selectedProfileData.managedSubfolders || []} />
                  <button className="buttonStrong profileInstallBtn" disabled={installButtonDisabled} onClick={onInstallProfile}>
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
          </div>
        ) : null}
      </div>
      </section>
      <ManageInstalledLootModal
        isOpen={manageModalOpen}
        managedSubfolders={status?.installedProfile?.managedSubfolders || []}
        disabledManagedSubfolders={status?.installedProfile?.disabledManagedSubfolders || []}
        onClose={() => setManageModalOpen(false)}
        onSave={onSaveManagedFolders}
      />
      <ConfirmationModal
        open={clearInstalledConfirmOpen}
        title="Clear installed models"
        message="Clear all installed models from the selected game folder?"
        confirmLabel="Clear"
        onConfirm={confirmClearInstalled}
        onClose={() => setClearInstalledConfirmOpen(false)}
      />
    </>
  );
}
