//! A lightweight check, run before each launch, that every installed mod's required Fabric
//! dependencies are present in the same mods folder.
//!
//! This only checks that a mod with the right id (or declaring it via `provides`, as Fabric API's
//! own jar does for its bundled modules) exists somewhere in the folder — it does not parse
//! Fabric's version-range syntax, so an installed dependency that is merely too old or too new
//! still passes. Fabric Loader itself does the full check at startup; this just catches the common
//! case of a dependency jar missing entirely, early enough to tell the player which mod needs it.

use serde::{Deserialize, Serialize};
use std::collections::HashSet;
use std::io::Read;
use std::path::Path;

/// Ids the game environment itself satisfies; never a jar in the mods folder.
const BUILTIN: [&str; 4] = ["minecraft", "java", "fabricloader", "fabric-loader"];

#[derive(Deserialize, Default)]
struct FabricModJson {
    id: Option<String>,
    #[serde(default)]
    provides: Vec<String>,
    #[serde(default)]
    depends: std::collections::HashMap<String, serde_json::Value>,
}

#[derive(Serialize, Clone, Debug)]
#[serde(rename_all = "camelCase")]
pub struct DependencyIssue {
    pub mod_name: String,
    pub missing: Vec<String>,
}

fn read_fabric_mod_json(jar: &Path) -> Option<FabricModJson> {
    let file = std::fs::File::open(jar).ok()?;
    let mut archive = zip::ZipArchive::new(file).ok()?;
    let mut entry = archive.by_name("fabric.mod.json").ok()?;
    let mut text = String::new();
    entry.read_to_string(&mut text).ok()?;
    serde_json::from_str(&text).ok()
}

/// Scans every `.jar` in `mods_dir` and reports the ones whose `depends` lists an id that no jar
/// there provides. Jars without a readable `fabric.mod.json` (not Fabric mods, or unreadable) are
/// skipped rather than failing the whole check.
pub fn check(mods_dir: &Path) -> Vec<DependencyIssue> {
    let Ok(entries) = std::fs::read_dir(mods_dir) else { return Vec::new() };
    let mut jars: Vec<(String, FabricModJson)> = Vec::new();
    for entry in entries.flatten() {
        let path = entry.path();
        if path.extension().and_then(|e| e.to_str()) != Some("jar") {
            continue;
        }
        if let Some(info) = read_fabric_mod_json(&path) {
            let filename = path.file_name().unwrap_or_default().to_string_lossy().into_owned();
            jars.push((filename, info));
        }
    }

    let mut available: HashSet<String> = BUILTIN.iter().map(|s| s.to_string()).collect();
    for (_, info) in &jars {
        if let Some(id) = &info.id {
            available.insert(id.clone());
        }
        available.extend(info.provides.iter().cloned());
    }

    jars.iter()
        .filter_map(|(filename, info)| {
            let missing: Vec<String> = info.depends.keys().filter(|dep| !available.contains(dep.as_str())).cloned().collect();
            if missing.is_empty() {
                return None;
            }
            let mod_name = info.id.clone().unwrap_or_else(|| filename.clone());
            Some(DependencyIssue { mod_name, missing })
        })
        .collect()
}
