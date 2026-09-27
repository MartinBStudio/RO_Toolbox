import { useEffect, useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { getCurrentWindow } from "@tauri-apps/api/window";
import { exit } from "@tauri-apps/plugin-process";
import { useApplicationContext } from "../context/ApplicationContext.tsx";
import roseLogo from "../assets/rose-logo-bg.webp";

const SLOW_STARTUP_CLOSE_DELAY_MS = 8000;

type BackendReadyGateProps = {
  children: React.ReactNode;
  onStartupError: (message: string) => void;
  showStartupScreen: boolean;
};

export function BackendReadyGate({ children, onStartupError, showStartupScreen }: BackendReadyGateProps) {
  const { startupError } = useApplicationContext();
  const [showCloseButton, setShowCloseButton] = useState(false);

  useEffect(() => {
    if (startupError) {
      onStartupError(startupError);
    }
  }, [onStartupError, startupError]);

  useEffect(() => {
    getCurrentWindow().setDecorations(!showStartupScreen).catch(() => undefined);
  }, [showStartupScreen]);

  useEffect(() => {
    if (!showStartupScreen) {
      setShowCloseButton(false);
      return;
    }

    const timer = window.setTimeout(() => setShowCloseButton(true), SLOW_STARTUP_CLOSE_DELAY_MS);
    return () => window.clearTimeout(timer);
  }, [showStartupScreen]);

  async function closeApp() {
    await invoke("stop_backend").catch(() => undefined);
    await exit(0);
  }

  if (showStartupScreen) {
    return (
      <div className="startingScreen">
        <div className="startingScreenContent">
          <div className="startingScreenBrand">
            <img src={roseLogo} alt="ROSE" className="headerLogo startingScreenLogo" />
            <p className="startingScreenTitle">Toolbox</p>
          </div>
          <span className="loadingSpinner" aria-hidden="true" />
          {showCloseButton ? (
            <button type="button" className="startingScreenClose" onClick={closeApp}>
              Close app
            </button>
          ) : null}
        </div>
      </div>
    );
  }

  return <>{children}</>;
}
