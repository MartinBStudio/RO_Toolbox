type HowToUseModalProps = {
  open: boolean;
  onClose: () => void;
};

export function HowToUseModal({ open, onClose }: HowToUseModalProps) {
  if (!open) {
    return null;
  }

  return (
    <div className="modalBackdrop" onClick={onClose}>
      <section className="card modalCard howToUseModalCard" onClick={(event) => event.stopPropagation()}>
        <div className="modalHeader">
          <h2>How to use app</h2>
          <button type="button" className="buttonSubtle" onClick={onClose}>✕</button>
        </div>

        <div className="howToUseBody">
          <p className="howToUseIntro">
            This tool helps you manage texture replacer profiles for ROSE Online.
          </p>
          <ol className="howToUseSteps">
            <li>Open <strong>Settings</strong> and choose your ROSE Online game folder.</li>
            <li>In each section, click <strong>⟳</strong> to check or <strong>↓</strong> to download profile updates.</li>
            <li>Select a profile from the dropdown.</li>
            <li>Use <strong>Install</strong> to apply the profile to your game files.</li>
            <li>Use <strong>📁</strong> to open installed files or <strong>🗑</strong> to clear them.</li>
            <li>Use <strong>🔗</strong> to visit the profile author page when available.</li>
          </ol>
          <p className="howToUseTip">
            Tip: After installing a new profile, always restart your game client.
          </p>
        </div>
      </section>
    </div>
  );
}
