//! Third-party mods from Modrinth.
//!
//! The player keeps one list of mods for every Minecraft version (`mods.json` in the data
//! directory). Before a version is played, [`sync`] puts the newest file of each listed mod for that
//! version, plus the mods they require, into its instance, and removes files the launcher put there
//! that are no longer wanted. What went into an instance is recorded in its `lucent-mods.json`.

use crate::download::{self, Download};
use anyhow::{anyhow, Context, Result};
use serde::{Deserialize, Serialize};
use std::collections::HashSet;
use std::path::Path;

const API: &str = "https://api.modrinth.com/v2";
/// Fabric API's Modrinth project; the launcher installs it itself.
pub const FABRIC_API_PROJECT: &str = "P7dR8mSH";
const WANTED: &str = "mods.json";
const INSTANCE_RECORD: &str = "lucent-mods.json";

/// Read from Modrinth's snake_case JSON, sent to the UI as camelCase.
#[derive(Serialize, Deserialize, Clone, Debug)]
#[serde(rename_all(serialize = "camelCase"))]
pub struct SearchHit {
    pub project_id: String,
    pub slug: String,
    pub title: String,
    pub author: String,
    pub description: String,
    pub downloads: u64,
    pub icon_url: Option<String>,
}

#[derive(Deserialize)]
struct SearchResponse {
    hits: Vec<SearchHit>,
    total_hits: u64,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SearchPage {
    pub hits: Vec<SearchHit>,
    pub total: u64,
}

#[derive(Deserialize, Clone)]
pub struct ModVersion {
    pub id: String,
    pub version_type: String,
    pub files: Vec<ModFile>,
    #[serde(default)]
    pub dependencies: Vec<Dependency>,
}

#[derive(Deserialize, Clone)]
pub struct ModFile {
    pub url: String,
    pub filename: String,
    pub primary: bool,
    pub size: u64,
    pub hashes: Hashes,
}

#[derive(Deserialize, Clone)]
pub struct Hashes {
    pub sha1: String,
}

#[derive(Deserialize, Clone)]
pub struct Dependency {
    pub project_id: Option<String>,
    pub version_id: Option<String>,
    pub dependency_type: String,
}

#[derive(Deserialize)]
struct Project {
    title: String,
    icon_url: Option<String>,
}

fn encode(value: &str) -> String {
    value
        .bytes()
        .map(|b| match b {
            b'A'..=b'Z' | b'a'..=b'z' | b'0'..=b'9' | b'-' | b'_' | b'.' | b'~' => (b as char).to_string(),
            _ => format!("%{b:02X}"),
        })
        .collect()
}

/// Sort orders Modrinth search accepts.
const SORTS: [&str; 5] = ["relevance", "downloads", "follows", "newest", "updated"];

/// Searches Fabric mods for `minecraft`. Every category in `categories` must match.
pub async fn search(
    client: &reqwest::Client,
    query: &str,
    minecraft: &str,
    offset: u32,
    sort: &str,
    categories: &[String],
) -> Result<SearchPage> {
    let mut facets = vec![
        r#"["project_type:mod"]"#.to_string(),
        r#"["categories:fabric"]"#.to_string(),
        format!(r#"["versions:{minecraft}"]"#),
    ];
    for category in categories {
        if category.chars().all(|c| c.is_ascii_lowercase() || c == '-') {
            facets.push(format!(r#"["categories:{category}"]"#));
        }
    }
    let index = if SORTS.contains(&sort) { sort } else { "relevance" };
    let url = format!(
        "{API}/search?query={}&facets={}&index={index}&limit=20&offset={offset}",
        encode(query.trim()),
        encode(&format!("[{}]", facets.join(",")))
    );
    let response: SearchResponse = download::get_json(client, &url).await?;
    Ok(SearchPage {
        hits: response.hits.into_iter().filter(|hit| hit.project_id != FABRIC_API_PROJECT).collect(),
        total: response.total_hits,
    })
}

/// Newest version of a project for this Minecraft version on Fabric, preferring releases.
pub async fn latest_version(client: &reqwest::Client, project: &str, minecraft: &str) -> Result<ModVersion> {
    let url = format!(
        "{API}/project/{project}/version?loaders={}&game_versions={}",
        encode(r#"["fabric"]"#),
        encode(&format!(r#"["{minecraft}"]"#))
    );
    let versions: Vec<ModVersion> = download::get_json(client, &url).await?;
    let release = versions.iter().find(|v| v.version_type == "release").cloned();
    release
        .or_else(|| versions.into_iter().next())
        .ok_or_else(|| anyhow!("No Fabric version of this mod for Minecraft {minecraft}"))
}

pub fn primary_file(version: &ModVersion) -> Result<&ModFile> {
    version
        .files
        .iter()
        .find(|f| f.primary)
        .or_else(|| version.files.first())
        .context("This mod version has no files")
}

pub fn file_download(file: &ModFile, mods: &Path) -> Download {
    Download {
        url: file.url.clone(),
        path: mods.join(&file.filename),
        sha1: Some(file.hashes.sha1.clone()),
        size: Some(file.size),
        executable: false,
    }
}

/// A mod the player chose, for every version.
#[derive(Serialize, Deserialize, Clone, Debug)]
#[serde(rename_all = "camelCase")]
pub struct WantedMod {
    pub project_id: String,
    pub title: String,
    pub icon_url: Option<String>,
    #[serde(default = "enabled_by_default")]
    pub enabled: bool,
}

fn enabled_by_default() -> bool {
    true
}

/// A file the launcher put into one instance's mods folder.
#[derive(Serialize, Deserialize, Clone, Debug)]
#[serde(rename_all = "camelCase")]
pub struct InstanceFile {
    pub project_id: String,
    pub version_id: String,
    pub filename: String,
    /// Installed only because another mod needs it.
    #[serde(default)]
    pub dependency: bool,
    /// Kept by older launchers; used once to build the shared list.
    #[serde(default)]
    pub title: Option<String>,
    #[serde(default)]
    pub icon_url: Option<String>,
}

#[derive(Serialize, Deserialize, Default)]
#[serde(rename_all = "camelCase")]
pub struct InstanceRecord {
    pub files: Vec<InstanceFile>,
    /// Wanted mods that have no file for this instance's version.
    pub missing: Vec<String>,
}

/// Reads an instance record, including the plain list older launchers wrote.
pub fn load_record(instance: &Path) -> InstanceRecord {
    let path = instance.join(INSTANCE_RECORD);
    let Ok(bytes) = std::fs::read(&path) else { return InstanceRecord::default() };
    serde_json::from_slice::<InstanceRecord>(&bytes)
        .or_else(|_| serde_json::from_slice::<Vec<InstanceFile>>(&bytes).map(|files| InstanceRecord { files, missing: Vec::new() }))
        .unwrap_or_default()
}

fn save_record(instance: &Path, record: &InstanceRecord) -> Result<()> {
    crate::state::save(&instance.join(INSTANCE_RECORD), record, false)
}

pub fn load_wanted(root: &Path) -> Vec<WantedMod> {
    crate::state::load(&root.join(WANTED))
}

pub fn save_wanted(root: &Path, wanted: &[WantedMod]) -> Result<()> {
    crate::state::save(&root.join(WANTED), &wanted.to_vec(), false)
}

/// Builds the shared list from mods installed per instance by older launchers, once.
pub fn migrate(root: &Path, instances: &Path) -> Result<()> {
    if root.join(WANTED).exists() {
        return Ok(());
    }
    let mut wanted: Vec<WantedMod> = Vec::new();
    if let Ok(entries) = std::fs::read_dir(instances) {
        for entry in entries.flatten() {
            for file in load_record(&entry.path()).files {
                if file.dependency || wanted.iter().any(|w| w.project_id == file.project_id) {
                    continue;
                }
                let enabled = entry.path().join("mods").join(&file.filename).exists();
                wanted.push(WantedMod {
                    title: file.title.clone().unwrap_or_else(|| file.project_id.clone()),
                    project_id: file.project_id,
                    icon_url: file.icon_url,
                    enabled,
                });
            }
        }
    }
    save_wanted(root, &wanted)
}

/// Adds a mod to the shared list.
pub async fn add(client: &reqwest::Client, root: &Path, project: &str) -> Result<()> {
    let mut wanted = load_wanted(root);
    if wanted.iter().any(|w| w.project_id == project) {
        return Ok(());
    }
    let info: Project = download::get_json(client, &format!("{API}/project/{project}")).await?;
    wanted.push(WantedMod { project_id: project.to_string(), title: info.title, icon_url: info.icon_url, enabled: true });
    save_wanted(root, &wanted)
}

#[derive(Serialize, Default)]
#[serde(rename_all = "camelCase")]
pub struct SyncReport {
    /// Titles of enabled mods with no file for this version.
    pub missing: Vec<String>,
}

/// Makes an instance's mods folder match the shared list for `minecraft`: downloads the newest
/// file of every enabled mod and its required dependencies, and deletes files the launcher added
/// earlier that are no longer needed. Jars the player dropped in by hand are left alone.
pub async fn sync(client: &reqwest::Client, root: &Path, instance: &Path, minecraft: &str) -> Result<SyncReport> {
    let mods = instance.join("mods");
    download::ensure_dir(&mods)?;
    let wanted = load_wanted(root);
    let old = load_record(instance);

    let mut files: Vec<InstanceFile> = Vec::new();
    let mut missing: Vec<WantedMod> = Vec::new();
    let mut seen: HashSet<String> = HashSet::from([FABRIC_API_PROJECT.to_string()]);
    let mut queue: Vec<(String, Option<String>, bool)> = Vec::new();
    for mod_ in wanted.iter().filter(|w| w.enabled) {
        seen.insert(mod_.project_id.clone());
        queue.push((mod_.project_id.clone(), None, false));
    }

    let mut downloads = Vec::new();
    while let Some((project_id, version_id, dependency)) = queue.pop() {
        let version = match &version_id {
            Some(id) => download::get_json::<ModVersion>(client, &format!("{API}/version/{id}")).await,
            None => latest_version(client, &project_id, minecraft).await,
        };
        let version = match version {
            Ok(version) => version,
            Err(error) if !dependency => {
                // Not available for this version (or Modrinth said no): play without it.
                log::warn(&format!("skipping {project_id} for {minecraft}: {error:#}"));
                if let Some(mod_) = wanted.iter().find(|w| w.project_id == project_id) {
                    missing.push(mod_.clone());
                }
                continue;
            }
            Err(error) => return Err(error.context(format!("a mod needs {project_id}, which has no version for {minecraft}"))),
        };
        let file = primary_file(&version)?.clone();
        downloads.push(file_download(&file, &mods));
        files.push(InstanceFile {
            project_id: project_id.clone(),
            version_id: version.id.clone(),
            filename: file.filename.clone(),
            dependency,
            title: None,
            icon_url: None,
        });
        for dep in version.dependencies.iter().filter(|d| d.dependency_type == "required") {
            let Some(dep_project) = dep.project_id.clone() else { continue };
            if seen.insert(dep_project.clone()) {
                queue.push((dep_project, dep.version_id.clone(), true));
            }
        }
    }
    download::fetch_all(client, downloads, |_, _| {}).await?;

    // Remove what the launcher put here before and no longer wants, including disabled copies.
    let keep: HashSet<&str> = files.iter().map(|f| f.filename.as_str()).collect();
    for file in &old.files {
        if !keep.contains(file.filename.as_str()) {
            let _ = std::fs::remove_file(mods.join(&file.filename));
        }
        let _ = std::fs::remove_file(mods.join(format!("{}.disabled", file.filename)));
    }

    let record = InstanceRecord { files, missing: missing.iter().map(|m| m.project_id.clone()).collect() };
    save_record(instance, &record)?;
    Ok(SyncReport { missing: missing.into_iter().map(|m| m.title).collect() })
}

mod log {
    pub fn warn(message: &str) {
        eprintln!("[lucent-launcher] {message}");
    }
}
