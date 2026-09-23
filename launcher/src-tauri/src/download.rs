//! HTTP downloads with SHA-1 verification, run many at a time.

use anyhow::{bail, Context, Result};
use futures::{stream, StreamExt};
use sha1::{Digest, Sha1};
use std::path::{Path, PathBuf};
use std::sync::atomic::{AtomicUsize, Ordering};
use std::sync::Arc;

const PARALLEL: usize = 16;

#[derive(Clone, Debug)]
pub struct Download {
    pub url: String,
    pub path: PathBuf,
    pub sha1: Option<String>,
    pub size: Option<u64>,
    pub executable: bool,
}

pub fn client() -> reqwest::Client {
    reqwest::Client::builder()
        .user_agent(concat!("LucentLauncher/", env!("CARGO_PKG_VERSION")))
        .build()
        .expect("HTTP client")
}

/// A file counts as present when it exists with the expected size. Hashing every asset on each
/// launch would take seconds; downloads are hashed when written instead.
async fn is_present(download: &Download) -> bool {
    match tokio::fs::metadata(&download.path).await {
        Ok(meta) => download.size.map_or(true, |size| meta.len() == size),
        Err(_) => false,
    }
}

pub async fn fetch(client: &reqwest::Client, download: &Download) -> Result<()> {
    if is_present(download).await {
        return Ok(());
    }
    let bytes = client
        .get(&download.url)
        .send()
        .await
        .and_then(|r| r.error_for_status())
        .with_context(|| format!("GET {}", download.url))?
        .bytes()
        .await
        .with_context(|| format!("reading {}", download.url))?;
    if let Some(expected) = &download.sha1 {
        let actual = hex::encode(Sha1::digest(&bytes));
        if !actual.eq_ignore_ascii_case(expected) {
            bail!("checksum mismatch for {} (expected {expected}, got {actual})", download.url);
        }
    }
    if let Some(parent) = download.path.parent() {
        tokio::fs::create_dir_all(parent).await?;
    }
    // Write to a temporary file first so an interrupted download never looks complete.
    let tmp = download.path.with_extension("part");
    tokio::fs::write(&tmp, &bytes).await?;
    tokio::fs::rename(&tmp, &download.path).await?;
    #[cfg(unix)]
    if download.executable {
        use std::os::unix::fs::PermissionsExt;
        tokio::fs::set_permissions(&download.path, std::fs::Permissions::from_mode(0o755)).await?;
    }
    Ok(())
}

/// Downloads everything, calling `progress(done, total)` as files finish.
pub async fn fetch_all(
    client: &reqwest::Client,
    downloads: Vec<Download>,
    progress: impl Fn(usize, usize) + Send + Sync,
) -> Result<()> {
    let total = downloads.len();
    let done = Arc::new(AtomicUsize::new(0));
    progress(0, total);
    let results: Vec<Result<()>> = stream::iter(downloads)
        .map(|download| {
            let done = done.clone();
            let progress = &progress;
            async move {
                let result = fetch(client, &download).await;
                progress(done.fetch_add(1, Ordering::Relaxed) + 1, total);
                result
            }
        })
        .buffer_unordered(PARALLEL)
        .collect()
        .await;
    results.into_iter().collect()
}

pub async fn get_json<T: serde::de::DeserializeOwned>(client: &reqwest::Client, url: &str) -> Result<T> {
    client
        .get(url)
        .send()
        .await
        .and_then(|r| r.error_for_status())
        .with_context(|| format!("GET {url}"))?
        .json()
        .await
        .with_context(|| format!("parsing {url}"))
}

/// Maven coordinates (`group:artifact:version[:classifier]`) to a repository path.
pub fn maven_path(name: &str) -> Option<String> {
    let parts: Vec<&str> = name.split(':').collect();
    let (group, artifact, version) = (parts.first()?, parts.get(1)?, parts.get(2)?);
    let classifier = parts.get(3).map(|c| format!("-{c}")).unwrap_or_default();
    Some(format!(
        "{}/{artifact}/{version}/{artifact}-{version}{classifier}.jar",
        group.replace('.', "/")
    ))
}

pub fn ensure_dir(path: &Path) -> Result<()> {
    std::fs::create_dir_all(path).with_context(|| format!("creating {}", path.display()))
}
