import { useEffect, useRef } from "react";
import { LogicalPosition, LogicalSize, currentMonitor, getCurrentWindow } from "@tauri-apps/api/window";

const DEFAULT_WINDOW_WIDTH = 900;
const DEFAULT_WINDOW_HEIGHT = 520;
const DEFAULT_MIN_WINDOW_WIDTH = 860;
const DEFAULT_MIN_WINDOW_HEIGHT = 420;
const QUICK_MODE_WINDOW_WIDTH = 350;
const QUICK_MODE_WINDOW_HEIGHT = 70;
const QUICK_MODE_MIN_WINDOW_WIDTH = 320;
const QUICK_MODE_MIN_WINDOW_HEIGHT = 100;
const WINDOW_EDGE_OFFSET = 35;

type WindowMode = "small" | "big";

export function useWindowMode(enabled: boolean, quickModeActive: boolean) {
  const appliedWindowModeRef = useRef<WindowMode | null>(null);

  useEffect(() => {
    if (!enabled) {
      return;
    }

    const appWindow = getCurrentWindow();
    if (quickModeActive) {
      if (appliedWindowModeRef.current === "small") {
        return;
      }
      (async () => {
        const fullscreen = await appWindow.isFullscreen();
        if (fullscreen) {
          await appWindow.setFullscreen(false);
        }
        const maximized = await appWindow.isMaximized();
        if (maximized) {
          await appWindow.unmaximize();
        }
        await appWindow.setResizable(true);
        await appWindow.setMaxSize(null);
        await appWindow.setMinSize(new LogicalSize(QUICK_MODE_MIN_WINDOW_WIDTH, QUICK_MODE_MIN_WINDOW_HEIGHT));
        await appWindow.setSize(new LogicalSize(QUICK_MODE_WINDOW_WIDTH, QUICK_MODE_WINDOW_HEIGHT));

        const monitor = await currentMonitor();
        if (monitor) {
          const workAreaPosition = monitor.workArea.position.toLogical(monitor.scaleFactor);
          const workAreaSize = monitor.workArea.size.toLogical(monitor.scaleFactor);
          const x = Math.max(
            workAreaPosition.x,
            workAreaPosition.x + workAreaSize.width - QUICK_MODE_WINDOW_WIDTH - 10
          );
          const y = Math.max(
            workAreaPosition.y,
            workAreaPosition.y + workAreaSize.height - QUICK_MODE_WINDOW_HEIGHT - WINDOW_EDGE_OFFSET
          );
          await appWindow.setPosition(new LogicalPosition(x, y));
        }

        await new Promise((resolve) => window.setTimeout(resolve, 30));
        await appWindow.setSize(new LogicalSize(QUICK_MODE_WINDOW_WIDTH, QUICK_MODE_WINDOW_HEIGHT));
        await appWindow.setMaxSize(new LogicalSize(QUICK_MODE_WINDOW_WIDTH, QUICK_MODE_WINDOW_HEIGHT));
        await appWindow.setResizable(false);
        appliedWindowModeRef.current = "small";
      })().catch(() => undefined);
      return;
    }

    if (appliedWindowModeRef.current === "big") {
      return;
    }
    (async () => {
      await appWindow.setResizable(true);
      await appWindow.setMaxSize(null);
      await appWindow.setMinSize(new LogicalSize(DEFAULT_MIN_WINDOW_WIDTH, DEFAULT_MIN_WINDOW_HEIGHT));
      await appWindow.setSize(new LogicalSize(DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT));

      const monitor = await currentMonitor();
      if (monitor) {
        const workAreaPosition = monitor.workArea.position.toLogical(monitor.scaleFactor);
        const workAreaSize = monitor.workArea.size.toLogical(monitor.scaleFactor);
        const x = workAreaPosition.x + Math.round((workAreaSize.width - DEFAULT_WINDOW_WIDTH) / 2);
        const y = workAreaPosition.y + Math.round((workAreaSize.height - DEFAULT_WINDOW_HEIGHT) / 2);
        await appWindow.setPosition(new LogicalPosition(x, y));
      }

      appliedWindowModeRef.current = "big";
    })().catch(() => undefined);
  }, [enabled, quickModeActive]);
}
