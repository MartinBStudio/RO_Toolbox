import type { ReactNode } from "react";

type ServiceDetailModalProps = {
  open: boolean;
  title: string;
  description?: string;
  onClose: () => void;
  children: ReactNode;
};

export function ServiceDetailModal({ open, title, description, onClose, children }: ServiceDetailModalProps) {
  if (!open) {
    return null;
  }

  return (
    <div className="modalBackdrop" onClick={onClose}>
      <section className="card modalCard serviceDetailModalCard" onClick={(event) => event.stopPropagation()}>
        <div className="modalHeader">
          <div className="serviceDetailModalHeaderBlock">
            <p className="serviceDetailModalEyebrow">Package browser</p>
            <h2 className="serviceDetailModalTitle">{title}</h2>
            {description ? <p className="serviceDetailModalDescription">{description}</p> : null}
          </div>
          <button type="button" className="buttonSubtle serviceDetailModalClose" onClick={onClose} aria-label="Close package browser">✕</button>
        </div>
        <div className="serviceDetailModalBody">
          {children}
        </div>
      </section>
    </div>
  );
}
