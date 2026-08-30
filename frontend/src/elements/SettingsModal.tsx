import { open as openDialog } from "@tauri-apps/plugin-dialog";
import { useState } from "react";
import type { AppStatus } from "../types.ts";
import { clearGameFolder, factoryReset, saveGameFolder } from "../backendConnector/api.ts";
import { useApplicationContext } from "../context/ApplicationContext.tsx";
import { ConfirmationModal } from "./ConfirmationModal.tsx";

type SettingsModalProps = {
  open: boolean;
  status: AppStatus | null;
  loading: boolean;
  onClose: () => void;
  onBusyChange: (busy: boolean) => void;
  onStatusRefresh: () => Promise<void>;
  onMessage: (message: string) => void;
};

export function SettingsModal({
  open,
  status,
  loading,
  onClose,
  onBusyChange,
  onStatusRefresh,
  onMessage
}: SettingsModalProps) {
  const { debugMode, setDebugMode } = useApplicationContext();
  const [factoryResetOpen, setFactoryResetOpen] = useState(false);

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

  async function onClearGameFolder() {
    await runAction(clearGameFolder, "Game folder cleared.");
  }

  async function onFactoryResetConfirmed() {
    setFactoryResetOpen(false);
    onClose();
    setDebugMode(false);
    window.localStorage.removeItem("roToolbox.debugMode");
    onBusyChange(true);
    try {
      await factoryReset();
      await onStatusRefresh();
      onMessage("Factory reset complete.");
      window.dispatchEvent(new Event("roToolbox:factory-reset"));
    } catch (err) {
      onMessage(toErrorMessage(err, "Factory reset failed."));
    } finally {
      onBusyChange(false);
    }
  }

  async function saveFolder(path: string) {
    const trimmedPath = path.trim();
    if (!trimmedPath) return;
    onBusyChange(true);
    try {
      const initialResult = await saveGameFolder(trimmedPath, false);
      await onStatusRefresh();
      onMessage(
        initialResult.containsExpectedItemFolder
          ? "Game folder saved."
          : "Game folder saved. Some mod folders may need to be created in this installation."
      );
    } catch (err) {
      const text = err instanceof Error ? err.message : "Request failed.";
      if (text.includes("trose.exe") || text.includes("not valid")) {
        onMessage("The selected folder is not valid. It must contain trose.exe.");
      } else {
        onMessage(text);
      }
    } finally {
      onBusyChange(false);
    }
  }

  async function onBrowseFolder() {
    try {
      const selected = await openDialog({
        directory: true,
        multiple: false
      });
      if (!selected || Array.isArray(selected)) {
        return;
      }
      await saveFolder(selected);
    } catch (err) {
      onMessage(toErrorMessage(err, "Folder selection failed."));
    }
  }

  if (!open) {
    return null;
  }

  const gameFolderSet = Boolean(status?.selectedGameBase);

  return (
    <div className="modalBackdrop" onClick={onClose}>
      <section className="card modalCard" onClick={(event) => event.stopPropagation()}>
        <div className="modalHeader">
          <h2>Settings</h2>
          <button type="button" className="buttonSubtle" onClick={onClose}>✕</button>
        </div>

        <div className="settingsSection">
          <p className="settingsSectionLabel">ROSE Online folder</p>
          <div className={`settingsFolderDisplay${gameFolderSet ? "" : " settingsFolderEmpty"}`}>
            {status?.selectedGameBase ?? "Not set"}
          </div>
          <div className="settingsFolderActions">
            <button className="buttonStrong" disabled={loading} onClick={onBrowseFolder}>
              📂 Browse…
            </button>
            {gameFolderSet && (
              <button className="buttonSubtle" disabled={loading} onClick={onClearGameFolder}>
                🗑 Clear
              </button>
            )}
          </div>
        </div>

        <div className="settingsSection settingsSectionSeparated">
          <div className="settingsToggleRow">
            <div>
              <p className="settingsSectionLabel">Debug mode</p>
              <p className="settingsToggleHelp">Show debug information.</p>
            </div>
            <label className="settingsToggle" aria-label="Toggle debug mode">
              <input
                type="checkbox"
                checked={debugMode}
                onChange={(event) => setDebugMode(event.target.checked)}
              />
              <span className="settingsToggleTrack" aria-hidden="true">
                <span className="settingsToggleThumb" />
              </span>
            </label>
          </div>
        </div>

        <div className="settingsSection settingsSectionSeparated">
          <p className="settingsSectionLabel">Danger zone</p>
          <button type="button" className="buttonDanger" disabled={loading} onClick={() => setFactoryResetOpen(true)}>
            Factory reset
          </button>
        </div>

        <ConfirmationModal
          open={factoryResetOpen}
          title="Factory reset"
          message="This clears RO_Toolbox settings and installed mod data. Downloaded profiles and local app state will be removed, while the real ROSE Online config in AppData/Rednim Games remains untouched."
          confirmLabel="Reset now"
          onConfirm={onFactoryResetConfirmed}
          onClose={() => setFactoryResetOpen(false)}
        />
      </section>
    </div>
  );
}
