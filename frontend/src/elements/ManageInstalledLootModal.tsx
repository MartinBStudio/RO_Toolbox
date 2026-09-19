import { useEffect, useState } from "react";
import { PhotoIcon, XMarkIcon } from "@heroicons/react/24/outline";
import {
  getLootModelScales,
  scaleLootModelFolder,
  type LootModelScaleDirection,
  type LootModelScaleFile,
  type LootModelScaleReport
} from "../backendConnector/api.ts";
import type { LootFolderInfo } from "../utils/lootDictionary";
import {
  getFolderDescription,
  getFolderLabel,
  getFolderPreviews,
  loadItemPreviews,
  loadLootDictionary
} from "../utils/lootDictionary";
import { ImagePreviewModal } from "./ImagePreviewModal";

type ModelScaleDisplay = {
  label: string;
  detail: string;
  state: "tiny" | "small" | "normal" | "large" | "huge";
};

function isReasonableSizeValue(value: number) {
  return Number.isFinite(value)
    && value >= 0
    && value < 10000;
}

function hasReasonableBounds(file: LootModelScaleFile) {
  const bounds = file.vertexBounds;
  const size = bounds?.size;
  return Boolean(
    size
      && isReasonableSizeValue(size.x)
      && isReasonableSizeValue(size.y)
      && isReasonableSizeValue(size.z)
      && isReasonableSizeValue(bounds.largestAxis)
  );
}

function buildScaleMap(report: LootModelScaleReport) {
  const scaleMap: Record<string, ModelScaleDisplay> = {};
  for (const folder of report.folders) {
    const currentFile = findLargestFile(folder.files);
    if (currentFile) {
      const originalFile = findLargestFile(folder.originalFiles);
      const scale = formatScaleDisplay(currentFile, originalFile);
      if (scale) {
        scaleMap[folder.folder.trim().toLowerCase()] = scale;
      }
    }
  }
  return scaleMap;
}

function findLargestFile(files: LootModelScaleFile[]) {
  return files
    .filter((file) => !file.error && hasReasonableBounds(file))
    .sort((left, right) => (right.vertexBounds?.largestAxis ?? 0) - (left.vertexBounds?.largestAxis ?? 0))[0] ?? null;
}

function formatScaleDisplay(currentFile: LootModelScaleFile, originalFile: LootModelScaleFile | null): ModelScaleDisplay | null {
  const current = currentFile.vertexBounds?.largestAxis;
  const original = originalFile?.vertexBounds?.largestAxis;
  if (!current || !original || !isReasonableSizeValue(current) || !isReasonableSizeValue(original)) {
    return null;
  }
  const ratio = current / original;
  const percent = (ratio - 1) * 100;
  if (!Number.isFinite(ratio) || !Number.isFinite(percent)) {
    return null;
  }
  const label = `${formatScaleRatio(ratio)}x`;
  const rounded = Math.round(percent);
  const percentLabel = rounded === 0 ? "original size" : `${rounded > 0 ? "+" : ""}${rounded}%`;
  return {
    label,
    detail: `${label} (${percentLabel})`,
    state: getScaleState(ratio)
  };
}

function formatScaleRatio(ratio: number) {
  if (ratio < 0.1) {
    return ratio.toFixed(2);
  }
  if (ratio < 10) {
    return Number(ratio.toFixed(2)).toString();
  }
  return Math.round(ratio).toString();
}

function getScaleState(ratio: number): ModelScaleDisplay["state"] {
  if (ratio < 0.1) return "tiny";
  if (ratio < 0.85) return "small";
  if (ratio <= 1.15) return "normal";
  if (ratio <= 3) return "large";
  return "huge";
}

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
  const [modelScales, setModelScales] = useState<Record<string, ModelScaleDisplay>>({});
  const [pendingDisabled, setPendingDisabled] = useState<Set<string>>(new Set());
  const [saving, setSaving] = useState(false);
  const [scalingFolder, setScalingFolder] = useState<string | null>(null);
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

  const loadModelScales = async (cancelled?: () => boolean) => {
    try {
      const report = await getLootModelScales();
      if (!cancelled?.()) {
        setModelScales(buildScaleMap(report));
      }
    } catch (_err) {
      if (!cancelled?.()) {
        setModelScales({});
      }
    }
  };

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    let cancelled = false;
    void loadModelScales(() => cancelled);

    return () => {
      cancelled = true;
    };
  }, [isOpen]);

  if (!isOpen) return null;

  const matchesDisabledFolder = (folder: string, disabledFolder: string) =>
    folder.trim().toLowerCase() === disabledFolder.trim().toLowerCase();

  const isFolderDisabled = (folder: string) =>
    Array.from(pendingDisabled).some((disabledFolder) => matchesDisabledFolder(folder, disabledFolder));

  const getFolderScale = (folder: string) => modelScales[folder.trim().toLowerCase()] ?? null;

  const sortedManagedSubfolders = managedSubfolders
    .map((folder, index) => ({ folder, index }))
    .sort((left, right) => {
      const disabledDiff = Number(isFolderDisabled(left.folder)) - Number(isFolderDisabled(right.folder));
      return disabledDiff || left.index - right.index;
    })
    .map((entry) => entry.folder);

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

  const handleScaleFolder = async (folder: string, direction: LootModelScaleDirection) => {
    setScalingFolder(folder);
    try {
      await scaleLootModelFolder(folder, direction);
      await loadModelScales();
    } finally {
      setScalingFolder(null);
    }
  };

  return (
    <>
      <div className="lootManageModalOverlay" onClick={onClose}>
        <div className="lootManageModal" onClick={(event) => event.stopPropagation()}>
          <div className="lootManageHeader">
            <div>
              <h2 className="lootManageTitle">Manage installed package</h2>
              <p className="lootManageSubtitle">
                Choose what parts of the package you want to enable or resize.
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
                      <th className="lootManagePreviewHeader">Preview</th>
                    ) : null}
                    <th
                      className="lootManageSizeHeader"
                      title="Size difference compared to the original model from the downloaded package."
                    >
                      Size
                    </th>
                    <th style={{ width: displayPreviews ? "18%" : "25%" }}>Label</th>
                    <th>Description</th>
                  </tr>
                </thead>
                <tbody>
                  {sortedManagedSubfolders.map((folder) => {
                    const isDisabled = isFolderDisabled(folder);
                    const label = getFolderLabel(folder, dictionary);
                    const description = getFolderDescription(folder, dictionary) || "—";
                    const folderPreviews = displayPreviews ? getFolderPreviews(folder, previews) : [];
                    const firstFolderPreview = folderPreviews[0];
                    const folderScale = getFolderScale(folder);

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
                            {firstFolderPreview ? (
                              <div className="lootFolderPreviewThumbnails">
                                <button
                                  type="button"
                                  className="lootFolderPreviewThumbBtn"
                                  onClick={() => setSelectedPreviewImage(firstFolderPreview)}
                                  title={`Click to view ${folder} preview`}
                                  aria-label={`View ${folder} preview`}
                                >
                                  <img
                                    src={firstFolderPreview}
                                    alt={`${folder} preview`}
                                    className="lootFolderPreviewThumb"
                                    loading="lazy"
                                  />
                                </button>
                              </div>
                            ) : (
                              <span className="lootFolderPreviewEmpty" title="No preview available">
                                <PhotoIcon className="lootFolderPreviewEmptyIcon" />
                                <span className="lootFolderPreviewEmptyText">—</span>
                              </span>
                            )}
                          </td>
                        ) : null}
                        <td className="lootManageSize">
                          <span
                            className={`lootManageSizeDelta${folderScale ? ` is-${folderScale.state}` : ""}`}
                            title={folderScale?.detail ?? "Size could not be measured"}
                          >
                            {folderScale?.label ?? "-"}
                          </span>
                          <span className="lootManageSizeActions">
                            <button
                              type="button"
                              className="lootManageSizeButton lootManageSizeLimitButton"
                              onClick={() => handleScaleFolder(folder, "min")}
                              disabled={scalingFolder === folder}
                              title="Set model size to the minimum, about -96%"
                              aria-label={`Set ${folder} model size to minimum`}
                            >
                              Min
                            </button>
                            <button
                              type="button"
                              className="lootManageSizeButton"
                              onClick={() => handleScaleFolder(folder, "decrease")}
                              disabled={scalingFolder === folder}
                              title="Decrease model size by 10%"
                              aria-label={`Decrease ${folder} model size by 10%`}
                            >
                              -
                            </button>
                            <button
                              type="button"
                              className="lootManageSizeButton lootManageSizeResetButton"
                              onClick={() => handleScaleFolder(folder, "reset")}
                              disabled={scalingFolder === folder}
                              title="Reset model size to the original package value"
                              aria-label={`Reset ${folder} model size to the original package value`}
                            >
                              R
                            </button>
                            <button
                              type="button"
                              className="lootManageSizeButton"
                              onClick={() => handleScaleFolder(folder, "increase")}
                              disabled={scalingFolder === folder}
                              title="Increase model size by 10%"
                              aria-label={`Increase ${folder} model size by 10%`}
                            >
                              +
                            </button>
                            <button
                              type="button"
                              className="lootManageSizeButton lootManageSizeLimitButton"
                              onClick={() => handleScaleFolder(folder, "max")}
                              disabled={scalingFolder === folder}
                              title="Set model size to +1000%"
                              aria-label={`Set ${folder} model size to maximum`}
                            >
                              Max
                            </button>
                          </span>
                        </td>
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
            <button type="button" className="buttonSubtle lootManageFooterButton" onClick={onClose}>
              Cancel
            </button>
            <button type="button" className="buttonStrong lootManageFooterButton" onClick={handleSave} disabled={saving}>
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
