mod auth;
mod download;
mod install;
mod java;
mod launch;
mod meta;
mod modrinth;
mod state;
mod versions;

use auth::Account;
use install::Layout;
use serde::Serialize;
use state::{Accounts, Settings};
use std::path::PathBuf;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{Arc, Mutex};
use tauri::{AppHandle, Emitter, Manager, State};
use tokio::io::{AsyncBufReadExt, BufReader};

struct App {
    layout: Layout,
    client: reqwest::Client,
    settings: Mutex<Settings>,
    accounts: Mutex<Accounts>,
    playing: AtomicBool,
}

impl App {
    /// The Minecraft version to play.
    fn version(&self) -> String {
        self.settings.lock().unwrap().version.clone().unwrap_or_else(|| versions::default_minecraft().to_string())
    }

    fn instance_dir(&self) -> PathBuf {
        self.layout.instance(&self.version())
    }

    /// Brings the selected version's mods folder in line with the shared mod list.
    async fn sync_mods(&self) -> anyhow::Result<modrinth::SyncReport> {
        modrinth::sync(&self.client, &self.layout.root, &self.instance_dir(), &self.version()).await
    }

    fn update_wanted(&self, change: impl FnOnce(&mut Vec<modrinth::WantedMod>)) -> Result<(), String> {
        let mut wanted = modrinth::load_wanted(&self.layout.root);
        change(&mut wanted);
        modrinth::save_wanted(&self.layout.root, &wanted).map_err(|e| e.to_string())
    }

    fn settings_path(&self) -> PathBuf {
        self.layout.root.join("settings.json")
    }

    fn accounts_path(&self) -> PathBuf {
        self.layout.root.join("accounts.json")
    }

    fn save_accounts(&self) -> Result<(), String> {
        state::save(&self.accounts_path(), &*self.accounts.lock().unwrap(), true).map_err(|e| e.to_string())
    }
}

/// What the UI needs to draw itself.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct Info {
    /// Every Minecraft release Lucent Client is built for.
    lucent_versions: Vec<&'static str>,
    /// The version selected to play.
    version: String,
    fabric_loader: &'static str,
    mod_version: &'static str,
    launcher_version: &'static str,
    settings: Settings,
    accounts: Vec<AccountView>,
    selected: Option<String>,
    microsoft_ready: bool,
    development: bool,
    playing: bool,
}

/// An account without its tokens.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct AccountView {
    kind: String,
    name: String,
    uuid: String,
}

impl From<&Account> for AccountView {
    fn from(a: &Account) -> Self {
        Self { kind: a.kind.clone(), name: a.name.clone(), uuid: a.uuid.clone() }
    }
}

#[derive(Serialize, Clone)]
#[serde(rename_all = "camelCase")]
struct LogLine {
    line: String,
    error: bool,
}

#[tauri::command]
fn get_info(app: State<'_, Arc<App>>) -> Info {
    let accounts = app.accounts.lock().unwrap();
    Info {
        lucent_versions: versions::supported(),
        version: app.version(),
        fabric_loader: versions::fabric_loader(),
        mod_version: versions::mod_version(),
        launcher_version: env!("CARGO_PKG_VERSION"),
        settings: app.settings.lock().unwrap().clone(),
        accounts: accounts.accounts.iter().map(AccountView::from).collect(),
        selected: accounts.selected.clone(),
        microsoft_ready: auth::configured(),
        development: cfg!(debug_assertions),
        playing: app.playing.load(Ordering::SeqCst),
    }
}

#[tauri::command]
fn save_settings(app: State<'_, Arc<App>>, settings: Settings) -> Result<(), String> {
    state::save(&app.settings_path(), &settings, false).map_err(|e| e.to_string())?;
    *app.settings.lock().unwrap() = settings;
    Ok(())
}

#[derive(serde::Deserialize)]
struct GameVersion {
    version: String,
    stable: bool,
}

/// Minecraft releases Fabric supports, from 1.19 on (older versions need a different launch setup).
#[tauri::command]
async fn list_versions(app: State<'_, Arc<App>>) -> Result<Vec<String>, String> {
    let all: Vec<GameVersion> = download::get_json(&app.client, "https://meta.fabricmc.net/v2/versions/game")
        .await
        .map_err(|e| e.to_string())?;
    Ok(all
        .into_iter()
        .filter(|v| v.stable)
        .map(|v| v.version)
        .filter(|v| {
            let mut parts = v.split('.').map(|p| p.parse::<u32>().unwrap_or(0));
            match (parts.next(), parts.next()) {
                (Some(1), Some(minor)) => minor >= 19,
                (Some(major), _) => major > 1,
                _ => false,
            }
        })
        .collect())
}

#[tauri::command]
fn set_version(app: State<'_, Arc<App>>, version: String) -> Result<(), String> {
    let mut settings = app.settings.lock().unwrap();
    settings.version = if version == versions::default_minecraft() { None } else { Some(version) };
    state::save(&app.settings_path(), &*settings, false).map_err(|e| e.to_string())
}

#[tauri::command]
async fn search_mods(
    app: State<'_, Arc<App>>,
    query: String,
    offset: u32,
    sort: String,
    categories: Vec<String>,
) -> Result<modrinth::SearchPage, String> {
    modrinth::search(&app.client, &query, &app.version(), offset, &sort, &categories).await.map_err(|e| e.to_string())
}

/// A mod in the shared list, as the UI shows it for the selected version.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct ModView {
    project_id: String,
    title: String,
    icon_url: Option<String>,
    enabled: bool,
    /// No file for the selected version, so it is left out when playing it.
    missing: bool,
}

#[tauri::command]
fn installed_mods(app: State<'_, Arc<App>>) -> Vec<ModView> {
    let record = modrinth::load_record(&app.instance_dir());
    modrinth::load_wanted(&app.layout.root)
        .into_iter()
        .map(|w| ModView {
            missing: record.missing.contains(&w.project_id),
            project_id: w.project_id,
            title: w.title,
            icon_url: w.icon_url,
            enabled: w.enabled,
        })
        .collect()
}

/// Adds a mod to the shared list and installs it (with what it needs) for the selected version.
#[tauri::command]
async fn install_mod(app: State<'_, Arc<App>>, project_id: String) -> Result<modrinth::SyncReport, String> {
    modrinth::add(&app.client, &app.layout.root, &project_id).await.map_err(|e| format!("{e:#}"))?;
    app.sync_mods().await.map_err(|e| format!("{e:#}"))
}

#[tauri::command]
async fn remove_mod(app: State<'_, Arc<App>>, project_id: String) -> Result<modrinth::SyncReport, String> {
    app.update_wanted(|wanted| wanted.retain(|w| w.project_id != project_id))?;
    app.sync_mods().await.map_err(|e| format!("{e:#}"))
}

#[tauri::command]
async fn set_mod_enabled(app: State<'_, Arc<App>>, project_id: String, enabled: bool) -> Result<modrinth::SyncReport, String> {
    app.update_wanted(|wanted| {
        if let Some(w) = wanted.iter_mut().find(|w| w.project_id == project_id) {
            w.enabled = enabled;
        }
    })?;
    app.sync_mods().await.map_err(|e| format!("{e:#}"))
}

#[tauri::command]
fn select_account(app: State<'_, Arc<App>>, uuid: String) -> Result<(), String> {
    app.accounts.lock().unwrap().selected = Some(uuid);
    app.save_accounts()
}

#[tauri::command]
fn remove_account(app: State<'_, Arc<App>>, uuid: String) -> Result<(), String> {
    app.accounts.lock().unwrap().remove(&uuid);
    app.save_accounts()
}

/// Development builds only: add an offline account to test installing and launching.
#[tauri::command]
fn add_offline_account(app: State<'_, Arc<App>>, name: String) -> Result<(), String> {
    if !cfg!(debug_assertions) {
        return Err("Offline accounts are only available in development builds".into());
    }
    let valid = (3..=16).contains(&name.len()) && name.chars().all(|c| c.is_ascii_alphanumeric() || c == '_');
    if !valid {
        return Err("Use 3-16 letters, numbers or underscores".into());
    }
    app.accounts.lock().unwrap().upsert(auth::offline(&name));
    app.save_accounts()
}

/// Starts Microsoft sign-in. Emits `auth-code` with the code to enter, then `auth-done` or `auth-error`.
#[tauri::command]
async fn sign_in(handle: AppHandle, app: State<'_, Arc<App>>) -> Result<(), String> {
    let app = app.inner().clone();
    let code = auth::start_device_code(&app.client).await.map_err(|e| e.to_string())?;
    handle.emit("auth-code", &code).ok();
    tauri::async_runtime::spawn(async move {
        match auth::finish_device_code(&app.client, &code).await {
            Ok(account) => {
                app.accounts.lock().unwrap().upsert(account);
                let _ = app.save_accounts();
                handle.emit("auth-done", ()).ok();
            }
            Err(error) => {
                handle.emit("auth-error", error.to_string()).ok();
            }
        }
    });
    Ok(())
}

/// Installs whatever is missing and starts the game. Progress arrives as `install-progress`, game
/// output as `game-log`, and the end as `game-exit` (exit code) or `play-error` (message).
#[tauri::command]
async fn play(handle: AppHandle, app: State<'_, Arc<App>>) -> Result<(), String> {
    let app = app.inner().clone();
    if app.playing.swap(true, Ordering::SeqCst) {
        return Err("The game is already running".into());
    }
    tauri::async_runtime::spawn(async move {
        let result = run_game(&handle, &app).await;
        app.playing.store(false, Ordering::SeqCst);
        if let Some(window) = handle.get_webview_window("main") {
            let _ = window.show();
        }
        if let Err(error) = result {
            handle.emit("play-error", format!("{error:#}")).ok();
        }
    });
    Ok(())
}

async fn run_game(handle: &AppHandle, app: &Arc<App>) -> anyhow::Result<()> {
    let mut account = app.accounts.lock().unwrap().selected().cloned().ok_or_else(|| anyhow::anyhow!("Sign in first"))?;
    if account.needs_refresh() {
        account = auth::refresh(&app.client, &account).await?;
        app.accounts.lock().unwrap().upsert(account.clone());
        app.save_accounts().map_err(anyhow::Error::msg)?;
    }
    let installed = install::install(&app.client, &app.layout, &app.version(), |progress| {
        handle.emit("install-progress", progress).ok();
    })
    .await?;

    // Fetch the player's mods for this version; without a connection, play with what is there.
    match app.sync_mods().await {
        Ok(report) if !report.missing.is_empty() => {
            handle.emit("mods-skipped", report.missing).ok();
        }
        Ok(_) => {}
        Err(error) => {
            handle.emit("mods-warning", format!("{error:#}")).ok();
        }
    }

    let settings = app.settings.lock().unwrap().clone();
    let mut child = launch::command(&app.layout, &installed, &account, &settings).spawn()?;
    handle.emit("game-started", ()).ok();
    if settings.hide_on_launch {
        if let Some(window) = handle.get_webview_window("main") {
            let _ = window.hide();
        }
    }
    for (stream, error) in [(child.stdout.take().map(|s| Box::new(s) as Box<dyn tokio::io::AsyncRead + Unpin + Send>), false),
        (child.stderr.take().map(|s| Box::new(s) as Box<dyn tokio::io::AsyncRead + Unpin + Send>), true)]
    {
        let Some(stream) = stream else { continue };
        let handle = handle.clone();
        tauri::async_runtime::spawn(async move {
            let mut lines = BufReader::new(stream).lines();
            while let Ok(Some(line)) = lines.next_line().await {
                handle.emit("game-log", LogLine { line, error }).ok();
            }
        });
    }
    let status = child.wait().await?;
    handle.emit("game-exit", status.code()).ok();
    Ok(())
}

#[tauri::command]
fn open_game_directory(handle: AppHandle, app: State<'_, Arc<App>>) -> Result<(), String> {
    use tauri_plugin_opener::OpenerExt;
    let dir = app.layout.instance(&app.version());
    std::fs::create_dir_all(&dir).map_err(|e| e.to_string())?;
    handle.opener().open_path(dir.to_string_lossy(), None::<&str>).map_err(|e| e.to_string())
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_opener::init())
        .setup(|tauri_app| {
            let root = tauri_app.path().app_data_dir()?;
            std::fs::create_dir_all(&root)?;
            let layout = Layout { root };
            // Before per-version instances, the game directory was data/game; it belongs to the
            // version Lucent Client was built for.
            let legacy = layout.root.join("game");
            let instance = layout.instance(versions::default_minecraft());
            if legacy.exists() && !instance.exists() {
                std::fs::create_dir_all(instance.parent().unwrap())?;
                std::fs::rename(&legacy, &instance)?;
            }
            // Mods used to be installed per version; they now share one list.
            modrinth::migrate(&layout.root, &layout.root.join("instances"))?;
            let settings: Settings = state::load(&layout.root.join("settings.json"));
            let accounts: Accounts = state::load(&layout.root.join("accounts.json"));
            tauri_app.manage(Arc::new(App {
                layout,
                client: download::client(),
                settings: Mutex::new(settings),
                accounts: Mutex::new(accounts),
                playing: AtomicBool::new(false),
            }));
            Ok(())
        })
        .invoke_handler(tauri::generate_handler![
            get_info,
            save_settings,
            select_account,
            remove_account,
            add_offline_account,
            sign_in,
            play,
            open_game_directory,
            list_versions,
            set_version,
            search_mods,
            installed_mods,
            install_mod,
            remove_mod,
            set_mod_enabled
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}

#[cfg(test)]
mod tests {
    use super::*;

    /// Downloads everything into LUCENT_TEST_DIR and prints the java command without running it.
    /// Run with: LUCENT_TEST_DIR=/some/dir cargo test install_and_build_command -- --ignored --nocapture
    #[tokio::test]
    #[ignore]
    async fn install_and_build_command() {
        let root = PathBuf::from(std::env::var("LUCENT_TEST_DIR").expect("set LUCENT_TEST_DIR"));
        let layout = Layout { root };
        let client = download::client();
        let version = std::env::var("LUCENT_TEST_VERSION").unwrap_or_else(|_| versions::default_minecraft().to_string());
        let installed = install::install(&client, &layout, &version, |p| {
            if p.done == p.total {
                println!("{} {}/{}", p.stage, p.done, p.total);
            }
        })
        .await
        .expect("install");
        assert!(installed.java.exists(), "java missing at {}", installed.java.display());
        let command = launch::command(&layout, &installed, &auth::offline("Tester"), &Settings::default());
        println!("{:?}", command.as_std());
    }

    /// Adds a mod to the shared list in LUCENT_TEST_DIR and syncs it into one version's instance.
    #[tokio::test]
    #[ignore]
    async fn sync_modrinth_mod() {
        let root = PathBuf::from(std::env::var("LUCENT_TEST_DIR").expect("set LUCENT_TEST_DIR"));
        let version = std::env::var("LUCENT_TEST_VERSION").unwrap_or_else(|_| versions::default_minecraft().to_string());
        let project = std::env::var("LUCENT_TEST_MOD").unwrap_or_else(|_| "AANobbMI".into());
        let client = download::client();
        modrinth::add(&client, &root, &project).await.expect("add");
        let instance = Layout { root: root.clone() }.instance(&version);
        let report = modrinth::sync(&client, &root, &instance, &version).await.expect("sync");
        println!("missing for {version}: {:?}", report.missing);
        for file in modrinth::load_record(&instance).files {
            println!("{version}: {} dependency={}", file.filename, file.dependency);
        }
    }
}
