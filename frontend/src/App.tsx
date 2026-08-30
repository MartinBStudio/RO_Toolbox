import {useEffect, useState} from "react";
import {LogicalSize, getCurrentWindow} from "@tauri-apps/api/window";
import { KeyIcon, SwatchIcon, WrenchScrewdriverIcon } from "@heroicons/react/24/outline";
import {AppHeader} from "./elements/AppHeader.tsx";
import {AppFooter} from "./elements/AppFooter.tsx";
import {LoadingOverlay} from "./elements/LoadingOverlay.tsx";
import {BackendReadyGate} from "./elements/BackendReadyGate.tsx";
import {LootManager} from "./components/LootManager";
import {CombatTextManager} from "./components/CombatTextManager";
import {UserInterfaceManager} from "./components/UserInterfaceManager";
import {LoginManager} from "./components/LoginManager.tsx";
import {ConfigEditorManager} from "./components/ConfigEditorManager.tsx";
import {SettingsModal} from "./elements/SettingsModal.tsx";
import {GameFolderSetupModal} from "./elements/GameFolderSetupModal.tsx";
import {StatusMessage} from "./elements/StatusMessage.tsx";
import {HowToUseModal} from "./elements/HowToUseModal.tsx";
import {ReleaseNotesModal} from "./elements/ReleaseNotesModal.tsx";
import {useApplicationContext} from "./context/ApplicationContext.tsx";
import {
    getReleaseNotes,
    getSelectedServiceSetting,
    listQuickLoginAccounts,
    quickLaunchGame,
    quickLaunchLoginAccount,
    saveSelectedServiceSetting,
    type LoginAccount
} from "./backendConnector/api.ts";

const SERVICES = [
    {id: "texture-replacer", title: "Texture replacer", icon: SwatchIcon},
    {id: "login-manager", title: "Login manager", icon: KeyIcon},
    {id: "config-editor", title: "Config editor", icon: WrenchScrewdriverIcon}
] as const;
const SELECTED_SERVICE_STORAGE_KEY = "roToolbox.selectedService";
const DEFAULT_SERVICE_ID = SERVICES[0].id;

function isServiceId(value: string): value is (typeof SERVICES)[number]["id"] {
    return SERVICES.some((service) => service.id === value);
}

function readInitialService() {
    try {
        const stored = window.localStorage.getItem(SELECTED_SERVICE_STORAGE_KEY);
        if (stored && isServiceId(stored)) {
            return stored;
        }
    } catch {
        // Ignore storage failures and use default service.
    }
    return DEFAULT_SERVICE_ID;
}

function App() {
    const {backendReady, status, appVersion, refreshStatus} = useApplicationContext();
    const [loading, setLoading] = useState(false);
    const [loadingMessage, setLoadingMessage] = useState<string | undefined>(undefined);
    const [message, setMessage] = useState("");
    const [settingsOpen, setSettingsOpen] = useState(false);
    const [howToUseOpen, setHowToUseOpen] = useState(false);
    const [releaseNotesOpen, setReleaseNotesOpen] = useState(false);
    const [releaseNotesContent, setReleaseNotesContent] = useState("");
    const [quickAccounts, setQuickAccounts] = useState<LoginAccount[]>([]);
    const [servicePreferenceLoaded, setServicePreferenceLoaded] = useState(false);
    const [selectedService, setSelectedService] = useState<(typeof SERVICES)[number]["id"]>(readInitialService);

    const needsSetup = backendReady && status !== null && !status.selectedGameBase;

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

    async function onQuickLaunch() {
        setLoading(true);
        try {
            await quickLaunchGame();
            setMessage("ROSE Online launched.");
        } catch (err) {
            setMessage(toErrorMessage(err, "Failed to launch ROSE Online."));
        } finally {
            setLoading(false);
        }
    }

    async function onOpenWhatsNew() {
        setLoading(true);
        try {
            const result = await getReleaseNotes();
            setReleaseNotesContent(result.content ?? "");
            setReleaseNotesOpen(true);
        } catch (err) {
            setMessage(toErrorMessage(err, "Failed to load release notes."));
        } finally {
            setLoading(false);
        }
    }

    async function refreshQuickAccounts() {
        if (!backendReady) {
            return;
        }
        try {
            const accounts = await listQuickLoginAccounts();
            setQuickAccounts(accounts);
        } catch {
            setQuickAccounts([]);
        }
    }

    async function onQuickLaunchAccount(account: LoginAccount) {
        setLoading(true);
        try {
            await quickLaunchLoginAccount(account.id);
            setMessage(`ROSE Online launched for ${account.name}.`);
        } catch (err) {
            setMessage(toErrorMessage(err, `Failed to launch ROSE Online for ${account.name}.`));
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        void refreshQuickAccounts();
    }, [backendReady]);

    useEffect(() => {
        if (!message) {
            return;
        }
        const timer = window.setTimeout(() => setMessage(""), 5000);
        return () => window.clearTimeout(timer);
    }, [message]);

    useEffect(() => {
        try {
            window.localStorage.setItem(SELECTED_SERVICE_STORAGE_KEY, selectedService);
        } catch {
            // Ignore storage failures and keep the in-memory selected service.
        }
    }, [selectedService]);

    useEffect(() => {
        if (!backendReady) {
            setServicePreferenceLoaded(false);
            return;
        }

        let cancelled = false;
        getSelectedServiceSetting()
            .then((response) => {
                if (!cancelled && response.serviceId && isServiceId(response.serviceId)) {
                    setSelectedService(response.serviceId);
                }
            })
            .catch(() => undefined)
            .finally(() => {
                if (!cancelled) {
                    setServicePreferenceLoaded(true);
                }
            });

        return () => {
            cancelled = true;
        };
    }, [backendReady]);

    useEffect(() => {
        if (!backendReady || !servicePreferenceLoaded) {
            return;
        }
        saveSelectedServiceSetting(selectedService).catch(() => undefined);
    }, [backendReady, servicePreferenceLoaded, selectedService]);

    useEffect(() => {
        if (!backendReady) {
            return;
        }

        const appWindow = getCurrentWindow();
        let frameId = 0;
        const minHeight = 430;
        const maxHeight = 760;
        const verticalPadding = 20;

        const syncHeight = async () => {
            const contentHeight = Math.ceil(document.documentElement.scrollHeight + verticalPadding);
            const targetHeight = Math.max(minHeight, Math.min(maxHeight, contentHeight));
            const currentSize = await appWindow.innerSize();
            if (Math.abs(currentSize.height - targetHeight) > 2) {
                await appWindow.setSize(new LogicalSize(currentSize.width, targetHeight));
            }
        };

        const observer = new ResizeObserver(() => {
            if (frameId) {
                window.cancelAnimationFrame(frameId);
            }
            frameId = window.requestAnimationFrame(() => {
                syncHeight().catch(() => undefined);
            });
        });

        observer.observe(document.documentElement);
        syncHeight().catch(() => undefined);

        return () => {
            observer.disconnect();
            if (frameId) {
                window.cancelAnimationFrame(frameId);
            }
        };
    }, [backendReady]);

    return (
        <main className={`layout${loading ? " layoutLoading" : ""}`}>
            <BackendReadyGate onStartupError={setMessage}>
                <>
                    <AppHeader
                        onOpenSettings={() => setSettingsOpen(true)}
                        onOpenHowToUse={() => setHowToUseOpen(true)}
                        onLaunchRose={onQuickLaunch}
                        onBusyChange={(busy, msg) => { setLoading(busy); setLoadingMessage(busy ? msg : undefined); }}
                        onMessage={setMessage}
                        loading={loading}
                        launchDisabled={!status?.selectedGameBase}
                        appVersion={appVersion ?? undefined}
                        backendVersion={status?.version}
                    />
                    <div className="appMainScroll">
                        {quickAccounts.length > 0 && (
                            <section className="card quickLaunchCard">
                                <div className="quickAccountList" aria-label="Quick launch accounts">
                                    {quickAccounts.map((account) => (
                                        <button
                                            key={account.id}
                                            type="button"
                                            className="quickAccountButton"
                                            onClick={() => onQuickLaunchAccount(account)}
                                            disabled={loading || !status?.selectedGameBase}
                                            title={`Launch ${account.name}`}
                                        >
                                            <span className="quickAccountIcon" aria-hidden="true">{account.icon || "👤"}</span>
                                            <span className="quickAccountName">{account.name}</span>
                                        </button>
                                    ))}
                                </div>
                            </section>
                        )}
                        <div className="appWorkspace">
                            <aside className="appSidebar">
                                <div className="card sidebarPanel">
                                    <div className="serviceList">
                                        {SERVICES.map((service) => {
                                            const ServiceIcon = service.icon;
                                            return (
                                                <button
                                                    key={service.id}
                                                    type="button"
                                                    className={`serviceListItem${selectedService === service.id ? " serviceListItemActive" : ""}`}
                                                    onClick={() => setSelectedService(service.id)}
                                                >
                                                    <span className="serviceListIcon" aria-hidden="true">
                                                        <ServiceIcon />
                                                    </span>
                                                    <span>{service.title}</span>
                                                </button>
                                            );
                                        })}
                                    </div>
                                </div>
                            </aside>

                            <section className="appContent">
                                {selectedService === "texture-replacer" && (
                                    <div className="card serviceContentPanel">
                                        <div>
                                            <p className="sectionTitle">Texture replacer</p>
                                            <p className="activeProfileMeta">Manage loot, combat text, and user interface profile packs.</p>
                                        </div>
                                        <LootManager
                                            status={status}
                                            loading={loading}
                                            onBusyChange={setLoading}
                                            onStatusRefresh={refreshStatus}
                                            onMessage={setMessage}
                                        />
                                        <CombatTextManager
                                            status={status}
                                            loading={loading}
                                            onBusyChange={setLoading}
                                            onStatusRefresh={refreshStatus}
                                            onMessage={setMessage}
                                        />
                                        <UserInterfaceManager
                                            status={status}
                                            loading={loading}
                                            onBusyChange={setLoading}
                                            onStatusRefresh={refreshStatus}
                                            onMessage={setMessage}
                                        />
                                    </div>
                                )}
                                {selectedService === "login-manager" && (
                                    <LoginManager onAccountsChanged={refreshQuickAccounts} />
                                )}
                                {selectedService === "config-editor" && (
                                    <ConfigEditorManager
                                        loading={loading}
                                        onBusyChange={setLoading}
                                        onMessage={setMessage}
                                    />
                                )}
                            </section>
                        </div>
                        <StatusMessage message={message}/>
                    </div>
                    <AppFooter
                        loading={loading}
                        onOpenWhatsNew={onOpenWhatsNew}
                    />
                    <SettingsModal
                        open={settingsOpen}
                        status={status}
                        loading={loading}
                        onClose={() => setSettingsOpen(false)}
                        onBusyChange={setLoading}
                        onStatusRefresh={refreshStatus}
                        onMessage={setMessage}
                    />
                    <HowToUseModal
                        open={howToUseOpen}
                        onClose={() => setHowToUseOpen(false)}
                    />
                    <ReleaseNotesModal
                        open={releaseNotesOpen}
                        content={releaseNotesContent}
                        onClose={() => setReleaseNotesOpen(false)}
                    />
                    <LoadingOverlay visible={loading} label={loadingMessage}/>
                    {needsSetup && (
                        <GameFolderSetupModal
                            loading={loading}
                            onBusyChange={setLoading}
                            onStatusRefresh={refreshStatus}
                            onMessage={setMessage}
                        />
                    )}
                </>
            </BackendReadyGate>
        </main>
    );
}

export default App;
