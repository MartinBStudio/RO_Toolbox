import { useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { ArrowPathIcon, FolderOpenIcon } from "@heroicons/react/24/outline";
import {
  getConfigEditorStatus,
  openConfigEditorFolder,
  saveConfigEditorFile
} from "../backendConnector/api.ts";
import type { ConfigEditorFileState, ConfigEditorStatus, TomlNode } from "../types.ts";

type ConfigEditorManagerProps = {
  loading: boolean;
  onBusyChange: (busy: boolean) => void;
  onMessage: (message: string) => void;
};

const FILE_ORDER: Array<ConfigEditorFileState["id"]> = ["ignore", "rose"];

export function ConfigEditorManager({ loading, onBusyChange, onMessage }: ConfigEditorManagerProps) {
  const [status, setStatus] = useState<ConfigEditorStatus | null>(null);
  const [selectedFileId, setSelectedFileId] = useState<ConfigEditorFileState["id"]>("ignore");
  const [editorContent, setEditorContent] = useState("");
  const [editorDirty, setEditorDirty] = useState(false);

  const selectedFile = useMemo(
    () => status?.files.find((file) => file.id === selectedFileId) ?? null,
    [selectedFileId, status]
  );

  useEffect(() => {
    loadStatus(false).catch((err) => {
      onMessage(toErrorMessage(err, "Failed to load config editor files."));
    });
  }, []);

  useEffect(() => {
    if (!selectedFile) {
      setEditorContent("");
      setEditorDirty(false);
      return;
    }
    setEditorContent(selectedFile.content ?? "");
    setEditorDirty(false);
  }, [selectedFileId, selectedFile?.content, selectedFile?.exists]);

  function toErrorMessage(err: unknown, fallback: string) {
    if (err instanceof Error && err.message) {
      return err.message;
    }
    if (typeof err === "string" && err.trim()) {
      return err;
    }
    if (err && typeof err === "object" && "message" in err && typeof err.message === "string") {
      return err.message;
    }
    return fallback;
  }

  async function loadStatus(showBusy: boolean) {
    if (showBusy) {
      onBusyChange(true);
    }
    try {
      const nextStatus = await getConfigEditorStatus();
      setStatus(nextStatus);
      if (!nextStatus.files.some((file) => file.id === selectedFileId)) {
        const fallbackFile = nextStatus.files.find((file) => FILE_ORDER.includes(file.id))?.id ?? "ignore";
        setSelectedFileId(fallbackFile);
      }
    } finally {
      if (showBusy) {
        onBusyChange(false);
      }
    }
  }

  async function onReload() {
    if (editorDirty) {
      const confirmed = window.confirm("Discard unsaved changes and reload from disk?");
      if (!confirmed) {
        return;
      }
    }
    try {
      await loadStatus(true);
      onMessage("Config files reloaded.");
    } catch (err) {
      onMessage(toErrorMessage(err, "Failed to reload config files."));
    }
  }

  async function onSave() {
    if (!selectedFile) {
      onMessage("No file selected.");
      return;
    }

    onBusyChange(true);
    try {
      await saveConfigEditorFile(selectedFile.id, editorContent);
      await loadStatus(false);
      setEditorDirty(false);
      onMessage(`Saved ${selectedFile.fileName}.`);
    } catch (err) {
      onMessage(toErrorMessage(err, "Failed to save file."));
    } finally {
      onBusyChange(false);
    }
  }

  async function onOpenFolder() {
    onBusyChange(true);
    try {
      await openConfigEditorFolder();
      onMessage("Opened ROSE config folder.");
    } catch (err) {
      onMessage(toErrorMessage(err, "Failed to open config folder."));
    } finally {
      onBusyChange(false);
    }
  }

  function onFileChange(fileId: ConfigEditorFileState["id"]) {
    if (fileId === selectedFileId) {
      return;
    }
    if (editorDirty) {
      const confirmed = window.confirm("Discard unsaved changes for current file?");
      if (!confirmed) {
        return;
      }
    }
    setSelectedFileId(fileId);
  }

  const hasParseError = Boolean(selectedFile?.parseError);

  return (
    <section className="card configEditor">
      <div className="configEditorHeader">
        <div>
          <h2>Config editor</h2>
          <p className="configEditorPath">{status?.configDir ?? "%APPDATA%\\Rednim Games\\ROSE Online\\config"}</p>
        </div>
        <div className="headerActions">
          <button
            type="button"
            className="iconBtn iconBtnSubtle iconBtnDim"
            disabled={loading}
            onClick={onOpenFolder}
            title="Open config folder"
            aria-label="Open config folder"
          >
            <FolderOpenIcon className="heroIcon" />
          </button>
          <button
            type="button"
            className="iconBtn iconBtnSubtle iconBtnDim"
            disabled={loading}
            onClick={onReload}
            title="Reload files"
            aria-label="Reload files"
          >
            <ArrowPathIcon className="heroIcon" />
          </button>
          <button type="button" className="buttonStrong" disabled={loading || !selectedFile || !editorDirty} onClick={onSave}>
            Save
          </button>
        </div>
      </div>

      <div className="configEditorFileTabs">
        {FILE_ORDER.map((fileId) => {
          const file = status?.files.find((entry) => entry.id === fileId);
          return (
            <button
              key={fileId}
              type="button"
              className={`configEditorFileTab${selectedFileId === fileId ? " configEditorFileTabActive" : ""}`}
              onClick={() => onFileChange(fileId)}
              disabled={loading}
            >
              {file?.fileName ?? `${fileId}.toml`}
              <span className={`configEditorFileBadge${file?.exists ? " configEditorFileBadgeFound" : ""}`}>
                {file?.exists ? "found" : "missing"}
              </span>
            </button>
          );
        })}
      </div>

      {selectedFile ? (
        <div className="configEditorBody">
          <div className="configEditorPane">
            <p className="settingsSectionLabel">TOML source</p>
            <textarea
              className="configEditorTextarea"
              value={editorContent}
              onChange={(event) => {
                setEditorContent(event.target.value);
                setEditorDirty(true);
              }}
              placeholder="File is missing. Add TOML content and save to create it."
              spellCheck={false}
            />
            <p className="configEditorMeta">{selectedFile.filePath}</p>
          </div>
          <div className="configEditorPane">
            <p className="settingsSectionLabel">Parsed preview</p>
            {hasParseError ? (
              <p className="configEditorError">{selectedFile.parseError}</p>
            ) : selectedFile.parsed ? (
              <TomlPreview value={selectedFile.parsed} />
            ) : (
              <p className="profileCardEmpty">No data to preview.</p>
            )}
          </div>
        </div>
      ) : (
        <p className="profileCardEmpty">No config file metadata loaded.</p>
      )}
    </section>
  );
}

function TomlPreview({ value }: { value: { [key: string]: TomlNode } }) {
  return <div className="configTree">{renderTomlNode(value)}</div>;
}

function renderTomlNode(value: TomlNode): ReactNode {
  if (value === null) {
    return <span className="configTreeValue">null</span>;
  }
  if (typeof value === "string") {
    return <span className="configTreeValue">"{value}"</span>;
  }
  if (typeof value === "number" || typeof value === "boolean") {
    return <span className="configTreeValue">{String(value)}</span>;
  }
  if (Array.isArray(value)) {
    if (value.length === 0) {
      return <span className="configTreeValue">[]</span>;
    }
    return (
      <ul className="configTreeList">
        {value.map((item, index) => (
          <li key={index}>{renderTomlNode(item)}</li>
        ))}
      </ul>
    );
  }

  const entries = Object.entries(value);
  if (entries.length === 0) {
    return <span className="configTreeValue">{`{}`}</span>;
  }

  return (
    <ul className="configTreeList">
      {entries.map(([key, entryValue]) => (
        <li key={key}>
          <span className="configTreeKey">{key}</span>: {renderTomlNode(entryValue)}
        </li>
      ))}
    </ul>
  );
}
