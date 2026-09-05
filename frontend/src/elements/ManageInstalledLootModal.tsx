import { useEffect, useState } from "react";
import { XMarkIcon } from "@heroicons/react/24/outline";
import type { LootFolderInfo } from "../utils/lootDictionary";
import { getFolderDescription, getFolderLabel, loadLootDictionary } from "../utils/lootDictionary";

interface ManageInstalledLootModalProps {
  isOpen: boolean;
  managedSubfolders: string[];
  disabledManagedSubfolders: string[];
  onClose: () => void;
  onSave: (disabledManagedSubfolders: string[]) => Promise<void> | void;
}

export function ManageInstalledLootModal({
  isOpen,
  managedSubfolders,
  disabledManagedSubfolders,
  onClose,
  onSave
}: ManageInstalledLootModalProps) {
  const [dictionary, setDictionary] = useState<Record<string, LootFolderInfo>>({});
  const [pendingDisabled, setPendingDisabled] = useState<Set<string>>(new Set());
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    void loadLootDictionary().then(setDictionary);
  }, []);

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
                  <th>Folder</th>
                  <th>Label</th>
                  <th>Description</th>
                </tr>
              </thead>
              <tbody>
                {managedSubfolders.map((folder) => {
                  const isDisabled = isFolderDisabled(folder);
                  const label = getFolderLabel(folder, dictionary);
                  const description = getFolderDescription(folder, dictionary) || "—";

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
  );
}
