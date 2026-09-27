import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import {
  PencilIcon,
  PlusIcon,
  TrashIcon
} from "@heroicons/react/24/outline";
import { StarIcon } from "@heroicons/react/24/solid";
import {
  backupLoginAccountsToOneDrive,
  createLoginAccount,
  deleteLoginAccount,
  getLoginOneDriveBackupStatus,
  listLoginAccounts,
  openLoginOneDriveBackupFolder,
  restoreLoginAccountsFromOneDrive,
  updateLoginAccount,
  type LoginOneDriveBackupStatus,
  type LoginAccount
} from "../backendConnector/loginApi.ts";
import { ConfirmationModal } from "../elements/ConfirmationModal.tsx";
import { BackupProviderPanel } from "./BackupProviderPanel.tsx";

const ACCOUNT_ICON_OPTIONS = [
  "👤",
  "🪙",
  "🛡️",
  "🏹",
  "⚔️",
  "💎",
] as const;

const EMPTY_FORM = {
  name: "",
  email: "",
  password: "",
  displayInQuick: true,
  icon: "👤"
};

export function LoginManager({
  onAccountsChanged,
  onMessage
}: {
  onAccountsChanged?: () => void | Promise<void>;
  onMessage?: (message: string) => void;
}) {
  const [accounts, setAccounts] = useState<LoginAccount[]>([]);
  const [form, setForm] = useState(EMPTY_FORM);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<LoginAccount | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [oneDriveBackup, setOneDriveBackup] = useState<LoginOneDriveBackupStatus | null>(null);
  const [selectedBackupName, setSelectedBackupName] = useState("");

  useEffect(() => {
    void loadAccounts();
    void loadOneDriveBackupStatus();
  }, []);

  useEffect(() => {
    const reloadBackupStatus = () => {
      void loadOneDriveBackupStatus();
    };
    window.addEventListener("roToolbox:backup-provider-changed", reloadBackupStatus);
    return () => window.removeEventListener("roToolbox:backup-provider-changed", reloadBackupStatus);
  }, []);

  useEffect(() => {
    const backups = oneDriveBackup?.backups ?? [];
    if (backups.length === 0) {
      setSelectedBackupName("");
      return;
    }
    if (!backups.some((backup) => backup.name === selectedBackupName)) {
      setSelectedBackupName(backups[0].name);
    }
  }, [oneDriveBackup?.backups, selectedBackupName]);

  async function loadAccounts() {
    try {
      const data = await listLoginAccounts();
      setAccounts(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load accounts.");
    }
  }

  async function loadOneDriveBackupStatus() {
    try {
      const status = await getLoginOneDriveBackupStatus();
      setOneDriveBackup(status);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load OneDrive backup status.");
    }
  }

  function resetForm() {
    setForm(EMPTY_FORM);
    setEditingId(null);
    setFormOpen(false);
  }

  function openCreateForm() {
    resetForm();
    setFormOpen(true);
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);

    try {
      if (editingId) {
        await updateLoginAccount(editingId, form);
      } else {
        await createLoginAccount(form);
      }
      await loadAccounts();
      await onAccountsChanged?.();
      resetForm();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to save account.");
    } finally {
      setBusy(false);
    }
  }

  async function onDeleteConfirmed() {
    if (!deleteTarget) {
      return;
    }

    setBusy(true);
    setError(null);
    try {
      await deleteLoginAccount(deleteTarget.id);
      await loadAccounts();
      await onAccountsChanged?.();
      if (editingId === deleteTarget.id) {
        resetForm();
      }
      setDeleteTarget(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to delete account.");
    } finally {
      setBusy(false);
    }
  }

  async function toggleQuickLaunch(account: LoginAccount) {
    setBusy(true);
    setError(null);
    try {
      await updateLoginAccount(account.id, {
        name: account.name,
        email: account.email,
        password: account.password,
        displayInQuick: !account.displayInQuick,
        icon: account.icon || "👤"
      });
      await loadAccounts();
      await onAccountsChanged?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to update quick launch state.");
    } finally {
      setBusy(false);
    }
  }

  function beginEdit(account: LoginAccount) {
    setEditingId(account.id);
    setForm({
      name: account.name,
      email: account.email,
      password: account.password,
      displayInQuick: account.displayInQuick,
      icon: account.icon || "👤"
    });
    setFormOpen(true);
  }

  async function onBackupToOneDrive() {
    setBusy(true);
    setError(null);
    try {
      await backupLoginAccountsToOneDrive();
      await loadOneDriveBackupStatus();
      onMessage?.("Login accounts backed up to OneDrive.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to back up accounts to OneDrive.");
    } finally {
      setBusy(false);
    }
  }

  async function onOpenOneDriveBackupFolder() {
    setBusy(true);
    setError(null);
    try {
      await openLoginOneDriveBackupFolder();
      onMessage?.("Opened OneDrive backup folder.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to open OneDrive backup folder.");
    } finally {
      setBusy(false);
    }
  }

  async function onRestoreFromOneDrive() {
    if (!selectedBackupName) {
      onMessage?.("Choose a OneDrive backup to restore.");
      return;
    }

    const confirmed = window.confirm(`Restore login accounts from "${selectedBackupName}"? Current accounts will be backed up first.`);
    if (!confirmed) {
      return;
    }

    setBusy(true);
    setError(null);
    try {
      await restoreLoginAccountsFromOneDrive(selectedBackupName);
      await loadAccounts();
      await loadOneDriveBackupStatus();
      await onAccountsChanged?.();
      onMessage?.("Login accounts restored from OneDrive.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to restore accounts from OneDrive.");
    } finally {
      setBusy(false);
    }
  }
  return (
    <section className="loginManager">
      <div className="card serviceContentPanel">
        <div className="loginManagerHeader">
          <p className="sectionTitle">Login manager</p>
          <p className="activeProfileMeta">Store multiple ROSE accounts locally.</p>
        </div>
        <div className="headerActions loginManagerActions">
            <button
              type="button"
              className="iconBtn iconBtnSubtle loginAddButton"
              onClick={openCreateForm}
              disabled={busy}
              aria-label="Add account"
              title="Add account"
            >
              <PlusIcon className="heroIcon" aria-hidden="true" />
            </button>
        </div>

        {error ? <p className="formError">{error}</p> : null}

        <BackupProviderPanel
          available={Boolean(oneDriveBackup?.available)}
          providerName={oneDriveBackup?.providerName ?? null}
          backupRootPath={oneDriveBackup?.backupRootPath ?? null}
          backups={oneDriveBackup?.backups ?? []}
          selectedBackupName={selectedBackupName}
          busy={busy}
          label="login"
          onBackup={onBackupToOneDrive}
          onOpenFolder={onOpenOneDriveBackupFolder}
          onRestore={onRestoreFromOneDrive}
          onSelectedBackupChange={setSelectedBackupName}
        />

        <div className="loginList">
          {accounts.length === 0 ? (
            <p className="emptyState">No saved accounts yet.</p>
          ) : (
            accounts.map((account) => (
              <div key={account.id} className="loginAccountItem">
                <div className="loginAccountSummary">
                  <div className="loginAvatar" aria-hidden="true">{account.icon || "👤"}</div>
                  <div className="loginAccountMeta">
                    <strong>{account.name}</strong>
                    <span>{account.email}</span>
                    {account.displayInQuick ? <small>Quick launch enabled</small> : <small>Hidden from quick launch</small>}
                  </div>
                </div>

                <div className="row loginActions">
                  <button
                    type="button"
                    className={`iconBtn ${account.displayInQuick ? "loginQuickStarActive" : "loginQuickStarInactive"}`}
                    onClick={() => toggleQuickLaunch(account)}
                    disabled={busy}
                    aria-label={account.displayInQuick ? "Remove from quick launch" : "Add to quick launch"}
                    title={account.displayInQuick ? "Remove from quick launch" : "Add to quick launch"}
                  >
                    <StarIcon className="heroIcon" aria-hidden="true" />
                  </button>
                  <button
                    type="button"
                    className="iconBtn loginEditButton"
                    onClick={() => beginEdit(account)}
                    disabled={busy}
                    aria-label={`Edit ${account.name}`}
                    title={`Edit ${account.name}`}
                  >
                    <PencilIcon className="heroIcon" aria-hidden="true" />
                  </button>
                  <button
                    type="button"
                    className="iconBtn loginDeleteButton"
                    onClick={() => setDeleteTarget(account)}
                    disabled={busy}
                    aria-label={`Delete ${account.name}`}
                    title={`Delete ${account.name}`}
                  >
                    <TrashIcon className="heroIcon" aria-hidden="true" />
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {formOpen ? (
        <div className="loginModalOverlay" onClick={() => resetForm()}>
          <div className="loginModal" onClick={(event) => event.stopPropagation()}>
            <div className="loginModalHeader">
              <h3>{editingId ? "Edit account" : "Add account"}</h3>
              <button type="button" className="iconBtn iconBtnSubtle loginModalClose" onClick={resetForm} aria-label="Close">
                ×
              </button>
            </div>

            <form className="loginForm" onSubmit={onSubmit}>
              <div className="loginFields">
                <div className="loginStackedFields">
                  <label className="fieldGroup loginFullWidthField">
                    <span>Icon</span>
                    <select
                      value={form.icon}
                      onChange={(event) => setForm((current) => ({ ...current, icon: event.target.value || "👤" }))}
                    >
                      {ACCOUNT_ICON_OPTIONS.map((option) => (
                        <option key={option} value={option}>
                          {option}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label className="fieldGroup loginFullWidthField">
                    <span>Name</span>
                    <input
                      value={form.name}
                      onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
                      placeholder="Name"
                      required
                    />
                  </label>
                  <label className="fieldGroup loginFullWidthField">
                    <span>Email</span>
                    <input
                      type="email"
                      value={form.email}
                      onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))}
                      placeholder="you@example.com"
                      required
                    />
                  </label>
                  <label className="fieldGroup loginFullWidthField">
                    <span>Password</span>
                    <input
                      type="password"
                      value={form.password}
                      onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
                      placeholder="Password"
                      required
                    />
                  </label>
                </div>
                <label className="checkboxRow">
                  <input
                    type="checkbox"
                    checked={form.displayInQuick}
                    onChange={(event) => setForm((current) => ({ ...current, displayInQuick: event.target.checked }))}
                  />
                  <span>Display in quick launch</span>
                </label>
              </div>

              <div className="loginModalActions">
                <button type="button" className="buttonSubtle" onClick={resetForm} disabled={busy}>
                  Cancel
                </button>
                <button type="submit" className="buttonStrong" disabled={busy}>
                  {editingId ? "Save changes" : "Add account"}
                </button>
              </div>
            </form>
          </div>
        </div>
      ) : null}

      <ConfirmationModal
        open={deleteTarget !== null}
        title="Delete account"
        message={`Are you sure you want to delete "${deleteTarget?.name ?? "this account"}"? This action cannot be undone.`}
        confirmLabel="Delete"
        cancelLabel="Cancel"
        onConfirm={onDeleteConfirmed}
        onClose={() => setDeleteTarget(null)}
      />
    </section>
  );
}
