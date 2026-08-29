/// <reference types="node" />
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
// Suppress EBUSY errors caused by Vite watching Rust build artifacts on Windows
process.on("uncaughtException", function (error) {
    if (error.code === "EBUSY") {
        console.warn("[vite] Ignoring EBUSY watcher error on:", error.path);
        return;
    }
    throw error;
});
export default defineConfig({
    plugins: [react()],
    server: {
        port: 5173,
        watch: {
            ignored: /src-tauri/
        }
    }
});
