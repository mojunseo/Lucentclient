//! Third-party mods from Modrinth: search, install with required dependencies, enable/disable, remove.
//! Installed mods are tracked per instance in `lucent-mods.json`.

use crate::download::{self, Download};
use anyhow::{anyhow, Context, Result};
use serde::{Deserialize, Serialize};
use std::collections::HashSet;
use std::path::{Path, PathBuf};

const API: &str = "https://api.modrinth.com/v2";
/// Fabric API's Modrinth project; the launcher installs it itself.
pub const FABRIC_API_PROJECT: &str = "P7dR8mSH";
const MANIFEST: &str = "lucent-mods.json";

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

#[derive(Serialize, Deserialize, Clone, Debug)]
#[serde(rename_all = "camelCase")]
pub struct InstalledMod {
    pub project_id: String,
    pub version_id: String,
    pub title: String,
    pub filename: String,
    pub icon_url: Option<String>,
    /// Installed because another mod needs it.
    pub dependency: bool,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct InstalledView {
    #[serde(flatten)]
    pub installed: InstalledMod,
    pub enabled: bool,
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

/// One instance's mods folder and its record of what came from Modrinth.
pub struct Instance {
    pub dir: PathBuf,
}

impl Instance {
    fn mods(&self) -> PathBuf {
        self.dir.join("mods")
    }

    fn manifest_path(&self) -> PathBuf {
        self.dir.join(MANIFEST)
    }

    pub fn load(&self) -> Vec<InstalledMod> {
        crate::state::load::<Vec<InstalledMod>>(&self.manifest_path())
    }

    fn save(&self, mods: &[InstalledMod]) -> Result<()> {
        crate::state::save(&self.manifest_path(), &mods.to_vec(), false)
    }

    fn enabled(&self, filename: &str) -> bool {
        self.mods().join(filename).exists()
    }

    pub fn list(&self) -> Vec<InstalledView> {
        self.load()
            .into_iter()
            .map(|installed| InstalledView { enabled: self.enabled(&installed.filename), installed })
            .collect()
    }

    /// Installs the newest compatible version of `project` and anything it requires.
    pub async fn install(&self, client: &reqwest::Client, project: &str, minecraft: &str) -> Result<()> {
        download::ensure_dir(&self.mods())?;
        let mut installed = self.load();
        let mut seen: HashSet<String> = installed.iter().map(|m| m.project_id.clone()).collect();
        seen.insert(FABRIC_API_PROJECT.to_string());
        seen.remove(project);

        let mut queue = vec![(project.to_string(), None::<String>, false)];
        while let Some((project_id, version_id, dependency)) = queue.pop() {
            let version = match &version_id {
                Some(id) => download::get_json::<ModVersion>(client, &format!("{API}/version/{id}")).await?,
                None => latest_version(client, &project_id, minecraft).await?,
            };
            let file = primary_file(&version)?.clone();
            download::fetch(client, &file_download(&file, &self.mods())).await?;
            let info: Project = download::get_json(client, &format!("{API}/project/{project_id}")).await?;

            // Replace an older copy of the same project.
            if let Some(old) = installed.iter().find(|m| m.project_id == project_id) {
                if old.filename != file.filename {
                    let _ = std::fs::remove_file(self.mods().join(&old.filename));
                    let _ = std::fs::remove_file(self.mods().join(format!("{}.disabled", old.filename)));
                }
            }
            installed.retain(|m| m.project_id != project_id);
            installed.push(InstalledMod {
                project_id: project_id.clone(),
                version_id: version.id.clone(),
                title: info.title,
                filename: file.filename,
                icon_url: info.icon_url,
                dependency,
            });
            seen.insert(project_id);

            for dep in version.dependencies.iter().filter(|d| d.dependency_type == "required") {
                let Some(dep_project) = dep.project_id.clone() else { continue };
                if seen.insert(dep_project.clone()) {
                    queue.push((dep_project, dep.version_id.clone(), true));
                }
            }
        }
        self.save(&installed)
    }

    /// Enables or disables a mod by renaming it to/from `.jar.disabled`, which Fabric ignores.
    pub fn set_enabled(&self, filename: &str, enabled: bool) -> Result<()> {
        let jar = self.mods().join(filename);
        let disabled = self.mods().join(format!("{filename}.disabled"));
        if enabled && disabled.exists() {
            std::fs::rename(disabled, jar)?;
        } else if !enabled && jar.exists() {
            std::fs::rename(jar, disabled)?;
        }
        Ok(())
    }

    pub fn remove(&self, project_id: &str) -> Result<()> {
        let mut installed = self.load();
        if let Some(entry) = installed.iter().find(|m| m.project_id == project_id) {
            let _ = std::fs::remove_file(self.mods().join(&entry.filename));
            let _ = std::fs::remove_file(self.mods().join(format!("{}.disabled", entry.filename)));
        }
        installed.retain(|m| m.project_id != project_id);
        self.save(&installed)
    }
}
