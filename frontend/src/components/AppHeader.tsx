type AppHeaderProps = {
  onOpenSettings: () => void;
  onCheckUpdates?: () => void;
  loading?: boolean;
  updateAvailable?: boolean;
  backendVersion?: string;
  appVersion?: string;
};

export function AppHeader({
  onOpenSettings,
  onCheckUpdates,
  loading = false,
  updateAvailable = false,
  backendVersion,
  appVersion
}: AppHeaderProps) {
  return (
    <header className="card">
      <div className="headerRow">
        <h1>RO Toolbox</h1>
        <div className="headerActions">
          <span className="versionMeta">App v{appVersion ?? "..."} | Backend v{backendVersion ?? "..."}</span>
          <button
            type="button"
            className={`settingsCog updateCog${updateAvailable ? " updateAvailable" : ""}`}
            aria-label="Check for updates"
            title={updateAvailable ? "Update available. Click to check again." : "Check for updates"}
            disabled={loading || !onCheckUpdates}
            onClick={onCheckUpdates}
          >
            ⟳
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
