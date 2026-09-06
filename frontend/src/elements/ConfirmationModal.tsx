type ConfirmationModalProps = {
  open: boolean;
  title: string;
  message: string;
  smallMode?: boolean;
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
  smallMode = false,
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
    <div className={`modalBackdrop${smallMode ? " modalBackdropSmall" : ""}`} onClick={onClose}>
      <section className={`card modalCard confirmationModal${smallMode ? " confirmationModalSmall" : ""}`} onClick={(event) => event.stopPropagation()}>
        <div className="modalHeader confirmationHeader">
          <div className={`confirmationTitleBlock${smallMode ? " confirmationTitleBlockSmall" : ""}`}>
            {!smallMode && <div className="confirmationIcon" aria-hidden="true">⚠</div>}
            <h2>{title}</h2>
          </div>
          {!smallMode && <button type="button" className="buttonSubtle" onClick={onClose}>✕</button>}
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
