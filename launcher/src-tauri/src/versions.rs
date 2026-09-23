//! Which Minecraft versions Lucent Client is built for, read at compile time from the mod's
//! stonecutter.properties.toml so the launcher always matches the mod.
//!
//! The file has top-level `key = "value"` lines and one `["<build>"]` section per build, whose
//! `mod.mc_releases` lists the Minecraft releases that build runs on.

const PROPERTIES: &str = include_str!("../../../stonecutter.properties.toml");

fn unquote(value: &str) -> &str {
    value.trim().trim_matches('"')
}

fn top_level(key: &str) -> &'static str {
    PROPERTIES
        .lines()
        .take_while(|line| !line.trim_start().starts_with('['))
        .filter_map(|line| line.split_once('='))
        .find(|(k, _)| k.trim() == key)
        .map(|(_, v)| unquote(v))
        .unwrap_or_else(|| panic!("{key} missing from stonecutter.properties.toml"))
}

/// Builds and the releases each runs on, newest build last (file order).
fn builds() -> Vec<(&'static str, Vec<&'static str>)> {
    let mut builds = Vec::new();
    let mut current: Option<&'static str> = None;
    for line in PROPERTIES.lines().map(str::trim) {
        if let Some(name) = line.strip_prefix("[\"").and_then(|l| l.strip_suffix("\"]")) {
            current = Some(name);
        } else if let (Some(build), Some(("mod.mc_releases", list))) = (current, line.split_once('=').map(|(k, v)| (k.trim(), v))) {
            let releases = list.trim().trim_start_matches('[').trim_end_matches(']').split(',').map(unquote).filter(|r| !r.is_empty()).collect();
            builds.push((build, releases));
        }
    }
    builds
}

pub fn mod_version() -> &'static str {
    top_level("mod.version")
}

pub fn fabric_loader() -> &'static str {
    top_level("deps.fabric_loader")
}

/// The Lucent Client build that runs on this Minecraft release, if any.
pub fn build_for(minecraft: &str) -> Option<&'static str> {
    builds().into_iter().find(|(_, releases)| releases.contains(&minecraft)).map(|(build, _)| build)
}

/// Every Minecraft release some Lucent Client build runs on.
pub fn supported() -> Vec<&'static str> {
    builds().into_iter().flat_map(|(_, releases)| releases).collect()
}

/// The newest supported release, played when nothing else is picked.
pub fn default_minecraft() -> &'static str {
    builds().last().and_then(|(_, releases)| releases.last().copied()).expect("no builds in stonecutter.properties.toml")
}

#[cfg(test)]
mod tests {
    #[test]
    fn reads_builds() {
        assert!(!super::mod_version().is_empty());
        assert_eq!(super::build_for("26.1.1"), Some("26.1.2"));
        assert_eq!(super::build_for("1.8.9"), None);
        assert!(super::supported().contains(&super::default_minecraft()));
    }
}
