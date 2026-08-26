import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { getVersion as getAppVersion } from "@tauri-apps/api/app";
import { getStatus } from "../backendConnector/api.ts";
import type { AppStatus } from "../types";

type ApplicationContextValue = {
  backendReady: boolean;
  status: AppStatus | null;
  appVersion: string | null;
  startupError: string | null;
  refreshStatus: () => Promise<void>;
};

const ApplicationContext = createContext<ApplicationContextValue | null>(null);

type ApplicationProviderProps = {
  children: React.ReactNode;
};

export function ApplicationProvider({ children }: ApplicationProviderProps) {
  const [backendReady, setBackendReady] = useState(false);
  const [status, setStatus] = useState<AppStatus | null>(null);
  const [appVersion, setAppVersion] = useState<string | null>(null);
  const [startupError, setStartupError] = useState<string | null>(null);

  const refreshStatus = useCallback(async () => {
    setStatus(await getStatus());
  }, []);

  useEffect(() => {
    getAppVersion()
      .then((version) => setAppVersion(version))
      .catch(() => setAppVersion(null));
  }, []);

  useEffect(() => {
    let cancelled = false;

    async function waitForBackend() {
      for (let i = 0; i < 30; i++) {
        if (cancelled) return;
        try {
          await refreshStatus();
          setBackendReady(true);
          setStartupError(null);
          return;
        } catch {
          await new Promise((resolve) => setTimeout(resolve, 1000));
        }
      }
      if (!cancelled) {
        setStartupError("Backend failed to start. Please restart the app.");
        setBackendReady(true);
      }
    }

    waitForBackend();
    return () => {
      cancelled = true;
    };
  }, [refreshStatus]);

  const value = useMemo(
    () => ({
      backendReady,
      status,
      appVersion,
      startupError,
      refreshStatus
    }),
    [appVersion, backendReady, refreshStatus, startupError, status]
  );

  return <ApplicationContext.Provider value={value}>{children}</ApplicationContext.Provider>;
}

export function useApplicationContext() {
  const context = useContext(ApplicationContext);
  if (!context) {
    throw new Error("useApplicationContext must be used inside ApplicationProvider.");
  }
  return context;
}
