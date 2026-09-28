type LoadingOverlayProps = {
  visible: boolean;
  label?: string;
  progress?: number | null;
};

export function LoadingOverlay({ visible, label = "Loading...", progress = null }: LoadingOverlayProps) {
  if (!visible) {
    return null;
  }

  const boundedProgress = typeof progress === "number" && Number.isFinite(progress)
    ? Math.min(100, Math.max(0, progress))
    : null;

  return (
    <div className="loadingOverlay" role="status" aria-live="polite" aria-label={label}>
      <div className="loadingOverlayContent">
        <span className="loadingSpinner" aria-hidden="true" />
        <p className="loadingLabel">{label}</p>
        {boundedProgress !== null ? (
          <div className="loadingProgress" aria-label={`Progress ${Math.round(boundedProgress)}%`}>
            <div className="loadingProgressTrack">
              <div className="loadingProgressBar" style={{ width: `${boundedProgress}%` }} />
            </div>
            <span className="loadingProgressText">{Math.round(boundedProgress)}%</span>
          </div>
        ) : null}
      </div>
    </div>
  );
}
