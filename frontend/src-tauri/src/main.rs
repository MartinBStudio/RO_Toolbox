#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use serde::Deserialize;
use std::fs;
use std::path::PathBuf;
use std::process::{Child, Command};
use std::sync::Mutex;
use std::thread;
use std::time::Duration;
use tauri::{AppHandle, Manager, RunEvent};

#[cfg(windows)]
use std::os::windows::process::CommandExt;

#[cfg(windows)]
use windows::core::{Interface, GUID, HSTRING};
#[cfg(windows)]
use windows::Win32::Foundation::PROPERTYKEY;
#[cfg(windows)]
use windows::Win32::System::Com::StructuredStorage::PROPVARIANT;
#[cfg(windows)]
use windows::Win32::System::Com::{
    CoCreateInstance, CoInitializeEx, CoUninitialize, CLSCTX_INPROC_SERVER,
    COINIT_APARTMENTTHREADED,
};
#[cfg(windows)]
use windows::Win32::UI::Shell::Common::{IObjectArray, IObjectCollection};
#[cfg(windows)]
use windows::Win32::UI::Shell::PropertiesSystem::IPropertyStore;
#[cfg(windows)]
use windows::Win32::UI::Shell::{
    DestinationList, EnumerableObjectCollection, ICustomDestinationList, IShellLinkW, ShellLink,
};

const CREATE_NO_WINDOW: u32 = 0x08000000;
const BACKEND_API_BASE: &str = "http://localhost:8080/api";
#[cfg(windows)]
const PKEY_TITLE: PROPERTYKEY = PROPERTYKEY {
    fmtid: GUID::from_u128(0xf29f85e0_4ff9_1068_ab91_08002b27b3d9),
    pid: 2,
};

struct BackendState(Mutex<Option<Child>>);

#[derive(Deserialize)]
struct TaskbarQuickAccount {
    id: String,
    name: String,
    icon: String,
}

#[tauri::command]
fn stop_backend(state: tauri::State<BackendState>) {
    stop_backend_child(&state);
}

#[tauri::command]
fn sync_taskbar_quick_launch(
    app_handle: AppHandle,
    accounts: Vec<TaskbarQuickAccount>,
) -> Result<(), String> {
    #[cfg(windows)]
    {
        return sync_taskbar_quick_launch_windows(&app_handle, &accounts);
    }

    #[cfg(not(windows))]
    {
        let _ = app_handle;
        let _ = accounts;
        Ok(())
    }
}

fn main() {
    let app = tauri::Builder::default()
        .plugin(tauri_plugin_updater::Builder::new().build())
        .plugin(tauri_plugin_process::init())
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_opener::init())
        .manage(BackendState(Mutex::new(None)))
        .invoke_handler(tauri::generate_handler![
            stop_backend,
            sync_taskbar_quick_launch
        ])
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
            stop_backend_child(&state);
        }
    });
}

fn stop_backend_child(state: &BackendState) {
    let child = state.0.lock().expect("backend lock poisoned").take();
    if let Some(child) = child {
        terminate_backend_process(child);
    }
}

fn terminate_backend_process(mut child: Child) {
    if matches!(child.try_wait(), Ok(Some(_))) {
        return;
    }

    #[cfg(windows)]
    terminate_process_tree_windows(child.id());

    let _ = child.kill();

    let deadline = std::time::Instant::now() + Duration::from_secs(3);
    loop {
        match child.try_wait() {
            Ok(Some(_)) => break,
            Ok(None) => {
                if std::time::Instant::now() >= deadline {
                    break;
                }
                thread::sleep(Duration::from_millis(100));
            }
            Err(_) => break,
        }
    }

    let _ = child.wait();
}

#[cfg(windows)]
fn terminate_process_tree_windows(pid: u32) {
    #[allow(unused_mut)]
    let mut command = Command::new("taskkill");
    command.args(["/PID", &pid.to_string(), "/T", "/F"]);
    command.creation_flags(CREATE_NO_WINDOW);

    if let Err(err) = command.status() {
        eprintln!(
            "[RO Toolbox] Failed to terminate backend process tree (PID {}): {}",
            pid, err
        );
    }
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

#[cfg(windows)]
fn sync_taskbar_quick_launch_windows(
    app_handle: &AppHandle,
    accounts: &[TaskbarQuickAccount],
) -> Result<(), String> {
    let mut tasks = Vec::new();

    for account in accounts {
        let name = account.name.trim();
        if name.is_empty() {
            continue;
        }
        tasks.push((
            name.to_string(),
            format!("{BACKEND_API_BASE}/login/{}/launch", account.id),
            resolve_taskbar_icon_path(app_handle, &account.icon),
        ));
    }

    update_windows_jump_list(tasks)
}

#[cfg(windows)]
fn update_windows_jump_list(tasks: Vec<(String, String, Option<PathBuf>)>) -> Result<(), String> {
    unsafe {
        CoInitializeEx(None, COINIT_APARTMENTTHREADED)
            .ok()
            .map_err(|err| format!("Failed to initialize COM for taskbar menu: {err}"))?;
    }

    let result = update_windows_jump_list_inner(tasks);

    unsafe {
        CoUninitialize();
    }

    result
}

#[cfg(windows)]
fn update_windows_jump_list_inner(
    tasks: Vec<(String, String, Option<PathBuf>)>,
) -> Result<(), String> {
    let destination_list: ICustomDestinationList = unsafe {
        CoCreateInstance(&DestinationList, None, CLSCTX_INPROC_SERVER)
            .map_err(|err| format!("Failed to create Windows jump list: {err}"))?
    };

    let mut max_slots = 0_u32;
    let _removed_destinations: IObjectArray = unsafe {
        destination_list
            .BeginList(&mut max_slots)
            .map_err(|err| format!("Failed to initialize taskbar menu: {err}"))?
    };

    if tasks.is_empty() {
        unsafe {
            destination_list
                .CommitList()
                .map_err(|err| format!("Failed to clear taskbar quick launch group: {err}"))?;
        }
        return Ok(());
    }

    let quick_launch_collection: IObjectCollection = unsafe {
        CoCreateInstance(&EnumerableObjectCollection, None, CLSCTX_INPROC_SERVER)
            .map_err(|err| format!("Failed to create taskbar menu collection: {err}"))?
    };

    let wscript_path = HSTRING::from(r"C:\Windows\System32\wscript.exe");
    let launcher_script = ensure_taskbar_launcher_script()?;
    let launcher_script_path = launcher_script.to_string_lossy().to_string();
    let app_exe = std::env::current_exe()
        .map_err(|err| format!("Failed to resolve app executable path: {err}"))?;

    let max_entries = max_slots.max(1) as usize;
    for (title, endpoint, icon_path) in tasks.into_iter().take(max_entries) {
        let task_item: IShellLinkW = unsafe {
            CoCreateInstance(&ShellLink, None, CLSCTX_INPROC_SERVER)
                .map_err(|err| format!("Failed to create taskbar menu item: {err}"))?
        };

        let script_args = HSTRING::from(format!(
            "//B //Nologo \"{}\" \"{}\"",
            escape_shell_link_argument(&launcher_script_path),
            escape_shell_link_argument(&endpoint)
        ));

        unsafe {
            task_item
                .SetPath(&wscript_path)
                .map_err(|err| format!("Failed to set taskbar command path: {err}"))?;
            task_item
                .SetArguments(&script_args)
                .map_err(|err| format!("Failed to set taskbar command arguments: {err}"))?;
            task_item
                .SetDescription(&HSTRING::from(title.clone()))
                .map_err(|err| format!("Failed to set taskbar item description: {err}"))?;
            task_item
                .SetIconLocation(
                    &HSTRING::from(
                        icon_path
                            .unwrap_or_else(|| app_exe.clone())
                            .to_string_lossy()
                            .to_string(),
                    ),
                    0,
                )
                .map_err(|err| format!("Failed to set taskbar item icon: {err}"))?;
        }

        set_shell_link_title(&task_item, &title)?;

        unsafe {
            quick_launch_collection
                .AddObject(&task_item)
                .map_err(|err| format!("Failed to add taskbar menu item: {err}"))?;
        }
    }

    let object_array: IObjectArray = quick_launch_collection
        .cast()
        .map_err(|err| format!("Failed to finalize taskbar items: {err}"))?;

    unsafe {
        destination_list
            .AppendCategory(&HSTRING::from("Quick launch"), &object_array)
            .map_err(|err| format!("Failed to update taskbar quick launch group: {err}"))?;
        destination_list
            .CommitList()
            .map_err(|err| format!("Failed to commit taskbar menu: {err}"))?;
    }

    Ok(())
}

#[cfg(windows)]
fn set_shell_link_title(shell_link: &IShellLinkW, title: &str) -> Result<(), String> {
    let property_store: IPropertyStore = shell_link
        .cast()
        .map_err(|err| format!("Failed to open taskbar item properties: {err}"))?;
    let title_variant = PROPVARIANT::from(title);

    unsafe {
        property_store
            .SetValue(&PKEY_TITLE, &title_variant)
            .map_err(|err| format!("Failed to set taskbar item title: {err}"))?;
        property_store
            .Commit()
            .map_err(|err| format!("Failed to save taskbar item title: {err}"))?;
    }

    Ok(())
}

fn taskbar_icon_file_name(icon: &str) -> &'static str {
    match icon.trim() {
        "✚" => "healer.ico",
        "🪙" => "coin.ico",
        "🛡️" => "shield.ico",
        "🪄" => "wand.ico",
        "🏹" => "bow.ico",
        "⚔️" => "swords.ico",
        "💎" => "gem.ico",
        "✦" => "sparkle.ico",
        "⚑" => "flag.ico",
        _ => "person.ico",
    }
}

fn resolve_taskbar_icon_path(app_handle: &AppHandle, icon: &str) -> Option<PathBuf> {
    let file_name = taskbar_icon_file_name(icon);
    let mut candidates = Vec::<PathBuf>::new();

    if let Ok(resource_dir) = app_handle.path().resource_dir() {
        candidates.push(resource_dir.join("taskbar-icons").join(file_name));
    }

    if let Ok(cwd) = std::env::current_dir() {
        candidates.push(cwd.join("resources").join("taskbar-icons").join(file_name));
        candidates.push(
            cwd.join("src-tauri")
                .join("resources")
                .join("taskbar-icons")
                .join(file_name),
        );
        candidates.push(
            cwd.join("..")
                .join("src-tauri")
                .join("resources")
                .join("taskbar-icons")
                .join(file_name),
        );
    }

    candidates.into_iter().find(|candidate| candidate.exists())
}

#[cfg(windows)]
fn ensure_taskbar_launcher_script() -> Result<PathBuf, String> {
    let script_path = std::env::temp_dir().join("ro_toolbox_taskbar_launch.vbs");
    let script = "On Error Resume Next\r\nDim url\r\nurl = WScript.Arguments.Item(0)\r\nSet http = CreateObject(\"MSXML2.XMLHTTP\")\r\nhttp.open \"POST\", url, False\r\nhttp.setRequestHeader \"Content-Type\", \"application/json\"\r\nhttp.setRequestHeader \"X-RO-Toolbox-Source\", \"taskbar\"\r\nhttp.send \"\"\r\n";

    fs::write(&script_path, script)
        .map_err(|err| format!("Failed to prepare taskbar launcher script: {err}"))?;
    Ok(script_path)
}

#[cfg(test)]
mod tests {
    use super::taskbar_icon_file_name;

    #[test]
    fn maps_login_icons_to_taskbar_icon_files() {
        assert_eq!(taskbar_icon_file_name("👤"), "person.ico");
        assert_eq!(taskbar_icon_file_name("✚"), "healer.ico");
        assert_eq!(taskbar_icon_file_name("🪙"), "coin.ico");
        assert_eq!(taskbar_icon_file_name("🛡️"), "shield.ico");
        assert_eq!(taskbar_icon_file_name("🪄"), "wand.ico");
        assert_eq!(taskbar_icon_file_name("🏹"), "bow.ico");
        assert_eq!(taskbar_icon_file_name("⚔️"), "swords.ico");
        assert_eq!(taskbar_icon_file_name("💎"), "gem.ico");
        assert_eq!(taskbar_icon_file_name("✦"), "sparkle.ico");
        assert_eq!(taskbar_icon_file_name("⚑"), "flag.ico");
    }

    #[test]
    fn falls_back_to_default_taskbar_icon_file() {
        assert_eq!(taskbar_icon_file_name("unknown"), "person.ico");
        assert_eq!(taskbar_icon_file_name(""), "person.ico");
    }
}

#[cfg(windows)]
fn escape_shell_link_argument(value: &str) -> String {
    value.replace('"', "\"\"")
}
