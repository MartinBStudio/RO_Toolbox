import {useEffect, useState} from "react";
import {LogicalSize, getCurrentWindow} from "@tauri-apps/api/window";
import {AppHeader} from "./elements/AppHeader.tsx";
import {AppFooter} from "./elements/AppFooter.tsx";
import {LoadingOverlay} from "./elements/LoadingOverlay.tsx";
import {BackendReadyGate} from "./elements/BackendReadyGate.tsx";
import {LootManager} from "./components/LootManager";
import {CombatTextManager} from "./components/CombatTextManager";
import {ServiceContainer} from "./elements/ServiceContainer.tsx";
import {SettingsModal} from "./elements/SettingsModal.tsx";
import {GameFolderSetupModal} from "./elements/GameFolderSetupModal.tsx";
import {StatusMessage} from "./elements/StatusMessage.tsx";
import {HowToUseModal} from "./elements/HowToUseModal.tsx";
import {useApplicationContext} from "./context/ApplicationContext.tsx";

function App() {
    const {backendReady, status, appVersion, refreshStatus} = useApplicationContext();
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState("");
    const [settingsOpen, setSettingsOpen] = useState(false);
    const [howToUseOpen, setHowToUseOpen] = useState(false);

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
        const minHeight = 420;
        const maxHeight = 920;
        const verticalPadding = 26;

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
                        onBusyChange={setLoading}
                        onMessage={setMessage}
                        loading={loading}
                        appVersion={appVersion ?? undefined}
                        backendVersion={status?.version}
                    />
                    <ServiceContainer
                        groups={[
                            {
                                id: "texture-replacer",
                                title: "Texture replacer",
                                contents: [
                                    <LootManager
                                        key="texture-replacer-loot-1"
                                        status={status}
                                        loading={loading}
                                        onBusyChange={setLoading}
                                        onStatusRefresh={refreshStatus}
                                        onMessage={setMessage}
                                    />,
                                    <CombatTextManager
                                        key="texture-replacer-combattext-1"
                                        status={status}
                                        loading={loading}
                                        onBusyChange={setLoading}
                                        onStatusRefresh={refreshStatus}
                                        onMessage={setMessage}
                                    />
                                ]
                            }
                        ]}
                    />
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
                    <LoadingOverlay visible={loading}/>
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
