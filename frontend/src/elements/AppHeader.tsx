import { useEffect, useState } from "react";
import { getCurrentWindow } from "@tauri-apps/api/window";
import { invoke } from "@tauri-apps/api/core";
import { check as checkTauriUpdate } from "@tauri-apps/plugin-updater";
import { relaunch } from "@tauri-apps/plugin-process";
import { openUrl } from "@tauri-apps/plugin-opener";
import {
  ArrowDownTrayIcon,
  ArrowPathIcon,
  Cog6ToothIcon,
  QuestionMarkCircleIcon
} from "@heroicons/react/24/outline";
import { checkBackendUpdate, fetchLatestReleaseDownload } from "../backendConnector/api.ts";
import { useApplicationContext } from "../context/ApplicationContext.tsx";

type AppHeaderProps = {
  onOpenSettings: () => void;
  onOpenHowToUse: () => void;
  onLaunchRose: () => void;
  onBusyChange: (busy: boolean, message?: string) => void;
  onMessage: (message: string) => void;
  onQuickLaunchAccount?: (account: any) => void;
  quickAccounts?: any[];
  loading?: boolean;
  launchDisabled?: boolean;
  backendVersion?: string;
  appVersion?: string;
};

export function AppHeader({
  onOpenSettings,
  onOpenHowToUse,
  onLaunchRose,
  onBusyChange,
  onMessage,
  onQuickLaunchAccount,
  quickAccounts = [],
  loading = false,
  launchDisabled = false,
  backendVersion,
  appVersion
}: AppHeaderProps) {
  const { backendReady, debugMode } = useApplicationContext();
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

  function resolveDownloadFileName(response: Response) {
    const contentDisposition = response.headers.get("content-disposition");
    if (contentDisposition) {
      const encodedMatch = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
      if (encodedMatch?.[1]) {
        return decodeURIComponent(encodedMatch[1]);
      }
      const plainMatch = contentDisposition.match(/filename="([^"]+)"/i);
      if (plainMatch?.[1]) {
        return plainMatch[1];
      }
    }
    return "RO_Toolbox.jar";
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
      // Kill the bundled Java backend before installing so its JRE files
      // are not locked — otherwise Windows schedules them for reboot replacement.
      await invoke("stop_backend").catch(() => undefined);
      await update.downloadAndInstall();
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

  async function onDownloadLatestRelease() {
    onBusyChange(true, "Downloading latest release...");
    try {
      const response = await fetchLatestReleaseDownload();
      if (!response.body) {
        throw new Error("Download stream is unavailable.");
      }

      const totalHeader = response.headers.get("content-length");
      const total = totalHeader ? Number.parseInt(totalHeader, 10) : 0;
      const reader = response.body.getReader();
      let downloaded = 0;

      while (true) {
        const { done, value } = await reader.read();
        if (done) {
          break;
        }
        if (!value) {
          continue;
        }

        downloaded += value.byteLength;

        if (total > 0 || downloaded > 0) {
          onBusyChange(true, "Downloading latest release...");
        }
      }

      const fileName = resolveDownloadFileName(response);
      onMessage(`Downloaded ${fileName} for test. The app was not updated.`);
    } catch (err) {
      onMessage(toErrorMessage(err, "Latest release download failed."));
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
        <h1 className="headerTitle">
          <img src="/src/assets/rose-logo-bg.webp" alt="ROSE" className="headerLogo" />
          Toolbox
        </h1>
        <div className="headerActions">
          <span className="versionMeta">App v{appVersion ?? "..."} | Backend v{backendVersion ?? "..."}</span>
          <button
            type="button"
            className="buttonSubtle headerLaunchButton"
            disabled={loading || launchDisabled}
            onClick={onLaunchRose}
          >
            Launch Rose Launcher
          </button>
          <button
            type="button"
            className={`settingsCog updateCog${updateAvailable ? " updateAvailable" : ""}`}
            aria-label={updateTitle}
            title={updateTitle}
            disabled={loading || updateChecking}
            onClick={onUpdateAction}
          >
            {updateAvailable ? <ArrowDownTrayIcon className="heroIcon" /> : <ArrowPathIcon className="heroIcon" />}
          </button>
          {debugMode && (
            <button
              type="button"
              className="settingsCog downloadTestCog"
              aria-label="Download the latest release package for testing"
              title="Download the latest release package for testing"
              disabled={loading || updateChecking}
              onClick={onDownloadLatestRelease}
            >
              <ArrowDownTrayIcon className="heroIcon" />
            </button>
          )}
          <button
            type="button"
            className="settingsCog"
            aria-label="Open how to use guide"
            onClick={onOpenHowToUse}
          >
            <QuestionMarkCircleIcon className="heroIcon" />
          </button>
          <button
            type="button"
            className="settingsCog"
            aria-label="Open settings"
            onClick={onOpenSettings}
          >
            <Cog6ToothIcon className="heroIcon" />
          </button>
        </div>
      </div>
      {quickAccounts.length > 0 && (
        <div className="headerQuickLaunch">
          {quickAccounts.map((account) => (
            <button
              key={account.id}
              type="button"
              className="quickAccountButton"
              onClick={() => onQuickLaunchAccount?.(account)}
              disabled={loading || launchDisabled}
              title={`Launch ${account.name}`}
            >
              <span className="quickAccountIcon" aria-hidden="true">{account.icon || "👤"}</span>
              <span className="quickAccountName">{account.name}</span>
            </button>
          ))}
        </div>
      )}
    </header>
  );
}
