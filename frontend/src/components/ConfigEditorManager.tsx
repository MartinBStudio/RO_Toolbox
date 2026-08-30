import { useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { ArrowPathIcon, FolderOpenIcon } from "@heroicons/react/24/outline";
import {
  addIgnoreListEntry,
  deleteIgnoreListEntry,
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

type TomlBooleanEntry = {
  id: string;
  lineIndex: number;
  context: string;
  key: string;
  value: boolean;
};

const FILE_ORDER: Array<ConfigEditorFileState["id"]> = ["ignore", "rose"];

export function ConfigEditorManager({ loading, onBusyChange, onMessage }: ConfigEditorManagerProps) {
  const [status, setStatus] = useState<ConfigEditorStatus | null>(null);
  const [selectedFileId, setSelectedFileId] = useState<ConfigEditorFileState["id"]>("ignore");
  const [editorContent, setEditorContent] = useState("");
  const [editorDirty, setEditorDirty] = useState(false);
  const [ignoreNames, setIgnoreNames] = useState<string[]>([]);
  const [newIgnoreName, setNewIgnoreName] = useState("");

  const selectedFile = useMemo(
    () => status?.files.find((file) => file.id === selectedFileId) ?? null,
    [selectedFileId, status]
  );

  const roseBooleanEntries = useMemo(() => {
    if (selectedFile?.id !== "rose") {
      return [];
    }
    return extractBooleanEntriesFromTomlSource(editorContent);
  }, [selectedFile?.id, editorContent]);

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

  useEffect(() => {
    const ignoreFile = status?.files.find((file) => file.id === "ignore");
    setIgnoreNames(extractIgnoreNames(ignoreFile));
  }, [status]);

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

  async function onAddIgnoreName() {
    const nextName = newIgnoreName.trim();
    if (!nextName) {
      onMessage("Ignore name is required.");
      return;
    }
    if (selectedFileId === "ignore" && editorDirty) {
      const confirmed = window.confirm("Discard unsaved changes in ignore.toml before adding an entry?");
      if (!confirmed) {
        return;
      }
    }

    onBusyChange(true);
    try {
      const result = await addIgnoreListEntry(nextName);
      await loadStatus(false);
      setIgnoreNames(result.names);
      setNewIgnoreName("");
      onMessage(`Added "${nextName}" to ignore list.`);
    } catch (err) {
      onMessage(toErrorMessage(err, "Failed to add ignore entry."));
    } finally {
      onBusyChange(false);
    }
  }

  async function onDeleteIgnoreName(name: string) {
    if (selectedFileId === "ignore" && editorDirty) {
      const confirmed = window.confirm("Discard unsaved changes in ignore.toml before deleting an entry?");
      if (!confirmed) {
        return;
      }
    }

    onBusyChange(true);
    try {
      const result = await deleteIgnoreListEntry(name);
      await loadStatus(false);
      setIgnoreNames(result.names);
      onMessage(`Removed "${name}" from ignore list.`);
    } catch (err) {
      onMessage(toErrorMessage(err, "Failed to remove ignore entry."));
    } finally {
      onBusyChange(false);
    }
  }

  function onToggleRoseBoolean(entry: TomlBooleanEntry, nextValue: boolean) {
    const lines = editorContent.split(/\r?\n/);
    const originalLine = lines[entry.lineIndex];
    if (originalLine === undefined) {
      onMessage("Could not update value: line not found.");
      return;
    }

    const match = originalLine.match(/^(\s*[A-Za-z0-9_-]+\s*=\s*)(true|false)(\s*(?:#.*)?)$/);
    if (!match) {
      onMessage(`Could not update value for ${entry.context}.${entry.key}.`);
      return;
    }

    lines[entry.lineIndex] = `${match[1]}${nextValue ? "true" : "false"}${match[3] ?? ""}`;
    const hasTrailingNewLine = editorContent.endsWith("\n");
    const nextContent = lines.join("\n") + (hasTrailingNewLine ? "\n" : "");
    setEditorContent(nextContent);
    setEditorDirty(true);
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
          <p className="configEditorWarning">Warning: edit these files only when the ROSE client is closed.</p>
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
            {selectedFile.id === "ignore" && (
              <div className="configEditorIgnorePanel">
                <p className="settingsSectionLabel">Ignore list manager</p>
                <div className="configEditorIgnoreAddRow">
                  <input
                    type="text"
                    value={newIgnoreName}
                    onChange={(event) => setNewIgnoreName(event.target.value)}
                    placeholder="Entry name"
                    disabled={loading}
                  />
                  <button type="button" className="buttonSubtle" disabled={loading} onClick={onAddIgnoreName}>
                    Add
                  </button>
                </div>
                {ignoreNames.length > 0 ? (
                  <ul className="configEditorIgnoreList">
                    {ignoreNames.map((name, index) => (
                      <li key={`${name}-${index}`} className="configEditorIgnoreItem">
                        <span className="configEditorIgnoreName">{name}</span>
                        <button type="button" className="buttonSubtle" disabled={loading} onClick={() => onDeleteIgnoreName(name)}>
                          Delete
                        </button>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p className="profileCardEmpty">No ignore entries yet.</p>
                )}
              </div>
            )}
            {selectedFile.id === "rose" && (
              <div className="configEditorBooleanPanel">
                <p className="settingsSectionLabel">Boolean values manager</p>
                {roseBooleanEntries.length > 0 ? (
                  <ul className="configEditorBooleanList">
                    {roseBooleanEntries.map((entry) => (
                      <li key={entry.id} className="configEditorBooleanItem">
                        <div className="configEditorBooleanMeta">
                          <span className="configEditorBooleanPath">{entry.context}</span>
                          <span className="configEditorBooleanKey">{entry.key}</span>
                        </div>
                        <div className="configEditorBooleanActions">
                          <button
                            type="button"
                            className={`buttonSubtle${entry.value ? " configEditorBooleanActive" : ""}`}
                            disabled={loading}
                            onClick={() => onToggleRoseBoolean(entry, true)}
                          >
                            true
                          </button>
                          <button
                            type="button"
                            className={`buttonSubtle${!entry.value ? " configEditorBooleanActive" : ""}`}
                            disabled={loading}
                            onClick={() => onToggleRoseBoolean(entry, false)}
                          >
                            false
                          </button>
                        </div>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p className="profileCardEmpty">No boolean entries found.</p>
                )}
              </div>
            )}
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

function extractIgnoreNames(file: ConfigEditorFileState | null | undefined): string[] {
  const ignoreNode = file?.parsed?.ignore;
  if (!Array.isArray(ignoreNode)) {
    return [];
  }

  const names: string[] = [];
  for (const item of ignoreNode) {
    if (!item || typeof item !== "object" || Array.isArray(item)) {
      continue;
    }
    const entryName = (item as { [key: string]: TomlNode }).name;
    if (typeof entryName === "string" && entryName.trim()) {
      names.push(entryName.trim());
    }
  }
  return names;
}

function extractBooleanEntriesFromTomlSource(content: string): TomlBooleanEntry[] {
  const lines = content.split(/\r?\n/);
  const entries: TomlBooleanEntry[] = [];
  let currentContext = "root";
  const arrayTableCounter = new Map<string, number>();

  for (let index = 0; index < lines.length; index++) {
    const line = lines[index];
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) {
      continue;
    }

    const arrayTableMatch = trimmed.match(/^\[\[([^\]]+)]]$/);
    if (arrayTableMatch) {
      const basePath = arrayTableMatch[1].trim();
      const nextIndex = (arrayTableCounter.get(basePath) ?? 0) + 1;
      arrayTableCounter.set(basePath, nextIndex);
      currentContext = `${basePath}[${nextIndex}]`;
      continue;
    }

    const tableMatch = trimmed.match(/^\[([^\]]+)]$/);
    if (tableMatch) {
      currentContext = tableMatch[1].trim();
      continue;
    }

    const valueMatch = line.match(/^\s*([A-Za-z0-9_-]+)\s*=\s*(true|false)\s*(?:#.*)?$/);
    if (!valueMatch) {
      continue;
    }

    const key = valueMatch[1];
    const value = valueMatch[2] === "true";
    entries.push({
      id: `${currentContext}.${key}.${index}`,
      lineIndex: index,
      context: currentContext,
      key,
      value
    });
  }

  return entries;
}
