import { useEffect } from "react";
import { getCurrentWindow } from "@tauri-apps/api/window";
import { useApplicationContext } from "../context/ApplicationContext.tsx";
import roseLogo from "../assets/rose-logo-bg.webp";

type BackendReadyGateProps = {
  children: React.ReactNode;
  onStartupError: (message: string) => void;
  showStartupScreen: boolean;
};

export function BackendReadyGate({ children, onStartupError, showStartupScreen }: BackendReadyGateProps) {
  const { startupError } = useApplicationContext();

  useEffect(() => {
    if (startupError) {
      onStartupError(startupError);
    }
  }, [onStartupError, startupError]);

  useEffect(() => {
    getCurrentWindow().setDecorations(!showStartupScreen).catch(() => undefined);
  }, [showStartupScreen]);

  if (showStartupScreen) {
    return (
      <div className="startingScreen">
        <div className="startingScreenContent">
          <div className="startingScreenBrand">
            <img src={roseLogo} alt="ROSE" className="headerLogo startingScreenLogo" />
            <p className="startingScreenTitle">Toolbox</p>
          </div>
          <span className="loadingSpinner" aria-hidden="true" />
        </div>
      </div>
    );
  }

  return <>{children}</>;
}
