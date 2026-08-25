#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use std::path::PathBuf;
use std::process::{Child, Command};
use std::sync::Mutex;
use tauri::{AppHandle, Manager, RunEvent};

#[cfg(windows)]
use std::os::windows::process::CommandExt;

const CREATE_NO_WINDOW: u32 = 0x08000000;

struct BackendState(Mutex<Option<Child>>);

fn main() {
    let app = tauri::Builder::default()
        .plugin(tauri_plugin_updater::Builder::new().build())
        .plugin(tauri_plugin_process::init())
        .manage(BackendState(Mutex::new(None)))
        .setup(|app| {
            if use_external_backend() {
                return Ok(());
            }
            let jar_path = find_backend_jar(app.handle())?;
            #[allow(unused_mut)]
            let mut cmd = Command::new("java");
            cmd.arg("-jar").arg(&jar_path);
            #[cfg(windows)]
            cmd.creation_flags(CREATE_NO_WINDOW);
            let child = cmd
                .spawn()
                .map_err(|e| format!("Failed to start backend with {:?}: {}", jar_path, e))?;
            let state = app.state::<BackendState>();
            *state.0.lock().expect("backend lock poisoned") = Some(child);
            Ok(())
        })
        .build(tauri::generate_context!())
        .expect("error while running tauri application");

    app.run(|app_handle, event| {
        if matches!(event, RunEvent::Exit | RunEvent::ExitRequested { .. }) {
            let state = app_handle.state::<BackendState>();
            let child = state.0.lock().expect("backend lock poisoned").take();
            if let Some(mut child) = child {
                let _ = child.kill();
            }
        }
    });
}

fn use_external_backend() -> bool {
    matches!(
        std::env::var("RO_TOOLBOX_EXTERNAL_BACKEND")
            .ok()
            .as_deref(),
        Some("1" | "true" | "TRUE" | "True")
    )
}

fn find_backend_jar(app_handle: &AppHandle) -> Result<PathBuf, String> {
    let mut candidates = Vec::<PathBuf>::new();

    if let Ok(resource_dir) = app_handle.path().resource_dir() {
        candidates.push(resource_dir.join("RO_Toolbox.jar"));
    }

    if let Ok(cwd) = std::env::current_dir() {
        candidates.push(cwd.join("resources").join("RO_Toolbox.jar"));
        candidates.push(cwd.join("..").join("build").join("libs").join("RO_Toolbox.jar"));
        candidates.push(
            cwd.join("..")
                .join("..")
                .join("build")
                .join("libs")
                .join("RO_Toolbox.jar"),
        );
    }

    for candidate in candidates {
        if candidate.exists() {
            return Ok(candidate);
        }
    }

    Err("RO_Toolbox.jar not found. Build backend jar and copy it to frontend/src-tauri/resources first.".to_string())
}
