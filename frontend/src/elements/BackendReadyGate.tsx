import { useEffect } from "react";
import { useApplicationContext } from "../context/ApplicationContext.tsx";

type BackendReadyGateProps = {
  children: React.ReactNode;
  onStartupError: (message: string) => void;
};

export function BackendReadyGate({ children, onStartupError }: BackendReadyGateProps) {
  const { backendReady, startupError } = useApplicationContext();

  useEffect(() => {
    if (startupError) {
      onStartupError(startupError);
    }
  }, [onStartupError, startupError]);

  if (!backendReady) {
    return (
      <div className="startingScreen">
        <p>Starting RO Toolbox…</p>
      </div>
    );
  }

  return <>{children}</>;
}
