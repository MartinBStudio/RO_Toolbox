import { useEffect, useState } from "react";
import { getCurrentWindow } from "@tauri-apps/api/window";
import { check as checkTauriUpdate, type DownloadEvent } from "@tauri-apps/plugin-updater";
import { relaunch } from "@tauri-apps/plugin-process";
import { openUrl } from "@tauri-apps/plugin-opener";
import { checkBackendUpdate } from "../backendConnector/api.ts";
import { useApplicationContext } from "../context/ApplicationContext.tsx";

type AppHeaderProps = {
  onOpenSettings: () => void;
  onOpenHowToUse: () => void;
  onBusyChange: (busy: boolean, message?: string) => void;
  onMessage: (message: string) => void;
  loading?: boolean;
  backendVersion?: string;
  appVersion?: string;
};

export function AppHeader({
  onOpenSettings,
  onOpenHowToUse,
  onBusyChange,
  onMessage,
  loading = false,
  backendVersion,
  appVersion
}: AppHeaderProps) {
  const { backendReady } = useApplicationContext();
  const [updateChecking, setUpdateChecking] = useState(false);
  const [updateAvailable, setUpdateAvailable] = useState(false);
  const [updateInstallable, setUpdateInstallable] = useState(false);
  const [updateVersion, setUpdateVersion] = useState<string | undefined>(undefined);
  const [releaseUrl, setReleaseUrl] = useState<string | undefined>(undefined);

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

  async function checkForUpdates(showUpToDateMessage = false) {
    setUpdateChecking(true);
    try {
      const update = await checkTauriUpdate();
      if (update?.available) {
        setUpdateAvailable(true);
        setUpdateInstallable(true);
        setUpdateVersion(update.version);
        setReleaseUrl(undefined);
      } else {
        setUpdateAvailable(false);
        setUpdateInstallable(false);
        setUpdateVersion(undefined);
        setReleaseUrl(undefined);
        if (showUpToDateMessage) {
          onMessage("You are up to date.");
        }
      }
    } catch (err) {
      try {
        const backendResult = await checkBackendUpdate();
        if (backendResult.success && backendResult.updateAvailable) {
          setUpdateAvailable(true);
          setUpdateInstallable(false);
          setUpdateVersion(backendResult.releaseVersion);
          setReleaseUrl(backendResult.releaseUrl);
        } else if (backendResult.success) {
          setUpdateAvailable(false);
          setUpdateInstallable(false);
          setUpdateVersion(undefined);
          setReleaseUrl(undefined);
          if (showUpToDateMessage) {
            onMessage("You are up to date.");
          }
        } else {
          onMessage(backendResult.message || toErrorMessage(err, "Update check failed."));
        }
      } catch (backendErr) {
        onMessage(
          `Update check failed. ${toErrorMessage(err, "Tauri updater error.")} ${toErrorMessage(backendErr, "Backend updater error.")}`
        );
      }
    } finally {
      setUpdateChecking(false);
    }
  }

  async function onInstallUpdate() {
    onBusyChange(true, "Downloading update...");
    try {
      const update = await checkTauriUpdate();
      if (!update?.available) {
        onMessage("No update available.");
        return;
      }
      let downloaded = 0;
      let total = 0;
      await update.download((event: DownloadEvent) => {
        switch (event.event) {
          case "Started":
            total = event.data.contentLength ?? 0;
            onBusyChange(true, "Downloading update...");
            break;
          case "Progress":
            downloaded += event.data.chunkLength;
            if (total > 0) {
              const pct = Math.min(100, Math.round((downloaded / total) * 100));
              onBusyChange(true, `Downloading update... ${pct}%`);
            } else {
              onBusyChange(true, "Downloading update...");
            }
            break;
          case "Finished":
            onBusyChange(true, "Installing update...");
            break;
        }
      });
      await update.install();
      onMessage("Update installed. Restarting...");
      await new Promise((resolve) => window.setTimeout(resolve, 1200));
      try {
        await relaunch();
      } catch (restartErr) {
        onMessage(`Update installed. Please reopen the app manually. ${toErrorMessage(restartErr, "")}`.trim());
        await getCurrentWindow().close().catch(() => undefined);
      }
    } catch (err) {
      onMessage(toErrorMessage(err, "Update failed."));
    } finally {
      onBusyChange(false);
    }
  }

  async function onOpenReleaseUrl(url: string) {
    try {
      await openUrl(url);
    } catch (err) {
      onMessage(toErrorMessage(err, "Failed to open release page."));
    }
  }

  async function onUpdateAction() {
    if (!updateAvailable) {
      await checkForUpdates(true);
      return;
    }
    if (updateInstallable) {
      await onInstallUpdate();
      return;
    }
    if (releaseUrl) {
      await onOpenReleaseUrl(releaseUrl);
    }
  }

  useEffect(() => {
    if (!backendReady) {
      return;
    }
    checkForUpdates().catch((err) => {
      onMessage(toErrorMessage(err, "Update check failed."));
    });
  }, [backendReady]);

  const updateTitle = updateAvailable
    ? (updateInstallable
      ? `Install update${updateVersion ? ` v${updateVersion}` : ""}`
      : "Update available. Open release page.")
    : "You are up to date.";

  return (
    <header className="card">
      <div className="headerRow">
        <h1>RO Toolbox</h1>
        <div className="headerActions">
          <span className="versionMeta">App v{appVersion ?? "..."} | Backend v{backendVersion ?? "..."}</span>
          <button
            type="button"
            className={`settingsCog updateCog${updateAvailable ? " updateAvailable" : ""}`}
            aria-label={updateTitle}
            title={updateTitle}
            disabled={loading || updateChecking}
            onClick={onUpdateAction}
          >
            {updateAvailable ? "↓" : "⟳"}
          </button>
          <button
            type="button"
            className="settingsCog"
            aria-label="Open how to use guide"
            onClick={onOpenHowToUse}
          >
            ?
          </button>
          <button
            type="button"
            className="settingsCog"
            aria-label="Open settings"
            onClick={onOpenSettings}
          >
            ⚙
          </button>
        </div>
      </div>
    </header>
  );
}
