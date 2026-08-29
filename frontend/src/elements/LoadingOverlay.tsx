type LoadingOverlayProps = {
  visible: boolean;
  label?: string;
};

export function LoadingOverlay({ visible, label = "Loading..." }: LoadingOverlayProps) {
  if (!visible) {
    return null;
  }

  const progressMatch = label.match(/(\d+)%/);
  const progressValue = progressMatch ? Number(progressMatch[1]) : 0;

  return (
    <div className="loadingOverlay" role="status" aria-live="polite" aria-label={label}>
      <div className="loadingOverlayContent">
        <span className="loadingSpinner" aria-hidden="true" />
        <div className="loadingProgressWrap" aria-hidden="true">
          <div className="loadingProgressBar" style={{ width: `${Math.min(100, Math.max(0, progressValue))}%` }} />
        </div>
        <p className="loadingLabel">{label}</p>
      </div>
    </div>
  );
}
