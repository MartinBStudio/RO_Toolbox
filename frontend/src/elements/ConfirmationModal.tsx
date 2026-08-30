type ConfirmationModalProps = {
  open: boolean;
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  confirmButtonClassName?: string;
  onConfirm: () => void | Promise<void>;
  onClose: () => void;
};

export function ConfirmationModal({
  open,
  title,
  message,
  confirmLabel = "Confirm",
  cancelLabel = "Cancel",
  confirmButtonClassName = "buttonDanger",
  onConfirm,
  onClose
}: ConfirmationModalProps) {
  if (!open) {
    return null;
  }

  return (
    <div className="modalBackdrop" onClick={onClose}>
      <section className="card modalCard confirmationModal" onClick={(event) => event.stopPropagation()}>
        <div className="modalHeader confirmationHeader">
          <div className="confirmationTitleBlock">
            <div className="confirmationIcon" aria-hidden="true">⚠</div>
            <h2>{title}</h2>
          </div>
          <button type="button" className="buttonSubtle" onClick={onClose}>✕</button>
        </div>

        <p className="confirmationMessage">{message}</p>

        <div className="modalActions confirmationActions">
          <button type="button" className="buttonSubtle" onClick={onClose}>
            {cancelLabel}
          </button>
          <button type="button" className={confirmButtonClassName} onClick={onConfirm}>
            {confirmLabel}
          </button>
        </div>
      </section>
    </div>
  );
}
