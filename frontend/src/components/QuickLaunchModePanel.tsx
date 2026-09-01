import type { LoginAccount } from "../backendConnector/api.ts";

type QuickLaunchModePanelProps = {
  accounts: LoginAccount[];
  loading: boolean;
  launchDisabled: boolean;
  onLaunchAccount: (account: LoginAccount) => void;
  onExit: () => void;
};

export function QuickLaunchModePanel({
  accounts,
  loading,
  launchDisabled,
  onLaunchAccount,
  onExit
}: QuickLaunchModePanelProps) {
  return (
    <section className="card quickLaunchOnlyPanel">
      <div className="quickLaunchOnlyContent">
        <div className="quickLaunchOnlyScroller">
          <div className="quickLaunchOnlyActions">
            {accounts.map((account) => (
              <button
                key={account.id}
                type="button"
                className="quickAccountButton"
                onClick={() => onLaunchAccount(account)}
                disabled={loading || launchDisabled}
                title={`Launch ${account.name}`}
              >
                <span className="quickAccountIcon" aria-hidden="true">{account.icon || "👤"}</span>
                <span className="quickAccountName">{account.name}</span>
              </button>
            ))}
            <button
              type="button"
              className="buttonSubtle quickLaunchOnlyExitButton"
              onClick={onExit}
            >
              Full mode
            </button>
          </div>
        </div>
      </div>
    </section>
  );
}
