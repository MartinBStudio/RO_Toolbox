import { open as openDialog } from "@tauri-apps/plugin-dialog";
import type { AppStatus } from "../types.ts";
import { clearGameFolder, saveGameFolder } from "../backendConnector/api.ts";

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
          : "Game folder saved (without 3ddata/item)."
      );
    } catch (err) {
      const text = err instanceof Error ? err.message : "Request failed.";
      if (text.includes("does not contain 3ddata/item")) {
        const confirmed = window.confirm("The folder does not contain 3ddata/item. Save it anyway?");
        if (!confirmed) {
          onMessage("Save cancelled.");
          onBusyChange(false);
          return;
        }
        await saveGameFolder(trimmedPath, true);
        await onStatusRefresh();
        onMessage("Game folder saved.");
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

  return (
    <div className="modalBackdrop" onClick={onClose}>
      <section className="card modalCard" onClick={(event) => event.stopPropagation()}>
        <div className="modalHeader">
          <h2>Settings</h2>
          <button type="button" onClick={onClose}>
            Close
          </button>
        </div>
        <p>Game folder: {status?.selectedGameBase ?? "Not set"}</p>
        <p>Item folder: {status?.selectedGameItemFolder ?? "Not set"}</p>
        <div className="row modalActions">
          <button className="buttonStrong" disabled={loading} onClick={onBrowseFolder}>
            📂 Set folder
          </button>
          <button className="buttonSubtle" disabled={loading} onClick={onClearGameFolder}>
            🗑 Clear
          </button>
        </div>
      </section>
    </div>
  );
}
