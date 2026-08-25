import type { AppStatus } from "../types";

type SettingsModalProps = {
  open: boolean;
  status: AppStatus | null;
  loading: boolean;
  onClose: () => void;
  onBrowseFolder: () => void;
  onClearGameFolder: () => void;
};

export function SettingsModal({
  open,
  status,
  loading,
  onClose,
  onBrowseFolder,
  onClearGameFolder
}: SettingsModalProps) {
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
