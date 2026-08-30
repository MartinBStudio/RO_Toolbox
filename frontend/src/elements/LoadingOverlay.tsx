type LoadingOverlayProps = {
  visible: boolean;
  label?: string;
};

export function LoadingOverlay({ visible, label = "Loading..." }: LoadingOverlayProps) {
  if (!visible) {
    return null;
  }

  return (
    <div className="loadingOverlay" role="status" aria-live="polite" aria-label={label}>
      <div className="loadingOverlayContent">
        <span className="loadingSpinner" aria-hidden="true" />
        <p className="loadingLabel">{label}</p>
      </div>
    </div>
  );
}
