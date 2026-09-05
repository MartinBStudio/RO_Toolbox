import { useEffect, useState } from "react";
import type { LootFolderInfo } from "../utils/lootDictionary";
import { loadLootDictionary, getFolderLabel, getFolderDescription } from "../utils/lootDictionary";

interface IncludedFoldersTableProps {
  folders: string[];
}

export function IncludedFoldersTable({ folders }: IncludedFoldersTableProps) {
  const [dictionary, setDictionary] = useState<Record<string, LootFolderInfo>>({});
  const [loading, setLoading] = useState(true);

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
    <div style={{ marginTop: 12, marginBottom: 12 }}>
      <p style={{ margin: "0 0 8px 0", color: "rgba(255,255,255,0.75)", fontSize: 14, fontWeight: 500 }}>
        Included packages ({folders.length}):
      </p>
      <div className="lootManageTableWrap" style={{ opacity: loading ? 0.6 : 1 }}>
        <table className="lootManageTable">
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
  );
}
