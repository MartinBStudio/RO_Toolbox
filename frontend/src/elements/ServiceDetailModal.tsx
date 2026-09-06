import type { ReactNode } from "react";

type ServiceDetailModalProps = {
  open: boolean;
  title: string;
  onClose: () => void;
  children: ReactNode;
};

export function ServiceDetailModal({ open, title, onClose, children }: ServiceDetailModalProps) {
  if (!open) {
    return null;
  }

  return (
    <div className="modalBackdrop" onClick={onClose}>
      <section className="card modalCard serviceDetailModalCard" onClick={(event) => event.stopPropagation()}>
        <div className="modalHeader">
          <h2>{title}</h2>
          <button type="button" className="buttonSubtle" onClick={onClose}>✕</button>
        </div>
        <div className="serviceDetailModalBody">
          {children}
        </div>
      </section>
    </div>
  );
}
