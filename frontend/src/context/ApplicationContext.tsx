import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { getVersion as getAppVersion } from "@tauri-apps/api/app";
import { getStatus } from "../backendConnector/api.ts";
import type { AppStatus } from "../types";

const DEBUG_MODE_STORAGE_KEY = "roToolbox.debugMode";

type ApplicationContextValue = {
  backendReady: boolean;
  status: AppStatus | null;
  appVersion: string | null;
  startupError: string | null;
  debugMode: boolean;
  setDebugMode: (enabled: boolean) => void;
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
  const [debugMode, setDebugMode] = useState(false);

  const refreshStatus = useCallback(async () => {
    setStatus(await getStatus());
  }, []);

  useEffect(() => {
    try {
      setDebugMode(window.localStorage.getItem(DEBUG_MODE_STORAGE_KEY) === "true");
    } catch {
      setDebugMode(false);
    }
  }, []);

  useEffect(() => {
    try {
      window.localStorage.setItem(DEBUG_MODE_STORAGE_KEY, String(debugMode));
    } catch {
      // Ignore storage failures and keep the in-memory toggle state.
    }
  }, [debugMode]);

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
      debugMode,
      setDebugMode,
      refreshStatus
    }),
    [appVersion, backendReady, debugMode, refreshStatus, startupError, status]
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
