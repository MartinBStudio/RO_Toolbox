import { useEffect } from "react";
import { XMarkIcon } from "@heroicons/react/24/outline";

interface ImagePreviewModalProps {
  imageUrl: string | null;
  altText?: string;
  onClose: () => void;
}

export function ImagePreviewModal({ imageUrl, altText, onClose }: ImagePreviewModalProps) {
  useEffect(() => {
    if (!imageUrl) return;

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        onClose();
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [imageUrl, onClose]);

  if (!imageUrl) return null;

  return (
    <div
      className="imagePreviewModalOverlay"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-label="Image preview"
    >
      <button
        type="button"
        className="imagePreviewModalClose iconBtn iconBtnSubtle"
        onClick={onClose}
        aria-label="Close preview"
      >
        <XMarkIcon className="heroIcon" />
      </button>
      <div className="imagePreviewModalContent" onClick={(event) => event.stopPropagation()}>
        <img
          src={imageUrl}
          alt={altText || "Preview image"}
          className="imagePreviewModalImage"
        />
      </div>
    </div>
  );
}
