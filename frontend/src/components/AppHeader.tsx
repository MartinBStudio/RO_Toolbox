type AppHeaderProps = {
  onOpenSettings: () => void;
  onUpdateAction?: () => void;
  loading?: boolean;
  updateAvailable?: boolean;
  updateInstallable?: boolean;
  updateVersion?: string;
  backendVersion?: string;
  appVersion?: string;
};

export function AppHeader({
  onOpenSettings,
  onUpdateAction,
  loading = false,
  updateAvailable = false,
  updateInstallable = false,
  updateVersion,
  backendVersion,
  appVersion
}: AppHeaderProps) {
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
            disabled={loading || !onUpdateAction}
            onClick={onUpdateAction}
          >
            {updateAvailable ? "↓" : "⟳"}
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
