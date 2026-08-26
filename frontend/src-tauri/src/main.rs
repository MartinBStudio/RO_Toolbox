#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use std::path::{Path, PathBuf};
use std::process::{Child, Command};
use std::sync::Mutex;
use std::thread;
use std::time::Duration;
use tauri::{AppHandle, Manager, RunEvent};

#[cfg(windows)]
use std::os::windows::process::CommandExt;

const CREATE_NO_WINDOW: u32 = 0x08000000;

struct BackendState(Mutex<Option<Child>>);

fn main() {
    let app = tauri::Builder::default()
        .plugin(tauri_plugin_updater::Builder::new().build())
        .plugin(tauri_plugin_process::init())
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_opener::init())
        .manage(BackendState(Mutex::new(None)))
        .setup(|app| {
            if use_external_backend() {
                return Ok(());
            }
            let jar_path = find_backend_jar(app.handle())?;
            let java_bin = find_java_bin(app.handle());
            let child = spawn_backend_with_retry(&java_bin, &jar_path, 5, Duration::from_secs(2))?;
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

fn spawn_backend_with_retry(
    java_bin: &Path,
    jar_path: &Path,
    retries: u32,
    delay: Duration,
) -> Result<Child, String> {
    let mut last_err = String::new();
    for attempt in 0..=retries {
        if attempt > 0 {
            thread::sleep(delay);
        }
        #[allow(unused_mut)]
        let mut cmd = Command::new(java_bin);
        cmd.arg("-jar").arg(jar_path);
        #[cfg(windows)]
        cmd.creation_flags(CREATE_NO_WINDOW);
        match cmd.spawn() {
            Ok(child) => return Ok(child),
            Err(e) => {
                last_err = format!(
                    "Failed to start backend (attempt {}/{}): {}",
                    attempt + 1,
                    retries + 1,
                    e
                );
            }
        }
    }
    Err(last_err)
}

fn find_java_bin(app_handle: &AppHandle) -> PathBuf {
    let mut candidates = Vec::<PathBuf>::new();

    if let Ok(resource_dir) = app_handle.path().resource_dir() {
        candidates.push(
            resource_dir
                .join("jre")
                .join("bin")
                .join(bundled_java_executable_name()),
        );
    }

    if cfg!(debug_assertions) {
        if let Ok(cwd) = std::env::current_dir() {
            candidates.push(
                cwd.join("resources")
                    .join("jre")
                    .join("bin")
                    .join(bundled_java_executable_name()),
            );
        }
    }

    candidates
        .into_iter()
        .find(|candidate| candidate.exists())
        .unwrap_or_else(|| PathBuf::from("java"))
}

#[cfg(windows)]
fn bundled_java_executable_name() -> &'static str {
    "java.exe"
}

#[cfg(not(windows))]
fn bundled_java_executable_name() -> &'static str {
    "java"
}

fn use_external_backend() -> bool {
    matches!(
        std::env::var("RO_TOOLBOX_EXTERNAL_BACKEND").ok().as_deref(),
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
        candidates.push(
            cwd.join("..")
                .join("build")
                .join("libs")
                .join("RO_Toolbox.jar"),
        );
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
