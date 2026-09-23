//! Mojang version JSON, the Fabric profile layered on top of it, and OS rules.

use serde::Deserialize;
use serde_json::Value;
use std::collections::HashMap;

pub const VERSION_MANIFEST: &str = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

#[derive(Deserialize)]
pub struct VersionManifest {
    pub versions: Vec<ManifestEntry>,
}

#[derive(Deserialize)]
pub struct ManifestEntry {
    pub id: String,
    pub url: String,
}

#[derive(Deserialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct Version {
    pub id: String,
    #[serde(default)]
    pub arguments: Arguments,
    pub asset_index: Option<AssetIndexRef>,
    pub downloads: Option<VersionDownloads>,
    pub java_version: Option<JavaVersion>,
    #[serde(default)]
    pub libraries: Vec<Library>,
    pub logging: Option<Logging>,
    pub main_class: String,
    #[serde(rename = "type", default)]
    pub kind: String,
}

#[derive(Deserialize, Default, Clone)]
pub struct Arguments {
    #[serde(default)]
    pub game: Vec<Value>,
    #[serde(default)]
    pub jvm: Vec<Value>,
}

#[derive(Deserialize, Clone)]
pub struct AssetIndexRef {
    pub id: String,
    pub sha1: String,
    pub size: u64,
    pub url: String,
}

#[derive(Deserialize, Clone)]
pub struct VersionDownloads {
    pub client: FileRef,
}

#[derive(Deserialize, Clone)]
pub struct FileRef {
    pub sha1: String,
    pub size: u64,
    pub url: String,
}

#[derive(Deserialize, Clone)]
pub struct JavaVersion {
    pub component: String,
}

#[derive(Deserialize, Clone)]
pub struct Library {
    pub name: String,
    pub downloads: Option<LibraryDownloads>,
    /// Maven repository base, used by Fabric libraries instead of `downloads`.
    pub url: Option<String>,
    pub sha1: Option<String>,
    pub size: Option<u64>,
    pub rules: Option<Vec<Rule>>,
}

#[derive(Deserialize, Clone)]
pub struct LibraryDownloads {
    pub artifact: Option<Artifact>,
}

#[derive(Deserialize, Clone)]
pub struct Artifact {
    pub path: String,
    pub sha1: String,
    pub size: u64,
    pub url: String,
}

#[derive(Deserialize, Clone)]
pub struct Logging {
    pub client: Option<LoggingClient>,
}

#[derive(Deserialize, Clone)]
pub struct LoggingClient {
    pub argument: String,
    pub file: LoggingFile,
}

#[derive(Deserialize, Clone)]
pub struct LoggingFile {
    pub id: String,
    pub sha1: String,
    pub size: u64,
    pub url: String,
}

#[derive(Deserialize, Clone)]
pub struct Rule {
    pub action: String,
    pub os: Option<OsRule>,
    pub features: Option<HashMap<String, bool>>,
}

#[derive(Deserialize, Clone)]
pub struct OsRule {
    pub name: Option<String>,
    pub arch: Option<String>,
}

#[derive(Deserialize)]
pub struct AssetIndex {
    pub objects: HashMap<String, AssetObject>,
}

#[derive(Deserialize)]
pub struct AssetObject {
    pub hash: String,
    pub size: u64,
}

pub fn os_name() -> &'static str {
    match std::env::consts::OS {
        "windows" => "windows",
        "macos" => "osx",
        _ => "linux",
    }
}

/// Evaluates Mojang rules: the last matching rule decides; with no rules, allow.
/// No launcher features (demo, custom resolution, quick play) are enabled.
pub fn rules_allow(rules: Option<&[Rule]>) -> bool {
    let Some(rules) = rules else { return true };
    let mut allowed = false;
    for rule in rules {
        let os_matches = rule.os.as_ref().map_or(true, |os| {
            os.name.as_deref().map_or(true, |name| name == os_name())
                && os.arch.as_deref().map_or(true, |arch| match arch {
                    "x86" => std::env::consts::ARCH == "x86",
                    "arm64" => std::env::consts::ARCH == "aarch64",
                    _ => true,
                })
        });
        let features_match = rule.features.as_ref().map_or(true, |features| features.values().all(|wanted| !wanted));
        if os_matches && features_match {
            allowed = rule.action == "allow";
        }
    }
    allowed
}

impl Library {
    /// Library group and artifact plus classifier, used to let Fabric's copy of a library win.
    pub fn key(&self) -> String {
        let parts: Vec<&str> = self.name.split(':').collect();
        match parts.as_slice() {
            [group, artifact, _, classifier, ..] => format!("{group}:{artifact}:{classifier}"),
            [group, artifact, ..] => format!("{group}:{artifact}"),
            _ => self.name.clone(),
        }
    }
}

/// Applies a Fabric profile (which `inheritsFrom` the vanilla version) on top of vanilla.
pub fn merge(vanilla: &Version, fabric: Version) -> Version {
    let mut merged = vanilla.clone();
    merged.id = fabric.id;
    merged.main_class = fabric.main_class;
    let fabric_keys: Vec<String> = fabric.libraries.iter().map(Library::key).collect();
    merged.libraries.retain(|library| !fabric_keys.contains(&library.key()));
    let mut libraries = fabric.libraries;
    libraries.extend(merged.libraries);
    merged.libraries = libraries;
    merged.arguments.jvm.extend(fabric.arguments.jvm);
    merged.arguments.game.extend(fabric.arguments.game);
    merged
}
