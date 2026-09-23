//! Game and loader versions, read from the mod's gradle.properties at compile time so the launcher
//! always installs exactly what the mod was built against.

const GRADLE_PROPERTIES: &str = include_str!("../../../gradle.properties");

fn property(key: &str) -> &'static str {
    GRADLE_PROPERTIES
        .lines()
        .filter_map(|line| line.split_once('='))
        .find(|(k, _)| k.trim() == key)
        .map(|(_, v)| v.trim())
        .unwrap_or_else(|| panic!("{key} missing from gradle.properties"))
}

pub fn minecraft() -> &'static str {
    property("minecraft_version")
}

pub fn fabric_loader() -> &'static str {
    property("loader_version")
}

pub fn fabric_api() -> &'static str {
    property("fabric_api_version")
}

pub fn mod_version() -> &'static str {
    property("version")
}
