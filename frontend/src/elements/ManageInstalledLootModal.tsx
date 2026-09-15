import { useEffect, useState } from "react";
import { PhotoIcon, XMarkIcon } from "@heroicons/react/24/outline";
import type { LootFolderInfo } from "../utils/lootDictionary";
import {
  getFolderDescription,
  getFolderLabel,
  getFolderPreviews,
  loadItemPreviews,
  loadLootDictionary
} from "../utils/lootDictionary";
import { ImagePreviewModal } from "./ImagePreviewModal";

interface ManageInstalledLootModalProps {
  isOpen: boolean;
  profileName?: string | null;
  showPreviews?: boolean;
  managedSubfolders: string[];
  disabledManagedSubfolders: string[];
  onClose: () => void;
  onSave: (disabledManagedSubfolders: string[]) => Promise<void> | void;
}

export function ManageInstalledLootModal({
  isOpen,
  profileName,
  showPreviews,
  managedSubfolders,
  disabledManagedSubfolders,
  onClose,
  onSave
}: ManageInstalledLootModalProps) {
  const [dictionary, setDictionary] = useState<Record<string, LootFolderInfo>>({});
  const [previews, setPreviews] = useState<Record<string, string[]>>({});
  const [pendingDisabled, setPendingDisabled] = useState<Set<string>>(new Set());
  const [saving, setSaving] = useState(false);
  const [selectedPreviewImage, setSelectedPreviewImage] = useState<string | null>(null);

  const displayPreviews = showPreviews ?? Boolean(profileName?.toLowerCase().includes("farming meta"));

  useEffect(() => {
    const promises: [Promise<Record<string, LootFolderInfo>>, Promise<Record<string, string[]>> | Promise<Record<string, string[]>>] = [
      loadLootDictionary(),
      displayPreviews ? loadItemPreviews() : Promise.resolve({})
    ];
    Promise.all(promises).then(([dict, prevs]) => {
      setDictionary(dict);
      setPreviews(prevs);
    });
  }, [displayPreviews]);

  useEffect(() => {
    if (isOpen) {
      setPendingDisabled(new Set((disabledManagedSubfolders ?? []).map((folder) => folder.trim()).filter(Boolean)));
    }
  }, [isOpen, disabledManagedSubfolders]);

  if (!isOpen) return null;

  const matchesDisabledFolder = (folder: string, disabledFolder: string) =>
    folder.trim().toLowerCase() === disabledFolder.trim().toLowerCase();

  const isFolderDisabled = (folder: string) =>
    Array.from(pendingDisabled).some((disabledFolder) => matchesDisabledFolder(folder, disabledFolder));

  const handleToggle = (folder: string) => {
    setPendingDisabled((current) => {
      const next = new Set(current);
      const existing = Array.from(next).find((disabledFolder) => matchesDisabledFolder(folder, disabledFolder));
      if (existing) {
        next.delete(existing);
      } else {
        next.add(folder);
      }
      return next;
    });
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      await onSave(Array.from(pendingDisabled));
      onClose();
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <div className="lootManageModalOverlay" onClick={onClose}>
        <div className="lootManageModal" onClick={(event) => event.stopPropagation()}>
          <div className="lootManageHeader">
            <div>
              <h2 className="lootManageTitle">Manage installed packages</h2>
              <p className="lootManageSubtitle">
                Choose what parts of the package you want to enable.
              </p>
            </div>
            <button type="button" onClick={onClose} className="iconBtn iconBtnSubtle" aria-label="Close">
              <XMarkIcon className="heroIcon" />
            </button>
          </div>

          {managedSubfolders.length === 0 ? (
            <p className="lootManageEmpty">No managed folders available.</p>
          ) : (
            <div className="lootManageTableWrap">
              <table className="lootManageTable">
                <thead>
                  <tr>
                    <th className="lootManageToggleHeader">Enabled</th>
                    <th style={{ width: displayPreviews ? "12%" : "15%" }}>Folder</th>
                    {displayPreviews ? (
                      <th style={{ width: "160px", minWidth: "120px" }}>Preview</th>
                    ) : null}
                    <th style={{ width: displayPreviews ? "18%" : "25%" }}>Label</th>
                    <th>Description</th>
                  </tr>
                </thead>
                <tbody>
                  {managedSubfolders.map((folder) => {
                    const isDisabled = isFolderDisabled(folder);
                    const label = getFolderLabel(folder, dictionary);
                    const description = getFolderDescription(folder, dictionary) || "—";
                    const folderPreviews = displayPreviews ? getFolderPreviews(folder, previews) : [];

                    return (
                      <tr key={folder}>
                        <td className="lootManageToggleCell">
                          <label
                            className={`lootManageCheckbox ${isDisabled ? "isDisabled" : "isEnabled"}`}
                            aria-label={`${folder} ${isDisabled ? "disabled" : "enabled"}`}
                          >
                            <input
                              type="checkbox"
                              checked={!isDisabled}
                              onChange={() => handleToggle(folder)}
                            />
                            <span className="lootManageCheckboxBox" aria-hidden="true">
                              <svg viewBox="0 0 12 12" className="lootManageCheckboxMark">
                                <path className="lootManageCheckboxCheck" d="M2.5 6.3L4.7 8.5L9.5 3.7" />
                                <path className="lootManageCheckboxCross" d="M3 3L9 9M9 3L3 9" />
                              </svg>
                            </span>
                          </label>
                        </td>
                        <td className="lootManageFolder">{folder}</td>
                        {displayPreviews ? (
                          <td>
                            {folderPreviews.length > 0 ? (
                              <div className="lootFolderPreviewThumbnails">
                                {folderPreviews.map((imgUrl, imgIndex) => (
                                  <button
                                    key={`${folder}-img-${imgIndex}`}
                                    type="button"
                                    className="lootFolderPreviewThumbBtn"
                                    onClick={() => setSelectedPreviewImage(imgUrl)}
                                    title={`Click to view ${folder} preview ${imgIndex + 1}`}
                                    aria-label={`View ${folder} preview ${imgIndex + 1}`}
                                  >
                                    <img
                                      src={imgUrl}
                                      alt={`${folder} preview ${imgIndex + 1}`}
                                      className="lootFolderPreviewThumb"
                                      loading="lazy"
                                    />
                                  </button>
                                ))}
                              </div>
                            ) : (
                              <span className="lootFolderPreviewEmpty" title="No preview available">
                                <PhotoIcon className="lootFolderPreviewEmptyIcon" />
                                <span className="lootFolderPreviewEmptyText">—</span>
                              </span>
                            )}
                          </td>
                        ) : null}
                        <td>{label}</td>
                        <td>{description}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}

          <div className="lootManageFooter">
            <button type="button" className="buttonSubtle" onClick={onClose} style={{ padding: "8px 16px" }}>
              Cancel
            </button>
            <button type="button" className="buttonStrong" onClick={handleSave} disabled={saving} style={{ padding: "8px 16px" }}>
              {saving ? "Saving..." : "Save"}
            </button>
          </div>
        </div>
      </div>

      <ImagePreviewModal
        imageUrl={selectedPreviewImage}
        onClose={() => setSelectedPreviewImage(null)}
      />
    </>
  );
}
