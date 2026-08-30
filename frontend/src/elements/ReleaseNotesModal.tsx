type ReleaseNotesModalProps = {
  open: boolean;
  content: string;
  onClose: () => void;
};

export function ReleaseNotesModal({ open, content, onClose }: ReleaseNotesModalProps) {
  if (!open) {
    return null;
  }

  return (
    <div className="modalBackdrop" onClick={onClose}>
      <section className="card modalCard releaseNotesModalCard" onClick={(event) => event.stopPropagation()}>
        <div className="modalHeader">
          <h2>What&apos;s new</h2>
          <button type="button" className="buttonSubtle" onClick={onClose}>✕</button>
        </div>
        <pre className="releaseNotesContent">{content}</pre>
      </section>
    </div>
  );
}
