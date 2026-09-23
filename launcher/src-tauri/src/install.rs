//! Installs everything the game needs: Minecraft, Fabric, assets, Java and the mods.

use crate::download::{self, Download};
use crate::meta::{self, AssetIndex, Version, VersionManifest};
use crate::{java, modrinth, versions};
use anyhow::{anyhow, bail, Context, Result};
use serde::{Deserialize, Serialize};
use std::path::{Path, PathBuf};

/// Where things live under the launcher's data directory.
#[derive(Clone)]
pub struct Layout {
    pub root: PathBuf,
}

impl Layout {
    pub fn versions(&self) -> PathBuf {
        self.root.join("versions")
    }
    pub fn libraries(&self) -> PathBuf {
        self.root.join("libraries")
    }
    pub fn assets(&self) -> PathBuf {
        self.root.join("assets")
    }
    pub fn runtimes(&self) -> PathBuf {
        self.root.join("runtimes")
    }
    pub fn natives(&self) -> PathBuf {
        self.root.join("natives")
    }
    /// One game directory per Minecraft version (saves, options, mods, configs), since mods only
    /// work with the version they were made for.
    pub fn instance(&self, minecraft: &str) -> PathBuf {
        self.root.join("instances").join(minecraft)
    }
}

#[derive(Serialize, Clone)]
pub struct Progress {
    /// metadata, libraries, assets, java or mods
    pub stage: &'static str,
    pub done: usize,
    pub total: usize,
}

pub struct Installed {
    pub version: Version,
    pub game_dir: PathBuf,
    pub java: PathBuf,
    pub classpath: Vec<PathBuf>,
    /// The JVM argument that points log4j at its config, already filled in.
    pub logging_argument: Option<String>,
}

pub async fn install(
    client: &reqwest::Client,
    layout: &Layout,
    minecraft: &str,
    progress: impl Fn(Progress) + Send + Sync,
) -> Result<Installed> {
    let report = |stage: &'static str| move |done: usize, total: usize| (stage, done, total);
    let emit = |(stage, done, total): (&'static str, usize, usize)| progress(Progress { stage, done, total });

    emit(report("metadata")(0, 1));
    let version = resolve_version(client, layout, minecraft).await?;
    emit(report("metadata")(1, 1));

    // Game jar and libraries.
    let mut classpath = Vec::new();
    let mut downloads = Vec::new();
    for library in &version.libraries {
        if !meta::rules_allow(library.rules.as_deref()) {
            continue;
        }
        let (url, path, sha1, size) = if let Some(artifact) = library.downloads.as_ref().and_then(|d| d.artifact.as_ref()) {
            (artifact.url.clone(), artifact.path.clone(), Some(artifact.sha1.clone()), Some(artifact.size))
        } else if let Some(base) = &library.url {
            let path = download::maven_path(&library.name).ok_or_else(|| anyhow!("bad library name {}", library.name))?;
            (format!("{}/{path}", base.trim_end_matches('/')), path, library.sha1.clone(), library.size)
        } else {
            continue;
        };
        let target = layout.libraries().join(&path);
        classpath.push(target.clone());
        downloads.push(Download { url, path: target, sha1, size, executable: false });
    }
    let client_download = version.downloads.as_ref().context("version has no client download")?.client.clone();
    let client_jar = layout.versions().join(minecraft).join(format!("{minecraft}.jar"));
    downloads.push(Download {
        url: client_download.url,
        path: client_jar.clone(),
        sha1: Some(client_download.sha1),
        size: Some(client_download.size),
        executable: false,
    });
    classpath.push(client_jar);
    download::fetch_all(client, downloads, |d, t| emit(report("libraries")(d, t))).await?;

    // Assets.
    let index_ref = version.asset_index.clone().context("version has no asset index")?;
    let index_path = layout.assets().join("indexes").join(format!("{}.json", index_ref.id));
    download::fetch(
        client,
        &Download {
            url: index_ref.url.clone(),
            path: index_path.clone(),
            sha1: Some(index_ref.sha1.clone()),
            size: Some(index_ref.size),
            executable: false,
        },
    )
    .await?;
    let index: AssetIndex = serde_json::from_slice(&tokio::fs::read(&index_path).await?)?;
    let asset_downloads = index
        .objects
        .values()
        .map(|object| {
            let prefix = &object.hash[..2];
            Download {
                url: format!("https://resources.download.minecraft.net/{prefix}/{}", object.hash),
                path: layout.assets().join("objects").join(prefix).join(&object.hash),
                sha1: Some(object.hash.clone()),
                size: Some(object.size),
                executable: false,
            }
        })
        .collect();
    download::fetch_all(client, asset_downloads, |d, t| emit(report("assets")(d, t))).await?;

    let mut logging_argument = None;
    if let Some(logging) = version.logging.as_ref().and_then(|l| l.client.as_ref()) {
        let path = layout.assets().join("log_configs").join(&logging.file.id);
        download::fetch(
            client,
            &Download {
                url: logging.file.url.clone(),
                path: path.clone(),
                sha1: Some(logging.file.sha1.clone()),
                size: Some(logging.file.size),
                executable: false,
            },
        )
        .await?;
        logging_argument = Some(logging.argument.replace("${path}", &path.to_string_lossy()));
    }

    // Java.
    let component = version.java_version.as_ref().map(|j| j.component.clone()).unwrap_or_else(|| "java-runtime-delta".into());
    let java = java::install(client, &layout.runtimes(), &component, |d, t| emit(report("java")(d, t))).await?;

    // Mods.
    emit(report("mods")(0, 2));
    let game_dir = layout.instance(minecraft);
    install_mods(client, &game_dir, minecraft).await?;
    emit(report("mods")(2, 2));

    download::ensure_dir(&layout.natives())?;
    Ok(Installed { version, game_dir, java, classpath, logging_argument })
}

#[derive(Deserialize)]
struct LoaderEntry {
    loader: LoaderVersion,
}

#[derive(Deserialize)]
struct LoaderVersion {
    version: String,
    stable: bool,
}

/// The Fabric Loader to use: the one the mod was built with on versions it supports, otherwise the
/// newest stable loader for that Minecraft version.
async fn fabric_loader(client: &reqwest::Client, minecraft: &str) -> Result<String> {
    if versions::build_for(minecraft).is_some() {
        return Ok(versions::fabric_loader().to_string());
    }
    let loaders: Vec<LoaderEntry> =
        download::get_json(client, &format!("https://meta.fabricmc.net/v2/versions/loader/{minecraft}")).await?;
    loaders
        .into_iter()
        .find(|l| l.loader.stable)
        .map(|l| l.loader.version)
        .ok_or_else(|| anyhow!("Fabric doesn't support Minecraft {minecraft}"))
}

async fn resolve_version(client: &reqwest::Client, layout: &Layout, minecraft: &str) -> Result<Version> {
    let id = minecraft;
    let dir = layout.versions().join(id);
    let vanilla_path = dir.join(format!("{id}.json"));
    let vanilla: Version = match tokio::fs::read(&vanilla_path).await {
        Ok(bytes) => serde_json::from_slice(&bytes)?,
        Err(_) => {
            let manifest: VersionManifest = download::get_json(client, meta::VERSION_MANIFEST).await?;
            let entry = manifest.versions.iter().find(|v| v.id == id).ok_or_else(|| anyhow!("Minecraft {id} not found"))?;
            let bytes = client.get(&entry.url).send().await?.error_for_status()?.bytes().await?;
            download::ensure_dir(&dir)?;
            tokio::fs::write(&vanilla_path, &bytes).await?;
            serde_json::from_slice(&bytes)?
        }
    };
    let loader = fabric_loader(client, id).await?;
    let fabric_url = format!("https://meta.fabricmc.net/v2/versions/loader/{id}/{loader}/profile/json");
    let fabric_path = dir.join(format!("fabric-{loader}.json"));
    let fabric: Version = match tokio::fs::read(&fabric_path).await {
        Ok(bytes) => serde_json::from_slice(&bytes)?,
        Err(_) => {
            let bytes = client.get(&fabric_url).send().await?.error_for_status()?.bytes().await?;
            tokio::fs::write(&fabric_path, &bytes).await?;
            serde_json::from_slice(&bytes)?
        }
    };
    Ok(meta::merge(&vanilla, fabric))
}

#[derive(Deserialize)]
struct Release {
    assets: Vec<ReleaseAsset>,
}

#[derive(Deserialize)]
struct ReleaseAsset {
    name: String,
    size: u64,
    browser_download_url: String,
}

/// Puts Fabric API, and Lucent Client when this is its version, into the instance's mods folder,
/// replacing older copies the launcher put there. Mods installed from Modrinth are left alone.
async fn install_mods(client: &reqwest::Client, game_dir: &Path, minecraft: &str) -> Result<()> {
    let mods = game_dir.join("mods");
    download::ensure_dir(&mods)?;

    let fabric_api = modrinth::latest_version(client, modrinth::FABRIC_API_PROJECT, minecraft)
        .await
        .context("finding Fabric API")?;
    let file = modrinth::primary_file(&fabric_api)?;
    download::fetch(client, &modrinth::file_download(file, &mods)).await?;
    remove_stale(&mods, "fabric-api-", &file.filename)?;

    if let Some(build) = versions::build_for(minecraft) {
        let lucent = install_lucent(client, &mods, build).await?;
        remove_stale(&mods, "lucentclient-", &lucent)?;
    } else {
        // No Lucent Client build for this version.
        remove_stale(&mods, "lucentclient-", "")?;
    }
    Ok(())
}

/// Development builds use the jar built next to the launcher; releases download the latest GitHub release.
async fn install_lucent(client: &reqwest::Client, mods: &Path, build: &str) -> Result<String> {
    let name = format!("lucentclient-{}+{build}.jar", versions::mod_version());
    if cfg!(debug_assertions) {
        let local = Path::new(env!("CARGO_MANIFEST_DIR")).join("../../versions").join(build).join("build/libs").join(&name);
        if local.exists() {
            tokio::fs::copy(&local, mods.join(&name)).await.context("copying the locally built mod")?;
            return Ok(name);
        }
    }
    let response = client.get("https://api.github.com/repos/mojunseo/Lucentclient/releases/latest").send().await?;
    if response.status() == reqwest::StatusCode::NOT_FOUND {
        bail!("No Lucent Client release has been published yet");
    }
    let release: Release = response.error_for_status()?.json().await?;
    let asset = release
        .assets
        .into_iter()
        .find(|a| a.name == name)
        .with_context(|| format!("The latest release has no Lucent Client jar for Minecraft {build}"))?;
    download::fetch(
        client,
        &Download { url: asset.browser_download_url, path: mods.join(&asset.name), sha1: None, size: Some(asset.size), executable: false },
    )
    .await?;
    Ok(asset.name)
}

fn remove_stale(mods: &Path, prefix: &str, keep: &str) -> Result<()> {
    for entry in std::fs::read_dir(mods)? {
        let name = entry?.file_name().to_string_lossy().into_owned();
        if name.starts_with(prefix) && name.ends_with(".jar") && name != keep {
            std::fs::remove_file(mods.join(&name))?;
        }
    }
    Ok(())
}
