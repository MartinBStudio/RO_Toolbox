import { createContext, useContext, useEffect, useState, useCallback, type ReactNode } from "react";
import { getVersion } from "@tauri-apps/api/app";
import { getStatus } from "../backendConnector/appApi.ts";
import type { AppStatus } from "../types";

const POLL_INTERVAL_MS = 600;
const MAX_WAIT_MS = 60_000;

type ApplicationContextValue = {
    backendReady: boolean;
    startupError: string | null;
    status: AppStatus | null;
    appVersion: string | null;
    refreshStatus: () => Promise<void>;
};

const ApplicationContext = createContext<ApplicationContextValue | null>(null);

export function ApplicationProvider({ children }: { children: ReactNode }) {
    const [backendReady, setBackendReady] = useState(false);
    const [startupError, setStartupError] = useState<string | null>(null);
    const [status, setStatus] = useState<AppStatus | null>(null);
    const [appVersion, setAppVersion] = useState<string | null>(null);

    const refreshStatus = useCallback(async () => {
        const updated = await getStatus();
        setStatus(updated);
    }, []);

    useEffect(() => {
        let cancelled = false;
        const start = Date.now();

        getVersion()
            .then((v) => { if (!cancelled) setAppVersion(v); })
            .catch(() => undefined);

        async function poll() {
            while (!cancelled) {
                try {
                    const s = await getStatus();
                    if (!cancelled) {
                        setStatus(s);
                        setBackendReady(true);
                    }
                    return;
                } catch {
                    if (Date.now() - start >= MAX_WAIT_MS) {
                        if (!cancelled) {
                            setStartupError(
                                "Backend failed to start within the expected time. Please restart the application."
                            );
                        }
                        return;
                    }
                    await new Promise<void>((resolve) => setTimeout(resolve, POLL_INTERVAL_MS));
                }
            }
        }

        poll();
        return () => { cancelled = true; };
    }, []);

    return (
        <ApplicationContext.Provider value={{ backendReady, startupError, status, appVersion, refreshStatus }}>
            {children}
        </ApplicationContext.Provider>
    );
}

export function useApplicationContext(): ApplicationContextValue {
    const ctx = useContext(ApplicationContext);
    if (!ctx) {
        throw new Error("useApplicationContext must be used inside ApplicationProvider");
    }
    return ctx;
}
