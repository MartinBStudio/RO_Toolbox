import { useEffect, useState } from "react";
import { ChevronDownIcon, ChevronUpIcon, PhotoIcon } from "@heroicons/react/24/outline";
import type { LootFolderInfo } from "../utils/lootDictionary";
import {
  loadLootDictionary,
  loadItemPreviews,
  getFolderLabel,
  getFolderDescription,
  getFolderPreviews
} from "../utils/lootDictionary";
import { ImagePreviewModal } from "./ImagePreviewModal";

interface IncludedFoldersTableProps {
  folders: string[];
  profileName?: string | null;
  showPreviews?: boolean;
}

export function IncludedFoldersTable({ folders, profileName, showPreviews }: IncludedFoldersTableProps) {
  const [dictionary, setDictionary] = useState<Record<string, LootFolderInfo>>({});
  const [previews, setPreviews] = useState<Record<string, string[]>>({});
  const [loading, setLoading] = useState(true);
  const [collapsed, setCollapsed] = useState(true);
  const [selectedPreviewImage, setSelectedPreviewImage] = useState<string | null>(null);

  const displayPreviews = showPreviews ?? Boolean(profileName?.toLowerCase().includes("farming meta"));

  useEffect(() => {
    setLoading(true);
    const promises: [Promise<Record<string, LootFolderInfo>>, Promise<Record<string, string[]>> | Promise<Record<string, string[]>>] = [
      loadLootDictionary(),
      displayPreviews ? loadItemPreviews() : Promise.resolve({})
    ];
    Promise.all(promises)
      .then(([dict, prevs]) => {
        setDictionary(dict);
        setPreviews(prevs);
      })
      .finally(() => {
        setLoading(false);
      });
  }, [displayPreviews]);

  if (folders.length === 0) {
    return null;
  }

  return (
    <>
      <div className="includedPackagesAccordion accordionItem">
        <button
          type="button"
          className="accordionHeader includedPackagesHeader"
          onClick={() => setCollapsed((value) => !value)}
          aria-expanded={!collapsed}
          aria-label={collapsed ? "Expand included packages" : "Collapse included packages"}
        >
          <span className="accordionLabel">Included packages ({folders.length})</span>
          <span className="accordionIcon" aria-hidden="true">
            {collapsed ? <ChevronDownIcon /> : <ChevronUpIcon />}
          </span>
        </button>
        {!collapsed ? (
          <div className="accordionContent includedPackagesContent">
            <div className="lootManageTableWrap includedPackagesTableWrap" style={{ opacity: loading ? 0.6 : 1 }}>
              <table className="lootManageTable includedPackagesTable">
                <thead>
                  <tr>
                    <th style={{ width: displayPreviews ? "12%" : "15%" }}>Folder</th>
                    {displayPreviews ? (
                      <th style={{ width: "160px", minWidth: "120px" }}>Preview</th>
                    ) : null}
                    <th style={{ width: displayPreviews ? "18%" : "25%" }}>Label</th>
                    <th>Description</th>
                  </tr>
                </thead>
                <tbody>
                  {folders.map((folder) => {
                    const label = getFolderLabel(folder, dictionary);
                    const description = getFolderDescription(folder, dictionary);
                    const folderPreviews = displayPreviews ? getFolderPreviews(folder, previews) : [];

                    return (
                      <tr key={folder}>
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
                        <td>{description || "—"}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        ) : null}
      </div>

      <ImagePreviewModal
        imageUrl={selectedPreviewImage}
        onClose={() => setSelectedPreviewImage(null)}
      />
    </>
  );
}
