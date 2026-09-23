//! Builds the java command line from the version JSON and starts the game.

use crate::auth::{self, Account};
use crate::install::{Installed, Layout};
use crate::meta;
use crate::state::Settings;
use serde_json::Value;
use std::collections::HashMap;
use std::path::PathBuf;
use tokio::process::Command;

/// Expands one entry of `arguments.game`/`arguments.jvm`: a plain string, or a rule-guarded value.
fn expand(argument: &Value, out: &mut Vec<String>) {
    match argument {
        Value::String(s) => out.push(s.clone()),
        Value::Object(object) => {
            let rules: Option<Vec<meta::Rule>> = object.get("rules").and_then(|r| serde_json::from_value(r.clone()).ok());
            if !meta::rules_allow(rules.as_deref()) {
                return;
            }
            match object.get("value") {
                Some(Value::String(s)) => out.push(s.clone()),
                Some(Value::Array(values)) => out.extend(values.iter().filter_map(|v| v.as_str().map(str::to_string))),
                _ => {}
            }
        }
        _ => {}
    }
}

fn substitute(argument: &str, vars: &HashMap<&str, String>) -> String {
    let mut result = argument.to_string();
    for (key, value) in vars {
        result = result.replace(&format!("${{{key}}}"), value);
    }
    result
}

pub fn command(layout: &Layout, installed: &Installed, account: &Account, settings: &Settings) -> Command {
    let separator = if cfg!(windows) { ";" } else { ":" };
    let classpath = installed.classpath.iter().map(|p| p.to_string_lossy().into_owned()).collect::<Vec<_>>().join(separator);
    let game_dir = installed.game_dir.clone();
    let path = |p: PathBuf| p.to_string_lossy().into_owned();

    let vars: HashMap<&str, String> = HashMap::from([
        ("auth_player_name", account.name.clone()),
        ("version_name", installed.version.id.clone()),
        ("game_directory", path(game_dir.clone())),
        ("assets_root", path(layout.assets())),
        ("assets_index_name", installed.version.asset_index.as_ref().map(|a| a.id.clone()).unwrap_or_default()),
        ("auth_uuid", account.uuid.clone()),
        ("auth_access_token", account.access_token.clone()),
        ("clientid", auth::MS_CLIENT_ID.to_string()),
        ("auth_xuid", account.xuid.clone().unwrap_or_default()),
        ("user_type", if account.kind == "microsoft" { "msa".into() } else { "legacy".into() }),
        ("version_type", installed.version.kind.clone()),
        ("natives_directory", path(layout.natives())),
        ("launcher_name", "lucent-launcher".to_string()),
        ("launcher_version", env!("CARGO_PKG_VERSION").to_string()),
        ("classpath", classpath),
        ("classpath_separator", separator.to_string()),
        ("library_directory", path(layout.libraries())),
    ]);

    let mut jvm = vec![format!("-Xmx{}M", settings.memory_mb), format!("-Xms{}M", settings.memory_mb.min(1024))];
    if let Some(logging) = &installed.logging_argument {
        jvm.push(logging.clone());
    }
    jvm.extend(settings.jvm_args.split_whitespace().map(str::to_string));
    for argument in &installed.version.arguments.jvm {
        expand(argument, &mut jvm);
    }
    let mut game = Vec::new();
    for argument in &installed.version.arguments.game {
        expand(argument, &mut game);
    }

    let mut command = Command::new(&installed.java);
    command
        .current_dir(&game_dir)
        .args(jvm.iter().map(|a| substitute(a, &vars)))
        .arg(&installed.version.main_class)
        .args(game.iter().map(|a| substitute(a, &vars)))
        .stdout(std::process::Stdio::piped())
        .stderr(std::process::Stdio::piped())
        .kill_on_drop(false);
    #[cfg(windows)]
    {
        const CREATE_NO_WINDOW: u32 = 0x0800_0000;
        command.creation_flags(CREATE_NO_WINDOW);
    }
    command
}
