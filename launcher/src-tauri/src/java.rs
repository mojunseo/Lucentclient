//! Installs the Java runtime Mojang publishes for each game version.

use crate::download::{self, Download};
use anyhow::{anyhow, Context, Result};
use serde::Deserialize;
use std::collections::HashMap;
use std::path::{Path, PathBuf};

const RUNTIMES: &str =
    "https://launchermeta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json";

#[derive(Deserialize)]
struct RuntimeEntry {
    manifest: ManifestRef,
}

#[derive(Deserialize)]
struct ManifestRef {
    url: String,
}

#[derive(Deserialize)]
struct RuntimeManifest {
    files: HashMap<String, RuntimeFile>,
}

#[derive(Deserialize)]
struct RuntimeFile {
    #[serde(rename = "type")]
    kind: String,
    #[serde(default)]
    executable: bool,
    downloads: Option<RuntimeDownloads>,
    target: Option<String>,
}

#[derive(Deserialize)]
struct RuntimeDownloads {
    raw: RawFile,
}

#[derive(Deserialize)]
struct RawFile {
    sha1: String,
    size: u64,
    url: String,
}

fn platform() -> &'static str {
    match (std::env::consts::OS, std::env::consts::ARCH) {
        ("windows", "aarch64") => "windows-arm64",
        ("windows", "x86") => "windows-x86",
        ("windows", _) => "windows-x64",
        ("macos", "aarch64") => "mac-os-arm64",
        ("macos", _) => "mac-os",
        (_, "x86") => "linux-i386",
        _ => "linux",
    }
}

pub fn java_executable(runtime_dir: &Path) -> PathBuf {
    match std::env::consts::OS {
        "windows" => runtime_dir.join("bin").join("javaw.exe"),
        "macos" => runtime_dir.join("jre.bundle/Contents/Home/bin/java"),
        _ => runtime_dir.join("bin").join("java"),
    }
}

/// Makes sure `component` (e.g. java-runtime-epsilon) is installed under `runtimes_dir` and
/// returns the path to its java executable.
pub async fn install(
    client: &reqwest::Client,
    runtimes_dir: &Path,
    component: &str,
    progress: impl Fn(usize, usize) + Send + Sync,
) -> Result<PathBuf> {
    let runtime_dir = runtimes_dir.join(component);
    let all: HashMap<String, HashMap<String, Vec<RuntimeEntry>>> = download::get_json(client, RUNTIMES).await?;
    let entry = all
        .get(platform())
        .and_then(|components| components.get(component))
        .and_then(|entries| entries.first())
        .ok_or_else(|| anyhow!("Mojang has no {component} runtime for {}", platform()))?;
    let manifest: RuntimeManifest = download::get_json(client, &entry.manifest.url).await?;

    let mut downloads = Vec::new();
    let mut links = Vec::new();
    for (path, file) in &manifest.files {
        let target = runtime_dir.join(path);
        match file.kind.as_str() {
            "directory" => download::ensure_dir(&target)?,
            "file" => {
                let raw = &file.downloads.as_ref().context("runtime file without download")?.raw;
                downloads.push(Download {
                    url: raw.url.clone(),
                    path: target,
                    sha1: Some(raw.sha1.clone()),
                    size: Some(raw.size),
                    executable: file.executable,
                });
            }
            "link" => links.push((target, file.target.clone().unwrap_or_default())),
            _ => {}
        }
    }
    download::fetch_all(client, downloads, progress).await?;

    #[cfg(unix)]
    for (link, target) in links {
        if std::fs::symlink_metadata(&link).is_err() {
            if let Some(parent) = link.parent() {
                download::ensure_dir(parent)?;
            }
            std::os::unix::fs::symlink(&target, &link).with_context(|| format!("linking {}", link.display()))?;
        }
    }
    #[cfg(not(unix))]
    let _ = links;

    Ok(java_executable(&runtime_dir))
}
