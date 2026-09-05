type TroseRunningBannerProps = {
  running: boolean;
  loading: boolean;
  onRefresh: () => void | Promise<void>;
};

import { ArrowPathIcon } from "@heroicons/react/24/outline";

export function TroseRunningBanner({ running, loading, onRefresh }: TroseRunningBannerProps) {
  return (
    <section className={`troseStatusBanner${running ? " troseStatusBannerRunning" : ""}`}>
      <span className="troseStatusIndicator" aria-hidden="true" />
      <span className="troseStatusText">
        {running
          ? "ROSE is running. Close it before making changes."
          : "ROSE client is not running."}
      </span>
      <button
        type="button"
        className="buttonSubtle troseStatusRefreshButton"
        onClick={() => void onRefresh()}
        disabled={loading}
        aria-label="Refresh ROSE status"
        title="Refresh ROSE status"
      >
        <ArrowPathIcon className="troseStatusRefreshIcon" aria-hidden="true" />
      </button>
    </section>
  );
}
