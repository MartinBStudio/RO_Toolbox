import {useEffect, useState} from "react";
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
import {QuickLaunchModePanel} from "./components/QuickLaunchModePanel.tsx";
import {useApplicationContext} from "./context/ApplicationContext.tsx";
import {useWindowMode} from "./hooks/useWindowMode.ts";
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
    const {backendReady, status, appVersion, refreshStatus, quickLaunchOnlyMode, setQuickLaunchOnlyMode} = useApplicationContext();
    const [loading, setLoading] = useState(false);
    const [loadingMessage, setLoadingMessage] = useState<string | undefined>(undefined);
    const [message, setMessage] = useState("");
    const [settingsOpen, setSettingsOpen] = useState(false);
    const [howToUseOpen, setHowToUseOpen] = useState(false);
    const [releaseNotesOpen, setReleaseNotesOpen] = useState(false);
    const [releaseNotesContent, setReleaseNotesContent] = useState("");
    const [quickAccounts, setQuickAccounts] = useState<LoginAccount[]>([]);
    const [factoryResetNonce, setFactoryResetNonce] = useState(0);
    const [servicePreferenceLoaded, setServicePreferenceLoaded] = useState(false);
    const [selectedService, setSelectedService] = useState<(typeof SERVICES)[number]["id"]>(readInitialService);
    const [minimumStartupDisplayReached, setMinimumStartupDisplayReached] = useState(false);

    const needsSetup = backendReady && status !== null && !status.selectedGameBase;
    const hasQuickLaunchProfiles = quickAccounts.length > 0;
    const quickLaunchOnlyActive = quickLaunchOnlyMode && hasQuickLaunchProfiles;

    useEffect(() => {
        const timer = window.setTimeout(() => setMinimumStartupDisplayReached(true), 2000);
        return () => window.clearTimeout(timer);
    }, []);

    const showStartupScreen = !backendReady || !minimumStartupDisplayReached;

    useWindowMode(backendReady && minimumStartupDisplayReached, quickLaunchOnlyActive);

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

    async function onEnterQuickLaunchOnlyMode() {
        try {
            await setQuickLaunchOnlyMode(true);
        } catch (err) {
            setMessage(toErrorMessage(err, "Failed to enable quick launch mode."));
        }
    }

    async function onExitQuickLaunchOnlyMode() {
        try {
            await setQuickLaunchOnlyMode(false);
        } catch (err) {
            setMessage(toErrorMessage(err, "Failed to disable quick launch mode."));
        }
    }

    useEffect(() => {
        void refreshQuickAccounts();
    }, [backendReady]);

    useEffect(() => {
        const handleFactoryReset = () => {
            void refreshQuickAccounts();
            setFactoryResetNonce((value) => value + 1);
            void setQuickLaunchOnlyMode(false);
        };
        window.addEventListener("roToolbox:factory-reset", handleFactoryReset);
        return () => {
            window.removeEventListener("roToolbox:factory-reset", handleFactoryReset);
        };
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

    return (
        <main className={`layout${loading ? " layoutLoading" : ""}${quickLaunchOnlyActive ? " layoutQuickLaunchOnly" : ""}`}>
            <BackendReadyGate onStartupError={setMessage} showStartupScreen={showStartupScreen}>
                <>
                    {!quickLaunchOnlyActive && (
                        <AppHeader
                            onOpenSettings={() => setSettingsOpen(true)}
                            onOpenHowToUse={() => setHowToUseOpen(true)}
                            onLaunchRose={onQuickLaunch}
                            onBusyChange={(busy, msg) => { setLoading(busy); setLoadingMessage(busy ? msg : undefined); }}
                            onMessage={setMessage}
                            onQuickLaunchAccount={onQuickLaunchAccount}
                            quickAccounts={quickAccounts}
                            loading={loading}
                            launchDisabled={!status?.selectedGameBase}
                            appVersion={appVersion ?? undefined}
                            backendVersion={status?.version}
                            quickLaunchOnlyMode={quickLaunchOnlyActive}
                            canToggleQuickLaunchOnlyMode={hasQuickLaunchProfiles}
                            onToggleQuickLaunchOnlyMode={onEnterQuickLaunchOnlyMode}
                        />
                    )}
                    <div className="appMainScroll">
                        {quickLaunchOnlyActive ? (
                            <QuickLaunchModePanel
                                accounts={quickAccounts}
                                loading={loading}
                                launchDisabled={!status?.selectedGameBase}
                                onLaunchAccount={onQuickLaunchAccount}
                                onExit={onExitQuickLaunchOnlyMode}
                            />
                        ) : (
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
                                                <p className="activeProfileMeta">Manage loot, combat text, and user interface packages.</p>
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
                                        <LoginManager
                                            key={`login-manager-${factoryResetNonce}`}
                                            onAccountsChanged={refreshQuickAccounts}
                                            onMessage={setMessage}
                                        />
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
                        )}
                        {!quickLaunchOnlyActive && <StatusMessage message={message} onDismiss={() => setMessage("")}/>}
                    </div>
                    {!quickLaunchOnlyActive && (
                        <AppFooter
                            loading={loading}
                            onOpenWhatsNew={onOpenWhatsNew}
                        />
                    )}
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
