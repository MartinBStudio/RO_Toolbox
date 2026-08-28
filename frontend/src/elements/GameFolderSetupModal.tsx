import { open as openDialog } from "@tauri-apps/plugin-dialog";
import { saveGameFolder } from "../backendConnector/api.ts";

type GameFolderSetupModalProps = {
  onBusyChange: (busy: boolean) => void;
  onStatusRefresh: () => Promise<void>;
  onMessage: (message: string) => void;
  loading: boolean;
};

export function GameFolderSetupModal({
  onBusyChange,
  onStatusRefresh,
  onMessage,
  loading
}: GameFolderSetupModalProps) {
  function toErrorMessage(err: unknown, fallback: string) {
    if (err instanceof Error && err.message) return err.message;
    if (typeof err === "string" && err.trim()) return err;
    if (err && typeof err === "object" && "message" in err && typeof err.message === "string") return err.message;
    return fallback;
  }

  async function saveFolder(path: string) {
    const trimmedPath = path.trim();
    if (!trimmedPath) return;
    onBusyChange(true);
    try {
      const result = await saveGameFolder(trimmedPath, false);
      await onStatusRefresh();
      if (!result.containsExpectedItemFolder) {
        onMessage("Game folder saved (3ddata/item not found — install may not work correctly).");
      }
    } catch (err) {
      const text = toErrorMessage(err, "Request failed.");
      if (text.includes("does not contain 3ddata/item")) {
        const confirmed = window.confirm("The folder does not contain 3ddata/item. Save it anyway?");
        if (!confirmed) {
          onBusyChange(false);
          return;
        }
        await saveGameFolder(trimmedPath, true);
        await onStatusRefresh();
      } else {
        onMessage(text);
      }
    } finally {
      onBusyChange(false);
    }
  }

  async function onBrowseFolder() {
    try {
      const selected = await openDialog({ directory: true, multiple: false });
      if (!selected || Array.isArray(selected)) return;
      await saveFolder(selected);
    } catch (err) {
      onMessage(toErrorMessage(err, "Folder selection failed."));
    }
  }

  return (
    <div className="modalBackdrop">
      <section className="card modalCard setupModal" onClick={(e) => e.stopPropagation()}>
        <div className="setupModalIcon">🎮</div>
        <h2 className="setupModalTitle">Select your game folder</h2>
        <p className="setupModalDesc">
          RO Toolbox needs to know where your ROSE Online is installed.
          Point it to the root game folder (the one containing <code>3ddata</code>).
        </p>
        <div className="modalActions">
          <button className="buttonStrong" disabled={loading} onClick={onBrowseFolder}>
            📂 Browse…
          </button>
        </div>
      </section>
    </div>
  );
}
