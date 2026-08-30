import { useEffect, useRef, useState } from "react";
import type { ChangeEvent, FormEvent } from "react";
import { save as saveDialog } from "@tauri-apps/plugin-dialog";
import {
  createLoginAccount,
  deleteLoginAccount,
  exportLoginAccounts,
  importLoginAccounts,
  listLoginAccounts,
  saveLoginAccountsExportFile,
  updateLoginAccount,
  type LoginAccount
} from "../backendConnector/loginApi.ts";
import { ConfirmationModal } from "../elements/ConfirmationModal.tsx";

const ACCOUNT_ICON_OPTIONS = [
  "👤",
  "✚",
  "🪙",
  "🛡️",
  "🪄",
  "🏹",
  "⚔️",
  "💎",
  "✦",
  "⚑"
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
  const importInputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    void loadAccounts();
  }, []);

  async function loadAccounts() {
    try {
      const data = await listLoginAccounts();
      setAccounts(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load accounts.");
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

  function normalizeExportFileName() {
    const now = new Date();
    const pad = (value: number) => String(value).padStart(2, "0");
    const timestamp = `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}-${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`;
    return `ro-toolbox-accounts-${timestamp}.json`;
  }

  async function onExportAccounts() {
    setBusy(true);
    setError(null);
    try {
      const payload = await exportLoginAccounts();
      const savePath = await saveDialog({
        title: "Export login accounts",
        defaultPath: normalizeExportFileName(),
        filters: [{ name: "JSON", extensions: ["json"] }]
      });
      if (!savePath) {
        return;
      }
      await saveLoginAccountsExportFile({
        filePath: savePath,
        content: JSON.stringify(payload, null, 2)
      });
      onMessage?.("Accounts exported.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to export accounts.");
    } finally {
      setBusy(false);
    }
  }

  async function onImportAccounts(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) {
      return;
    }

    const confirmed = window.confirm("Import accounts and replace current saved accounts?");
    if (!confirmed) {
      return;
    }

    setBusy(true);
    setError(null);
    try {
      const content = await file.text();
      const parsed = JSON.parse(content) as { accounts?: unknown };
      if (!parsed || typeof parsed !== "object" || !Array.isArray(parsed.accounts)) {
        throw new Error("Invalid backup file format.");
      }
      const accountsPayload = parsed.accounts.map((entry) => {
        if (!entry || typeof entry !== "object") {
          throw new Error("Invalid account entry in backup file.");
        }
        const account = entry as {
          id?: unknown;
          name?: unknown;
          email?: unknown;
          password?: unknown;
          displayInQuick?: unknown;
          icon?: unknown;
        };
        if (typeof account.name !== "string" || !account.name.trim()) {
          throw new Error("Each imported account must have a name.");
        }
        if (typeof account.email !== "string" || !account.email.trim()) {
          throw new Error("Each imported account must have an email.");
        }
        if (typeof account.password !== "string" || !account.password.trim()) {
          throw new Error("Each imported account must have a password.");
        }
        return {
          id: typeof account.id === "string" && account.id.trim() ? account.id.trim() : undefined,
          name: account.name.trim(),
          email: account.email.trim(),
          password: account.password,
          displayInQuick: typeof account.displayInQuick === "boolean" ? account.displayInQuick : true,
          icon: typeof account.icon === "string" && account.icon.trim() ? account.icon.trim() : "👤"
        };
      });

      await importLoginAccounts({
        accounts: accountsPayload,
        replaceExisting: true
      });
      await loadAccounts();
      await onAccountsChanged?.();
      onMessage?.("Accounts imported.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to import accounts.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="loginManager">
      <div className="card serviceContentPanel">
        <div className="headerRow">
          <div>
            <p className="sectionTitle">Login manager</p>
            <p className="activeProfileMeta">Store multiple ROSE accounts locally.</p>
          </div>
          <div className="loginHeaderActions">
            <button
              type="button"
              className="buttonSubtle"
              onClick={onExportAccounts}
              disabled={busy}
            >
              Export
            </button>
            <button
              type="button"
              className="buttonSubtle"
              onClick={() => importInputRef.current?.click()}
              disabled={busy}
            >
              Import
            </button>
            <button
              type="button"
              className="iconBtn iconBtnSubtle loginAddButton"
              onClick={openCreateForm}
              disabled={busy}
              aria-label="Add account"
              title="Add account"
            >
              +
            </button>
          </div>
        </div>

        {error ? <p className="formError">{error}</p> : null}
        <input
          ref={importInputRef}
          type="file"
          accept="application/json,.json"
          onChange={onImportAccounts}
          style={{ display: "none" }}
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
                    ★
                  </button>
                  <button type="button" className="buttonSubtle" onClick={() => beginEdit(account)} disabled={busy}>
                    Edit
                  </button>
                  <button type="button" className="buttonDanger" onClick={() => setDeleteTarget(account)} disabled={busy}>
                    Delete
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
