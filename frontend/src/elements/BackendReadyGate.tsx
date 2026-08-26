import { useEffect, type ReactNode } from "react";
import { useApplicationContext } from "../context/ApplicationContext.tsx";

type BackendReadyGateProps = {
    children: ReactNode;
    onStartupError?: (message: string) => void;
};

export function BackendReadyGate({ children, onStartupError }: BackendReadyGateProps) {
    const { backendReady, startupError } = useApplicationContext();

    useEffect(() => {
        if (startupError) {
            onStartupError?.(startupError);
        }
    }, [startupError, onStartupError]);

    if (startupError) {
        return (
            <div className="startupError">
                <p>⚠ {startupError}</p>
            </div>
        );
    }

    if (!backendReady) {
        return (
            <div className="startupLoading">
                <p>Starting backend…</p>
            </div>
        );
    }

    return <>{children}</>;
}
