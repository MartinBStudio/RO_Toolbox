import { useEffect, useMemo, useState } from "react";
import { check as checkTauriUpdate } from "@tauri-apps/plugin-updater";
import { relaunch } from "@tauri-apps/plugin-process";
import { open } from "@tauri-apps/plugin-dialog";
import { openUrl } from "@tauri-apps/plugin-opener";
import {
  checkBackendUpdate,
  clearGameFolder,
  clearInstalled,
  clearResources,
  downloadProfiles,
  getStatus,
  installProfile,
  openItemFolder,
  openResourcesFolder,
  saveGameFolder
} from "./api";
import { AppHeader } from "./components/AppHeader";
import { AppFooter } from "./components/AppFooter";
import { LoadingOverlay } from "./components/LoadingOverlay";
import { LootManager } from "./components/LootManager";
import { ServiceContainer } from "./components/ServiceContainer";
import { SettingsModal } from "./components/SettingsModal";
import { StatusMessage } from "./components/StatusMessage";
import type { AppStatus } from "./types";

type UpdateStatus = {
  available: boolean;
  version?: string;
  releaseUrl?: string;
  installable: boolean;
};

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

function App() {
  const [status, setStatus] = useState<AppStatus | null>(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [selectedProfile, setSelectedProfile] = useState("");
  const [updateStatus, setUpdateStatus] = useState<UpdateStatus | null>(null);

  const availableProfiles = status?.availableProfiles ?? [];
  const canInstall = useMemo(() => Boolean(selectedProfile), [selectedProfile]);

  async function refresh() {
    setStatus(await getStatus());
  }

  async function runAction(action: () => Promise<unknown>, successMessage?: string) {
    setLoading(true);
    try {
      await action();
      await refresh();
      if (successMessage) {
        setMessage(successMessage);
      }
    } catch (err) {
      setMessage(toErrorMessage(err, "Request failed."));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    refresh().catch((err) => {
      setMessage(toErrorMessage(err, "Failed to load app status."));
    });
  }, []);

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

  async function saveFolder(path: string) {
    const trimmedPath = path.trim();
    if (!trimmedPath) return;
    setLoading(true);
    try {
      const initialResult = await saveGameFolder(trimmedPath, false);
      await refresh();
      setMessage(
        initialResult.containsExpectedItemFolder
          ? "Game folder saved."
          : "Game folder saved (without 3ddata/item)."
      );
    } catch (err) {
      const text = err instanceof Error ? err.message : "Request failed.";
      if (text.includes("does not contain 3ddata/item")) {
        const confirmed = window.confirm("The folder does not contain 3ddata/item. Save it anyway?");
        if (!confirmed) {
          setMessage("Save cancelled.");
          setLoading(false);
          return;
        }
        await saveGameFolder(trimmedPath, true);
        await refresh();
        setMessage("Game folder saved.");
      } else {
        setMessage(text);
      }
    } finally {
      setLoading(false);
    }
  }

  async function onBrowseFolder() {
    try {
      const selected = await open({
        directory: true,
        multiple: false
      });
      if (!selected || Array.isArray(selected)) {
        return;
      }
      await saveFolder(selected);
    } catch (err) {
      setMessage(toErrorMessage(err, "Folder selection failed."));
    }
  }

  async function onInstallProfile() {
    if (!selectedProfile) return;
    if (!status?.selectedGameItemFolder) {
      setMessage("Set game installation folder first (must point to a base containing 3ddata/item).");
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

  async function onCheckUpdates() {
    setLoading(true);
    try {
      const update = await checkTauriUpdate();
      if (update?.available) {
        setUpdateStatus({ available: true, version: update.version, installable: true });
        setMessage(`Update available: v${update.version}`);
      } else {
        setUpdateStatus({ available: false, installable: false });
        setMessage("You are on the latest version.");
      }
    } catch (err) {
      try {
        const backendResult = await checkBackendUpdate();
        if (backendResult.success && backendResult.updateAvailable) {
          setUpdateStatus({
            available: true,
            version: backendResult.releaseVersion,
            releaseUrl: backendResult.releaseUrl,
            installable: false
          });
          setMessage(`Update available on release channel: ${backendResult.releaseVersion}`);
        } else if (backendResult.success) {
          setUpdateStatus({ available: false, installable: false });
          setMessage("You are on the latest version.");
        } else {
          setMessage(backendResult.message || toErrorMessage(err, "Update check failed."));
        }
      } catch (backendErr) {
        setMessage(
          `Update check failed. ${toErrorMessage(err, "Tauri updater error.")} ${toErrorMessage(backendErr, "Backend updater error.")}`
        );
      }
    } finally {
      setLoading(false);
    }
  }

  async function onInstallUpdate() {
    setLoading(true);
    try {
      const update = await checkTauriUpdate();
      if (!update?.available) {
        setMessage("No update available.");
        return;
      }
      setMessage("Downloading update...");
      await update.downloadAndInstall();
      setMessage("Update installed. Restarting...");
      await relaunch();
    } catch (err) {
      setMessage(toErrorMessage(err, "Update failed."));
    } finally {
      setLoading(false);
    }
  }

  async function onOpenInstalledProfile(url: string) {
    try {
      await openUrl(url);
    } catch (err) {
      setMessage(toErrorMessage(err, "Failed to open profile link."));
    }
  }

  async function onOpenReleaseUrl(url: string) {
    try {
      await openUrl(url);
    } catch (err) {
      setMessage(toErrorMessage(err, "Failed to open release page."));
    }
  }

  return (
    <main className={`layout${loading ? " layoutLoading" : ""}`}>
      <AppHeader
        onOpenSettings={() => setSettingsOpen(true)}
        onCheckUpdates={onCheckUpdates}
        loading={loading}
        updateAvailable={Boolean(updateStatus?.available)}
        currentVersion={status?.version}
      />

      <ServiceContainer
        groups={[
          {
            id: "texture-replacer",
            title: "Texture replacer",
            content: (
              <LootManager
                status={status}
                loading={loading}
                selectedProfile={selectedProfile}
                canInstall={canInstall}
                onSelectedProfileChange={setSelectedProfile}
                onDownloadProfiles={() => runAction(downloadProfiles, "Profiles downloaded.")}
                onOpenResourcesFolder={() => runAction(openResourcesFolder, "Opened resources folder.")}
                onClearResources={() => runAction(clearResources, "Downloaded resources cleared.")}
                onInstallProfile={onInstallProfile}
                onOpenItemFolder={() => runAction(openItemFolder, "Opened installed folder.")}
                onClearInstalled={onClearInstalled}
                onOpenInstalledProfile={onOpenInstalledProfile}
              />
            )
          }
        ]}
      />

      {updateStatus?.available ? (
        <section className="card">
          <h2>Update available</h2>
          <p>Version: {updateStatus.version ?? "Unknown"}</p>
          <div className="row">
            {updateStatus.installable ? (
              <button disabled={loading} onClick={onInstallUpdate}>
                Install update
              </button>
            ) : (
              <button
                disabled={loading || !updateStatus.releaseUrl}
                onClick={() => updateStatus.releaseUrl && onOpenReleaseUrl(updateStatus.releaseUrl)}
              >
                View release
              </button>
            )}
          </div>
        </section>
      ) : null}

      <StatusMessage message={message} />

      <AppFooter />

      <SettingsModal
        open={settingsOpen}
        status={status}
        loading={loading}
        onClose={() => setSettingsOpen(false)}
        onBrowseFolder={onBrowseFolder}
        onClearGameFolder={() => runAction(clearGameFolder, "Game folder cleared.")}
        onOpenItemFolder={() => runAction(openItemFolder, "Opened item folder.")}
      />
      <LoadingOverlay visible={loading} />
    </main>
  );
}

export default App;
