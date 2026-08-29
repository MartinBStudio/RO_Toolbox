import {useEffect, useState} from "react";
import {LogicalSize, getCurrentWindow} from "@tauri-apps/api/window";
import { SparklesIcon } from "@heroicons/react/24/outline";
import {AppHeader} from "./elements/AppHeader.tsx";
import {AppFooter} from "./elements/AppFooter.tsx";
import {LoadingOverlay} from "./elements/LoadingOverlay.tsx";
import {BackendReadyGate} from "./elements/BackendReadyGate.tsx";
import {LootManager} from "./components/LootManager";
import {CombatTextManager} from "./components/CombatTextManager";
import {SettingsModal} from "./elements/SettingsModal.tsx";
import {GameFolderSetupModal} from "./elements/GameFolderSetupModal.tsx";
import {StatusMessage} from "./elements/StatusMessage.tsx";
import {HowToUseModal} from "./elements/HowToUseModal.tsx";
import {useApplicationContext} from "./context/ApplicationContext.tsx";

const SERVICES = [
    {id: "texture-replacer", title: "Texture replacer"}
] as const;

function App() {
    const {backendReady, status, appVersion, refreshStatus} = useApplicationContext();
    const [loading, setLoading] = useState(false);
    const [loadingMessage, setLoadingMessage] = useState<string | undefined>(undefined);
    const [message, setMessage] = useState("");
    const [settingsOpen, setSettingsOpen] = useState(false);
    const [howToUseOpen, setHowToUseOpen] = useState(false);
    const [selectedService, setSelectedService] = useState<(typeof SERVICES)[number]["id"]>("texture-replacer");

    const needsSetup = backendReady && status !== null && !status.selectedGameBase;

    useEffect(() => {
        if (!message) {
            return;
        }
        const timer = window.setTimeout(() => setMessage(""), 5000);
        return () => window.clearTimeout(timer);
    }, [message]);
    useEffect(() => {
        if (!backendReady) {
            return;
        }

        const appWindow = getCurrentWindow();
        let frameId = 0;
        const minHeight = 350;
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
                        onBusyChange={(busy, msg) => { setLoading(busy); setLoadingMessage(busy ? msg : undefined); }}
                        onMessage={setMessage}
                        loading={loading}
                        appVersion={appVersion ?? undefined}
                        backendVersion={status?.version}
                    />
                    <div className="appWorkspace">
                        <aside className="appSidebar">
                            <div className="card sidebarPanel">
                                <div className="serviceList">
                                    {SERVICES.map((service) => (
                                        <button
                                            key={service.id}
                                            type="button"
                                            className={`serviceListItem${selectedService === service.id ? " serviceListItemActive" : ""}`}
                                            onClick={() => setSelectedService(service.id)}
                                        >
                                            <span className="serviceListIcon" aria-hidden="true">
                                                <SparklesIcon />
                                            </span>
                                            <span>{service.title}</span>
                                        </button>
                                    ))}
                                </div>
                            </div>
                        </aside>

                        <section className="appContent">
                            {selectedService === "texture-replacer" && (
                                <div className="card serviceContentPanel">
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
                                </div>
                            )}
                        </section>
                    </div>
                    <StatusMessage message={message}/>
                    <AppFooter/>
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
