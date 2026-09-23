//! Settings and signed-in accounts, saved as JSON in the launcher's data directory.

use crate::auth::Account;
use anyhow::Result;
use serde::{de::DeserializeOwned, Deserialize, Serialize};
use std::path::Path;

#[derive(Serialize, Deserialize, Clone, Debug)]
#[serde(rename_all = "camelCase", default)]
pub struct Settings {
    /// Maximum Java heap in megabytes.
    pub memory_mb: u32,
    /// Extra JVM arguments, space separated.
    pub jvm_args: String,
    /// Hide the launcher while the game runs.
    pub hide_on_launch: bool,
    /// Minecraft version to play; None means the version Lucent Client is built for.
    pub version: Option<String>,
}

impl Default for Settings {
    fn default() -> Self {
        Self { memory_mb: 4096, jvm_args: String::new(), hide_on_launch: true, version: None }
    }
}

#[derive(Serialize, Deserialize, Clone, Debug, Default)]
#[serde(rename_all = "camelCase", default)]
pub struct Accounts {
    pub accounts: Vec<Account>,
    /// uuid of the account to play with.
    pub selected: Option<String>,
}

impl Accounts {
    pub fn selected(&self) -> Option<&Account> {
        let uuid = self.selected.as_ref()?;
        self.accounts.iter().find(|a| &a.uuid == uuid)
    }

    pub fn upsert(&mut self, account: Account) {
        self.selected = Some(account.uuid.clone());
        match self.accounts.iter_mut().find(|a| a.uuid == account.uuid) {
            Some(existing) => *existing = account,
            None => self.accounts.push(account),
        }
    }

    pub fn remove(&mut self, uuid: &str) {
        self.accounts.retain(|a| a.uuid != uuid);
        if self.selected.as_deref() == Some(uuid) {
            self.selected = self.accounts.first().map(|a| a.uuid.clone());
        }
    }
}

pub fn load<T: DeserializeOwned + Default>(path: &Path) -> T {
    std::fs::read(path).ok().and_then(|bytes| serde_json::from_slice(&bytes).ok()).unwrap_or_default()
}

/// Saves as JSON; `private` files (tokens) are readable only by the current user.
pub fn save<T: Serialize>(path: &Path, value: &T, private: bool) -> Result<()> {
    if let Some(parent) = path.parent() {
        std::fs::create_dir_all(parent)?;
    }
    let tmp = path.with_extension("tmp");
    std::fs::write(&tmp, serde_json::to_vec_pretty(value)?)?;
    #[cfg(unix)]
    if private {
        use std::os::unix::fs::PermissionsExt;
        std::fs::set_permissions(&tmp, std::fs::Permissions::from_mode(0o600))?;
    }
    #[cfg(not(unix))]
    let _ = private;
    std::fs::rename(&tmp, path)?;
    Ok(())
}
