#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use std::path::PathBuf;
use std::process::{Child, Command};
use std::sync::Mutex;
use std::thread;
use std::time::Duration;
use tauri::{AppHandle, Manager, RunEvent};

#[cfg(windows)]
use std::os::windows::process::CommandExt;

const CREATE_NO_WINDOW: u32 = 0x08000000;

struct BackendState(Mutex<Option<Child>>);

#[tauri::command]
fn stop_backend(state: tauri::State<BackendState>) {
    let child = state.0.lock().expect("backend lock poisoned").take();
    if let Some(mut child) = child {
        let _ = child.kill();
        let _ = child.wait();
    }
}

fn main() {
    let app = tauri::Builder::default()
        .plugin(tauri_plugin_updater::Builder::new().build())
        .plugin(tauri_plugin_process::init())
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_opener::init())
        .manage(BackendState(Mutex::new(None)))
        .invoke_handler(tauri::generate_handler![stop_backend])
        .setup(|app| {
            if use_external_backend() {
                return Ok(());
            }
            let jar_path = match find_backend_jar(app.handle()) {
                Ok(p) => p,
                Err(e) => {
                    eprintln!("[RO Toolbox] Backend JAR not found: {e}");
                    return Ok(());
                }
            };
            match spawn_backend_with_retry(app.handle(), &jar_path, 5, Duration::from_secs(2)) {
                Ok(child) => {
                    let state = app.state::<BackendState>();
                    *state.0.lock().expect("backend lock poisoned") = Some(child);
                }
                Err(e) => {
                    eprintln!("[RO Toolbox] Failed to start backend: {e}");
                }
            }
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
    app_handle: &AppHandle,
    jar_path: &PathBuf,
    retries: u32,
    delay: Duration,
) -> Result<Child, String> {
    let mut last_err = String::new();
    let bundled_java = find_bundled_java(app_handle);
    let java_label = bundled_java
        .as_ref()
        .map(|p| p.to_string_lossy().to_string())
        .unwrap_or_else(|| "java".to_string());

    for attempt in 0..=retries {
        if attempt > 0 {
            thread::sleep(delay);
        }
        #[allow(unused_mut)]
        let mut cmd = if let Some(ref java_path) = bundled_java {
            Command::new(java_path)
        } else {
            Command::new("java")
        };
        cmd.arg("-jar").arg(jar_path);
        #[cfg(windows)]
        cmd.creation_flags(CREATE_NO_WINDOW);
        match cmd.spawn() {
            Ok(child) => return Ok(child),
            Err(e) => {
                last_err = format!(
                    "Failed to start backend using {} (attempt {}/{}): {}",
                    java_label,
                    attempt + 1,
                    retries + 1,
                    e
                );
            }
        }
    }
    Err(last_err)
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

#[cfg(windows)]
fn bundled_java_bin_name() -> &'static str {
    "javaw.exe"
}

#[cfg(not(windows))]
fn bundled_java_bin_name() -> &'static str {
    "java"
}

fn find_bundled_java(app_handle: &AppHandle) -> Option<PathBuf> {
    let mut candidates = Vec::<PathBuf>::new();

    if let Ok(resource_dir) = app_handle.path().resource_dir() {
        candidates.push(
            resource_dir
                .join("jre")
                .join("bin")
                .join(bundled_java_bin_name()),
        );
    }

    if let Ok(cwd) = std::env::current_dir() {
        candidates.push(
            cwd.join("resources")
                .join("jre")
                .join("bin")
                .join(bundled_java_bin_name()),
        );
        candidates.push(
            cwd.join("src-tauri")
                .join("resources")
                .join("jre")
                .join("bin")
                .join(bundled_java_bin_name()),
        );
    }

    candidates.into_iter().find(|candidate| candidate.exists())
}
