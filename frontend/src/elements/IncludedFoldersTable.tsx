import { useEffect, useState } from "react";
import { ChevronDownIcon, ChevronUpIcon } from "@heroicons/react/24/outline";
import type { LootFolderInfo } from "../utils/lootDictionary";
import { loadLootDictionary, getFolderLabel, getFolderDescription } from "../utils/lootDictionary";

interface IncludedFoldersTableProps {
  folders: string[];
}

export function IncludedFoldersTable({ folders }: IncludedFoldersTableProps) {
  const [dictionary, setDictionary] = useState<Record<string, LootFolderInfo>>({});
  const [loading, setLoading] = useState(true);
  const [collapsed, setCollapsed] = useState(true);

  useEffect(() => {
    setLoading(true);
    void loadLootDictionary().then((dict) => {
      setDictionary(dict);
      setLoading(false);
    });
  }, []);

  if (folders.length === 0) {
    return null;
  }

  return (
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
                  <th style={{ width: "15%" }}>Folder</th>
                  <th style={{ width: "25%" }}>Label</th>
                  <th>Description</th>
                </tr>
              </thead>
              <tbody>
                {folders.map((folder) => {
                  const label = getFolderLabel(folder, dictionary);
                  const description = getFolderDescription(folder, dictionary);

                  return (
                    <tr key={folder}>
                      <td className="lootManageFolder">{folder}</td>
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
  );
}
