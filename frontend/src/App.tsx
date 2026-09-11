import {useCallback, useEffect, useRef, useState} from "react";
import { getCurrentWindow } from "@tauri-apps/api/window";
import { invoke } from "@tauri-apps/api/core";
import { KeyIcon, SwatchIcon, WrenchScrewdriverIcon } from "@heroicons/react/24/outline";
import {AppHeader} from "./elements/AppHeader.tsx";
import {AppFooter} from "./elements/AppFooter.tsx";
import {LoadingOverlay} from "./elements/LoadingOverlay.tsx";
import {BackendReadyGate} from "./elements/BackendReadyGate.tsx";
import {LootManager} from "./components/LootManager";
import {CombatTextManager} from "./components/CombatTextManager";
import {UserInterfaceManager} from "./components/UserInterfaceManager";
import {BuffIconsManager} from "./components/BuffIconsManager";
import {BuffsManager} from "./components/BuffsManager";
import {LoginManager} from "./components/LoginManager.tsx";
import {ConfigEditorManager} from "./components/ConfigEditorManager.tsx";
import {SettingsModal} from "./elements/SettingsModal.tsx";
import {GameFolderSetupModal} from "./elements/GameFolderSetupModal.tsx";
import {StatusMessage} from "./elements/StatusMessage.tsx";
import {HowToUseModal} from "./elements/HowToUseModal.tsx";
import {ReleaseNotesModal} from "./elements/ReleaseNotesModal.tsx";
import {TroseRunningBanner} from "./elements/TroseRunningBanner.tsx";
import {QuickLaunchModePanel} from "./components/QuickLaunchModePanel.tsx";
import {UsefulStuffPanel} from "./elements/UsefulStuffPanel.tsx";
import {useApplicationContext} from "./context/ApplicationContext.tsx";
import {useWindowMode} from "./hooks/useWindowMode.ts";
import { ConfirmationModal } from "./elements/ConfirmationModal.tsx";
import {
    drainNotifications,
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
type TaskbarQuickAccount = {
    id: string;
    name: string;
    icon: string;
};

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
    const [loadingOverlayVisible, setLoadingOverlayVisible] = useState(false);
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
    const [troseRefreshLoading, setTroseRefreshLoading] = useState(false);
    const [closeConfirmOpen, setCloseConfirmOpen] = useState(false);
    const allowWindowCloseRef = useRef(false);
    const appInForegroundRef = useRef(false);

    const needsSetup = backendReady && status !== null && !status.selectedGameBase;
    const hasQuickLaunchProfiles = quickAccounts.length > 0;
    const quickLaunchOnlyActive = quickLaunchOnlyMode && hasQuickLaunchProfiles;

    useEffect(() => {
        const timer = window.setTimeout(() => setMinimumStartupDisplayReached(true), 2000);
        return () => window.clearTimeout(timer);
    }, []);

    const showStartupScreen = !backendReady || !minimumStartupDisplayReached;

    useWindowMode(backendReady && minimumStartupDisplayReached, quickLaunchOnlyActive);

    useEffect(() => {
        const appWindow = getCurrentWindow();
        let unlisten: (() => void) | undefined;

        appWindow.onCloseRequested((event) => {
            if (allowWindowCloseRef.current) {
                return;
            }
            event.preventDefault();
            setCloseConfirmOpen(true);
        }).then((dispose) => {
            unlisten = dispose;
        }).catch(() => undefined);

        return () => {
            if (unlisten) {
                unlisten();
            }
        };
    }, []);

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

    const refreshStatusSilently = useCallback(() => {
        void refreshStatus().catch(() => undefined);
    }, [refreshStatus]);
    const consumeTaskbarLaunchMessages = useCallback(() => {
        if (!backendReady) {
            return;
        }
        void drainNotifications()
            .then((messages) => {
                const latestMessage = messages[messages.length - 1];
                if (latestMessage) {
                    setMessage(latestMessage);
                }
            })
            .catch(() => undefined);
    }, [backendReady]);

    const onBusyChange = useCallback((busy: boolean, msg?: string) => {
        setLoading(busy);
        setLoadingOverlayVisible(busy);
        setLoadingMessage(busy ? msg : undefined);
    }, []);

    async function onQuickLaunch() {
        setLoading(true);
        setLoadingOverlayVisible(false);
        setLoadingMessage(undefined);
        try {
            await quickLaunchGame();
            setMessage("ROSE Online launched.");
            await refreshStatus();
        } catch (err) {
            setMessage(toErrorMessage(err, "Failed to launch ROSE Online."));
        } finally {
            setLoading(false);
            setLoadingOverlayVisible(false);
            setLoadingMessage(undefined);
        }
    }

    async function onOpenWhatsNew() {
        onBusyChange(true);
        try {
            const result = await getReleaseNotes();
            setReleaseNotesContent(result.content ?? "");
            setReleaseNotesOpen(true);
        } catch (err) {
            setMessage(toErrorMessage(err, "Failed to load release notes."));
        } finally {
            onBusyChange(false);
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
        setLoadingOverlayVisible(false);
        setLoadingMessage(undefined);
        try {
            await quickLaunchLoginAccount(account.id);
            setMessage(`ROSE Online launched for ${account.name}.`);
            await refreshStatus();
        } catch (err) {
            setMessage(toErrorMessage(err, `Failed to launch ROSE Online for ${account.name}.`));
        } finally {
            setLoading(false);
            setLoadingOverlayVisible(false);
            setLoadingMessage(undefined);
        }
    }

    async function onRefreshTroseStatus() {
        setTroseRefreshLoading(true);
        try {
            await refreshStatus();
        } catch (err) {
            setMessage(toErrorMessage(err, "Failed to refresh game status."));
        } finally {
            setTroseRefreshLoading(false);
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

    async function confirmCloseApplication() {
        allowWindowCloseRef.current = true;
        setCloseConfirmOpen(false);
        try {
            await getCurrentWindow().destroy();
        } catch (err) {
            allowWindowCloseRef.current = false;
            setMessage(toErrorMessage(err, "Failed to close application."));
        }
    }

    useEffect(() => {
        void refreshQuickAccounts();
    }, [backendReady]);

    useEffect(() => {
        if (!backendReady) {
            return;
        }

        const taskbarAccounts: TaskbarQuickAccount[] = quickAccounts.map((account) => ({
            id: account.id,
            name: account.name,
            icon: account.icon
        }));

        invoke("sync_taskbar_quick_launch", {accounts: taskbarAccounts}).catch(() => undefined);
    }, [backendReady, quickAccounts]);

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
        const timer = window.setTimeout(() => setMessage(""), 2500);
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
            appInForegroundRef.current = false;
            return;
        }

        const updateForegroundState = (nextForeground: boolean) => {
            const wasInForeground = appInForegroundRef.current;
            appInForegroundRef.current = nextForeground;
            if (!wasInForeground && nextForeground) {
                refreshStatusSilently();
                consumeTaskbarLaunchMessages();
            }
        };
        const readForegroundState = () => !document.hidden && document.hasFocus();
        const handleWindowFocus = () => {
            updateForegroundState(readForegroundState());
        };
        const handleWindowBlur = () => {
            appInForegroundRef.current = false;
        };
        const handleVisibilityChange = () => {
            updateForegroundState(readForegroundState());
        };

        appInForegroundRef.current = readForegroundState();

        window.addEventListener("focus", handleWindowFocus);
        window.addEventListener("blur", handleWindowBlur);
        document.addEventListener("visibilitychange", handleVisibilityChange);

        return () => {
            window.removeEventListener("focus", handleWindowFocus);
            window.removeEventListener("blur", handleWindowBlur);
            document.removeEventListener("visibilitychange", handleVisibilityChange);
        };
    }, [backendReady, consumeTaskbarLaunchMessages, refreshStatusSilently]);

    useEffect(() => {
        if (!backendReady) {
            return;
        }

        const timer = window.setInterval(() => {
            if (appInForegroundRef.current) {
                consumeTaskbarLaunchMessages();
            }
        }, 2000);

        return () => window.clearInterval(timer);
    }, [backendReady, consumeTaskbarLaunchMessages]);

    return (
        <main className={`layout${loadingOverlayVisible ? " layoutLoading" : ""}${quickLaunchOnlyActive ? " layoutQuickLaunchOnly" : ""}`}>
            <div className="appBackground" aria-hidden="true" />
            <BackendReadyGate onStartupError={setMessage} showStartupScreen={showStartupScreen}>
                <>
                    {!quickLaunchOnlyActive && (
                        <AppHeader
                            onOpenSettings={() => setSettingsOpen(true)}
                            onOpenHowToUse={() => setHowToUseOpen(true)}
                            onLaunchRose={onQuickLaunch}
                            onBusyChange={onBusyChange}
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
                                    <UsefulStuffPanel onMessage={setMessage} />
                                </aside>

                                <section className="appContent">
                                    {selectedService === "texture-replacer" && (
                                        <div className="card serviceContentPanel">
                                            <div>
                                                <p className="sectionTitle">Texture replacer</p>
                                            <p className="activeProfileMeta">Manage loot, combat text, user interface, buffs, and buff icon packages.</p>
                                            </div>
                                            <LootManager
                                                status={status}
                                                loading={loading}
                                                onBusyChange={onBusyChange}
                                                onStatusRefresh={refreshStatus}
                                                onMessage={setMessage}
                                            />
                                            <CombatTextManager
                                                status={status}
                                                loading={loading}
                                                onBusyChange={onBusyChange}
                                                onStatusRefresh={refreshStatus}
                                                onMessage={setMessage}
                                            />
                                            <UserInterfaceManager
                                                status={status}
                                                loading={loading}
                                                onBusyChange={onBusyChange}
                                                onStatusRefresh={refreshStatus}
                                                onMessage={setMessage}
                                            />
                                            <BuffsManager
                                                status={status}
                                                loading={loading}
                                                onBusyChange={onBusyChange}
                                                onStatusRefresh={refreshStatus}
                                                onMessage={setMessage}
                                            />
                                            <BuffIconsManager
                                                status={status}
                                                loading={loading}
                                                onBusyChange={onBusyChange}
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
                                            onBusyChange={onBusyChange}
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
                    {!quickLaunchOnlyActive && Boolean(status?.troseRunning) && (
                        <div className="layoutFloatingStatus">
                            <TroseRunningBanner
                                running={true}
                                loading={troseRefreshLoading || loading}
                                onRefresh={onRefreshTroseStatus}
                            />
                        </div>
                    )}
                    <SettingsModal
                        open={settingsOpen}
                        status={status}
                        loading={loading}
                        onClose={() => setSettingsOpen(false)}
                        onBusyChange={onBusyChange}
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
                    <LoadingOverlay visible={loadingOverlayVisible} label={loadingMessage}/>
                    {needsSetup && (
                        <GameFolderSetupModal
                            loading={loading}
                            onBusyChange={onBusyChange}
                            onStatusRefresh={refreshStatus}
                            onMessage={setMessage}
                        />
                    )}
                    <ConfirmationModal
                        open={closeConfirmOpen}
                        smallMode={quickLaunchOnlyActive}
                        title={quickLaunchOnlyActive ? "Close RO Toolbox?" : "Close application"}
                        message={quickLaunchOnlyActive ? "Exit the app now?" : "Are you sure you want to close RO Toolbox?"}
                        confirmLabel="Close"
                        confirmButtonClassName="buttonDanger"
                        onConfirm={confirmCloseApplication}
                        onClose={() => setCloseConfirmOpen(false)}
                    />
                </>
            </BackendReadyGate>
        </main>
    );
}

export default App;
