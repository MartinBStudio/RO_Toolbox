import { useEffect } from "react";
import { XMarkIcon } from "@heroicons/react/24/outline";
import { LootModelPreviewCanvas } from "./LootModelPreviewCanvas";

interface LootModelPreviewModalProps {
  folder: string | null;
  profileId?: string | null;
  onClose: () => void;
}

export function LootModelPreviewModal({ folder, profileId, onClose }: LootModelPreviewModalProps) {
  useEffect(() => {
    if (!folder) return;

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        onClose();
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [folder, onClose]);

  if (!folder) return null;

  return (
    <div
      className="imagePreviewModalOverlay"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-label="Model preview"
    >
      <button
        type="button"
        className="imagePreviewModalClose iconBtn iconBtnSubtle"
        onClick={onClose}
        aria-label="Close preview"
      >
        <XMarkIcon className="heroIcon" />
      </button>
      <div className="lootModelPreviewModalContent" onClick={(event) => event.stopPropagation()}>
        <LootModelPreviewCanvas folder={folder} profileId={profileId} size="modal" />
      </div>
    </div>
  );
}
