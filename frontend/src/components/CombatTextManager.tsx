import { useEffect, useState } from "react";
import { openUrl } from "@tauri-apps/plugin-opener";
import {
  ArrowDownTrayIcon,
  ArrowPathIcon,
  FolderIcon,
  FolderOpenIcon,
  TrashIcon
} from "@heroicons/react/24/outline";
import type { AppStatus } from "../types";
import {
  checkCombatTextResourcesUpdate,
  clearCombatTextInstalled,
  clearCombatTextResources,
  downloadCombatTextProfiles,
  installCombatTextProfile,
  openCombatTextItemFolder,
  openCombatTextResourcesFolder
} from "../backendConnector/api.ts";
import { useApplicationContext } from "../context/ApplicationContext.tsx";
import {
  findInstalledAvailableProfileId,
  formatManifestVersion,
  resolveProfileName
} from "../formatting.ts";
import { ConfirmationModal } from "../elements/ConfirmationModal.tsx";
import { PackageBrowserModal } from "../elements/PackageBrowserModal";
import { ServiceAccordionTitle } from "../elements/ServiceAccordionTitle.tsx";

type CombatTextManagerProps = {
  status: AppStatus | null;
  loading: boolean;
  onBusyChange: (busy: boolean, message?: string, progress?: number | null) => void;
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
  const { backendReady, debugMode } = useApplicationContext();
  const [collapsed, setCollapsed] = useState(true);
  const [selectedProfile, setSelectedProfile] = useState("");
  const [resourcesUpdateAvailable, setResourcesUpdateAvailable] = useState(false);
  const [resourcesUpdateChecking, setResourcesUpdateChecking] = useState(false);
  const [resourcesUpdateVersion, setResourcesUpdateVersion] = useState<string | undefined>(undefined);
  const [clearInstalledConfirmOpen, setClearInstalledConfirmOpen] = useState(false);
  const availableProfiles = status?.combatTextAvailableProfiles ?? [];
  const recommendedProfile = availableProfiles.find(isRecommendedCombatTextProfile) ?? null;
  const installedProfileUrl = status?.combatTextInstalledProfile?.url ?? null;
  const installedAvailableProfileId = findInstalledAvailableProfileId(availableProfiles, status?.combatTextInstalledProfile);
  const hasInstalledProfile = Boolean(status?.combatTextInstalledProfile);
  const activeProfileName = resolveProfileName(status?.combatTextInstalledProfile?.name, "No active package");
  const activeProfileAuthor = status?.combatTextInstalledProfile?.author ? `by ${status.combatTextInstalledProfile.author}` : null;
  const activeProfileVersion = formatManifestVersion(status?.combatTextInstalledProfile?.version);

  useEffect(() => {
    if (availableProfiles.length === 0) {
      if (selectedProfile) {
        setSelectedProfile("");
      }
      return;
    }
    const selectedStillExists = availableProfiles.some((profile) => profile.id === selectedProfile);
    if (!selectedStillExists) {
      setSelectedProfile(installedAvailableProfileId ?? recommendedProfile?.id ?? availableProfiles[0].id);
    }
  }, [availableProfiles, installedAvailableProfileId, recommendedProfile, selectedProfile]);

  function openPackageBrowser() {
    setSelectedProfile(installedAvailableProfileId ?? recommendedProfile?.id ?? availableProfiles[0]?.id ?? "");
    setCollapsed(false);
  }

  useEffect(() => {
    if (!backendReady) return;
    void checkResourcesUpdate();
  }, [backendReady, status?.combatTextDownloadedProfiles]);


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
      const result = await checkCombatTextResourcesUpdate();
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
      onBusyChange(true, "Downloading combat text packages...", 0);
      try {
        await downloadCombatTextProfiles((progress) => {
          if (progress.type === "complete") {
            onBusyChange(true, progress.message || "Download complete.", 100);
            return;
          }
          if (progress.type === "started") {
            onBusyChange(true, progress.message || "Downloading combat text packages...", 0);
            return;
          }
          if (progress.type === "progress") {
            const percentage = progress.totalBytes > 0
              ? (progress.downloadedBytes / progress.totalBytes) * 100
              : null;
            onBusyChange(true, "Downloading combat text packages...", percentage ?? 0);
          }
        });
        await onStatusRefresh();
        onMessage("Combat text packages downloaded.");
        setResourcesUpdateAvailable(false);
        setResourcesUpdateVersion(undefined);
      } catch (err) {
        onMessage(toErrorMessage(err, "Request failed."));
      } finally {
        onBusyChange(false);
      }
    } else {
      setResourcesUpdateChecking(true);
      try {
        const result = await checkCombatTextResourcesUpdate();
        if (result.success && result.updateAvailable) {
          setResourcesUpdateAvailable(true);
          setResourcesUpdateVersion(result.remoteVersion);
        } else if (result.success) {
          setResourcesUpdateAvailable(false);
          setResourcesUpdateVersion(undefined);
          onMessage("Combat text resources are up to date.");
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

  async function onInstallProfile(profileId: string) {
    if (!profileId) return;
    if (!status?.combatTextSelectedGameItemFolder) {
      onMessage("Set the game folder first. It must contain trose.exe.");
      return;
    }
    setSelectedProfile(profileId);
    const confirmed = window.confirm(
      `Install combat text package "${profileId}"? This will clear current installed models first.`
    );
    if (!confirmed) return;
    const success = await runAction(
      () => installCombatTextProfile(profileId),
      `Installed combat text package: ${profileId}.`
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
    await runAction(clearCombatTextInstalled, "Installed combat text models cleared.");
  }

  async function onOpenInstalledProfile(url: string) {
    try {
      await openUrl(url);
    } catch (err) {
      onMessage(toErrorMessage(err, "Failed to open package link."));
    }
  }

  const resourcesUpdateTitle = resourcesUpdateAvailable
    ? `Download combat text update${resourcesUpdateVersion ? ` v${resourcesUpdateVersion}` : ""}`
    : "Check for combat text updates";
  const hasProfiles = (status?.combatTextDownloadedProfiles?.length ?? 0) > 0;
  const canClearInstalled = Boolean(status?.combatTextInstalledProfile);

  return (
    <>
      <section className="lootManager">
        <div className="lootAccordion">
        <div className="accordionHeader serviceBrowserTrigger"
            role="button"
            tabIndex={loading || !hasProfiles ? -1 : 0}
            aria-label="Browse packages"
            aria-disabled={loading || !hasProfiles}
            title={!hasProfiles ? "No downloaded packages yet" : "Browse packages"}
            onClick={() => {if (!loading && hasProfiles) openPackageBrowser();}}
            onKeyDown={event => {
              if (event.target !== event.currentTarget) return;
              if ((event.key === "Enter" || event.key === " ") && !loading && hasProfiles) {
                event.preventDefault();
                openPackageBrowser();
              }
            }}
          >
          <ServiceAccordionTitle
            title="Combat text"
            activeProfileName={activeProfileName}
            activeProfileAuthor={activeProfileAuthor}
            activeProfileVersion={activeProfileVersion}
            hasInstalledProfile={hasInstalledProfile}
          />
          <div className="headerActions" onClick={event => event.stopPropagation()}>
            {debugMode && installedProfileUrl ? (
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
                  onClick={() => runAction(openCombatTextResourcesFolder)}
                  title="Browse downloaded"
                  aria-label="Open downloaded"
                >
                  <FolderOpenIcon className="heroIcon" />
                </button>
                <button
                  type="button"
                  className="iconBtn iconBtnSubtle iconBtnDim"
                  disabled={loading}
                  onClick={() => runAction(clearCombatTextResources, "Downloaded combat text resources cleared.")}
                  title="Clear downloaded"
                  aria-label="Clear downloaded"
                >
                  <TrashIcon className="heroIcon" />
                </button>
                <button
                  type="button"
                  className="iconBtn iconBtnSubtle iconBtnDim"
                  disabled={loading}
                  onClick={() => runAction(openCombatTextItemFolder)}
                  title="Browse installed"
                  aria-label="Browse installed"
                >
                  <FolderIcon className="heroIcon" />
                </button>
              </>
            ) : null}
            {debugMode && canClearInstalled ? (
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
            
          </div>
        </div>

        <PackageBrowserModal
          resourceType="combatText"
          open={!collapsed}
          title="Combat text"
          profiles={availableProfiles}
          installedProfile={status?.combatTextInstalledProfile}
          selectedProfile={selectedProfile}
          recommendedId={recommendedProfile?.id}
          loading={loading}
          canInstall={Boolean(status?.combatTextSelectedGameItemFolder)}
          onSelect={setSelectedProfile}
          onInstall={onInstallProfile}
          onUninstall={onClearInstalled}
          onClose={() => setCollapsed(true)}
          onMessage={onMessage}
        />
        </div>
      </section>
      <ConfirmationModal
        open={clearInstalledConfirmOpen}
        title="Uninstall package"
        message="Uninstall the active package from the selected game folder?"
        confirmLabel="Uninstall"
        onConfirm={confirmClearInstalled}
        onClose={() => setClearInstalledConfirmOpen(false)}
      />
    </>
  );
}

function isRecommendedCombatTextProfile(profile: AppStatus["combatTextAvailableProfiles"][number]) {
  const name = profile.name?.trim().toLowerCase();
  const id = profile.id?.trim().toLowerCase();
  return name === "no combat text" || id === "no combat text";
}
