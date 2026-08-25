import { useEffect, useMemo, useState } from "react";
import { check as checkTauriUpdate } from "@tauri-apps/plugin-updater";
import { relaunch } from "@tauri-apps/plugin-process";
import {
  clearGameFolder,
  clearInstalled,
  clearResources,
  downloadProfiles,
  getStatus,
  installProfile,
  openItemFolder,
  openResourcesFolder,
  saveGameFolder
} from "./api";
import type { AppStatus } from "./types";

function App() {
  const [status, setStatus] = useState<AppStatus | null>(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [folderInput, setFolderInput] = useState("");
  const [selectedProfile, setSelectedProfile] = useState("");
  const [updateStatus, setUpdateStatus] = useState<{ available: boolean; version?: string } | null>(null);

  const availableProfiles = status?.availableProfiles ?? [];
  const selectedProfileData = availableProfiles.find((profile) => profile.id === selectedProfile) ?? null;

  const canInstall = useMemo(() => Boolean(selectedProfile), [selectedProfile]);

  async function refresh() {
    setStatus(await getStatus());
  }

  async function runAction(action: () => Promise<unknown>, successMessage?: string) {
    setLoading(true);
    try {
      await action();
      await refresh();
      if (successMessage) {
        setMessage(successMessage);
      }
    } catch (err) {
      setMessage(err instanceof Error ? err.message : "Request failed.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    refresh().catch((err) => {
      setMessage(err instanceof Error ? err.message : "Failed to load app status.");
    });
  }, []);

  useEffect(() => {
    if (availableProfiles.length === 0) {
      if (selectedProfile) {
        setSelectedProfile("");
      }
      return;
    }
    const selectedStillExists = availableProfiles.some((profile) => profile.id === selectedProfile);
    if (!selectedStillExists) {
      setSelectedProfile(availableProfiles[0].id);
    }
  }, [availableProfiles, selectedProfile]);

  async function onSaveFolder() {
    const path = folderInput.trim();
    if (!path) return;
    setLoading(true);
    try {
      const initialResult = await saveGameFolder(path, false);
      await refresh();
      setMessage(
        initialResult.containsExpectedItemFolder
          ? "Game folder saved."
          : "Game folder saved (without 3ddata/item)."
      );
    } catch (err) {
      const text = err instanceof Error ? err.message : "Request failed.";
      if (text.includes("does not contain 3ddata/item")) {
        const confirmed = window.confirm("The folder does not contain 3ddata/item. Save it anyway?");
        if (!confirmed) {
          setMessage("Save cancelled.");
          setLoading(false);
          return;
        }
        await saveGameFolder(path, true);
        await refresh();
        setMessage("Game folder saved.");
      } else {
        setMessage(text);
      }
    } finally {
      setLoading(false);
    }
  }

  async function onInstallProfile() {
    if (!selectedProfile) return;
    if (!status?.selectedGameItemFolder) {
      setMessage("Set game installation folder first (must point to a base containing 3ddata/item).");
      return;
    }
    const confirmed = window.confirm(
      `Install profile "${selectedProfile}"? This will clear current installed models first.`
    );
    if (!confirmed) return;
    await runAction(() => installProfile(selectedProfile), `Installed profile: ${selectedProfile}.`);
  }

  async function onClearInstalled() {
    const confirmed = window.confirm("Clear all installed models from 3ddata/item?");
    if (!confirmed) return;
    await runAction(clearInstalled, "Installed models cleared.");
  }

  async function onCheckUpdates() {
    setLoading(true);
    try {
      const update = await checkTauriUpdate();
      if (update?.available) {
        setUpdateStatus({ available: true, version: update.version });
        setMessage(`Update available: v${update.version}`);
      } else {
        setUpdateStatus({ available: false });
        setMessage("You are on the latest version.");
      }
    } catch (err) {
      setMessage(err instanceof Error ? err.message : "Update check failed.");
    } finally {
      setLoading(false);
    }
  }

  async function onInstallUpdate() {
    setLoading(true);
    try {
      const update = await checkTauriUpdate();
      if (!update?.available) {
        setMessage("No update available.");
        return;
      }
      setMessage("Downloading update...");
      await update.downloadAndInstall();
      setMessage("Update installed. Restarting...");
      await relaunch();
    } catch (err) {
      setMessage(err instanceof Error ? err.message : "Update failed.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="layout">
      <header className="card">
        <h1>RO Toolbox</h1>
        <p>Version: {status?.version ?? "..."}</p>
      </header>

      <section className="card">
        <h2>Settings</h2>
        <p>Game folder: {status?.selectedGameBase ?? "Not set"}</p>
        <p>Item folder: {status?.selectedGameItemFolder ?? "Not set"}</p>
        <div className="row">
          <input
            value={folderInput}
            onChange={(e) => setFolderInput(e.target.value)}
            placeholder="C:\\Games\\Ragnarok"
          />
          <button disabled={loading || folderInput.trim().length === 0} onClick={onSaveFolder}>
            Save folder
          </button>
          <button disabled={loading} onClick={() => runAction(clearGameFolder, "Game folder cleared.")}>
            Clear
          </button>
          <button disabled={loading} onClick={() => runAction(openItemFolder, "Opened item folder.")}>
            Open item folder
          </button>
        </div>
      </section>

      <section className="card">
        <h2>Loot Profiles</h2>
        <p>Downloaded: {status?.downloadedProfiles.length ?? 0}</p>
        <div className="row">
          <button disabled={loading} onClick={() => runAction(downloadProfiles, "Profiles downloaded.")}>
            Download profiles
          </button>
          <button disabled={loading} onClick={() => runAction(openResourcesFolder, "Opened resources folder.")}>
            Open resources folder
          </button>
          <button disabled={loading} onClick={() => runAction(clearResources, "Downloaded resources cleared.")}>
            Clear downloads
          </button>
          <button disabled={loading} onClick={onClearInstalled}>
            Clear installed
          </button>
        </div>

        <div className="row">
          <select
            disabled={loading || availableProfiles.length === 0}
            value={selectedProfile}
            onChange={(e) => setSelectedProfile(e.target.value)}
          >
            {availableProfiles.map((profile) => (
              <option key={profile.id} value={profile.id}>
                {profile.id}
              </option>
            ))}
          </select>
          <button
            disabled={loading || !canInstall}
            onClick={onInstallProfile}
          >
            Install selected
          </button>
          {selectedProfileData?.url ? (
            <a href={selectedProfileData.url} target="_blank" rel="noreferrer" className="linkBtn">
              Visit profile
            </a>
          ) : null}
        </div>

        <p>Selected details:</p>
        <p>
          {selectedProfileData
            ? `${selectedProfileData.name ?? selectedProfileData.id}${
                selectedProfileData.author ? ` • by ${selectedProfileData.author}` : ""
              }${selectedProfileData.createdAt ? ` • ${selectedProfileData.createdAt}` : ""}`
            : "None"}
        </p>
        <p>{selectedProfileData?.description ?? "-"}</p>
        <p>
          Installed profile:{" "}
          {status?.installedProfile?.name
            ? `${status.installedProfile.name}${status.installedProfile.author ? ` by ${status.installedProfile.author}` : ""}`
            : "None"}
        </p>
        {status?.installedProfile?.url ? (
          <p>
            Installed URL:{" "}
            <a href={status.installedProfile.url} target="_blank" rel="noreferrer">
              {status.installedProfile.url}
            </a>
          </p>
        ) : null}
      </section>

      <section className="card">
        <h2>Updater</h2>
        <div className="row">
          <button disabled={loading} onClick={onCheckUpdates}>
            Check updates
          </button>
          <button disabled={loading || !updateStatus?.available} onClick={onInstallUpdate}>
            Install update
          </button>
        </div>
        <p>
          {updateStatus
            ? updateStatus.available
              ? `Update available: v${updateStatus.version}`
              : "Up to date."
            : "No update check yet."}
        </p>
      </section>

      {message ? <section className="card status">{message}</section> : null}
    </main>
  );
}

export default App;
