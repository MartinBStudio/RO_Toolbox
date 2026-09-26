import { useEffect, useState } from "react";
import { ChevronDownIcon, ChevronUpIcon } from "@heroicons/react/24/outline";
import type { LootFolderInfo } from "../utils/lootDictionary";
import {
  loadLootDictionary,
  loadItemPreviews,
  getFolderLabel,
  getFolderDescription,
  getFolderPreviews
} from "../utils/lootDictionary";
import { ImagePreviewModal } from "./ImagePreviewModal";
import { LootModelPreviewCanvas } from "./LootModelPreviewCanvas";
import { LootModelPreviewModal } from "./LootModelPreviewModal";

interface IncludedFoldersTableProps {
  folders: string[];
  profileId?: string | null;
  showPreviews?: boolean;
}

export function IncludedFoldersTable({ folders, profileId, showPreviews }: IncludedFoldersTableProps) {
  const [dictionary, setDictionary] = useState<Record<string, LootFolderInfo>>({});
  const [previews, setPreviews] = useState<Record<string, string[]>>({});
  const [loading, setLoading] = useState(true);
  const [collapsed, setCollapsed] = useState(true);
  const [selectedPreviewImage, setSelectedPreviewImage] = useState<string | null>(null);
  const [selectedModelPreviewFolder, setSelectedModelPreviewFolder] = useState<string | null>(null);

  const displayPreviews = showPreviews ?? true;

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
                <colgroup>
                  <col className="lootManageFolderCol" />
                  {displayPreviews ? (
                    <>
                      <col className="lootManageRenderCol" />
                      <col className="lootManageVisualCol" />
                    </>
                  ) : null}
                  <col className="lootManageLabelCol" />
                  <col />
                </colgroup>
                <thead>
                  <tr>
                    <th>Folder</th>
                    {displayPreviews ? (
                      <>
                        <th className="lootManageRenderHeader">3D render</th>
                        <th className="lootManageVisualHeader">Default visual</th>
                      </>
                    ) : null}
                    <th>Label</th>
                    <th>Description</th>
                  </tr>
                </thead>
                <tbody>
                  {folders.map((folder) => {
                    const label = getFolderLabel(folder, dictionary);
                    const description = getFolderDescription(folder, dictionary);
                    const folderPreviews = displayPreviews ? getFolderPreviews(folder, previews) : [];
                    const firstFolderPreview = folderPreviews[0];

                    return (
                      <tr key={folder}>
                        <td className="lootManageFolder">{folder}</td>
                        {displayPreviews ? (
                          <>
                            <td className="lootManagePreviewCell lootManageRenderCell">
                              <LootModelPreviewCanvas
                                folder={folder}
                                profileId={profileId}
                                onOpen={() => setSelectedModelPreviewFolder(folder)}
                              />
                            </td>
                            <td className="lootManagePreviewCell lootManageVisualCell">
                              {firstFolderPreview ? (
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
                              ) : null}
                            </td>
                          </>
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
      <LootModelPreviewModal
        folder={selectedModelPreviewFolder}
        profileId={profileId}
        onClose={() => setSelectedModelPreviewFolder(null)}
      />
    </>
  );
}
