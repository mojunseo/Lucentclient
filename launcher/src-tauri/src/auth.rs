//! Microsoft account sign-in for Minecraft: device code -> Xbox Live -> XSTS -> Minecraft services.

use anyhow::{anyhow, bail, Context, Result};
use serde::{Deserialize, Serialize};
use serde_json::{json, Value};
use std::time::{Duration, SystemTime, UNIX_EPOCH};

/// The Azure application the launcher signs in as. Register a public client app in Azure
/// (Accounts in any identity provider or organizational directory and personal Microsoft accounts,
/// "Allow public client flows" on) and request Minecraft API access for it; until Mojang approves,
/// the last step (login_with_xbox) is refused.
pub const MS_CLIENT_ID: &str = "";
const SCOPE: &str = "XboxLive.signin offline_access";

#[derive(Serialize, Deserialize, Clone, Debug)]
#[serde(rename_all = "camelCase")]
pub struct Account {
    /// "microsoft" or, in development builds only, "offline".
    pub kind: String,
    pub name: String,
    /// Minecraft profile id without dashes.
    pub uuid: String,
    pub access_token: String,
    /// Unix seconds when `access_token` stops working.
    pub expires_at: u64,
    #[serde(default)]
    pub refresh_token: Option<String>,
    #[serde(default)]
    pub xuid: Option<String>,
}

impl Account {
    pub fn needs_refresh(&self) -> bool {
        self.kind == "microsoft" && now() + 300 >= self.expires_at
    }
}

fn now() -> u64 {
    SystemTime::now().duration_since(UNIX_EPOCH).map(|d| d.as_secs()).unwrap_or(0)
}

pub fn configured() -> bool {
    !MS_CLIENT_ID.is_empty()
}

/// A local account for trying the launcher before Microsoft sign-in is approved.
pub fn offline(name: &str) -> Account {
    let uuid = uuid::Uuid::new_v3(&uuid::Uuid::NAMESPACE_OID, format!("OfflinePlayer:{name}").as_bytes());
    Account {
        kind: "offline".into(),
        name: name.into(),
        uuid: uuid.simple().to_string(),
        access_token: "0".into(),
        expires_at: u64::MAX,
        refresh_token: None,
        xuid: None,
    }
}

#[derive(Deserialize, Serialize, Clone, Debug)]
pub struct DeviceCode {
    pub device_code: String,
    pub user_code: String,
    pub verification_uri: String,
    pub expires_in: u64,
    pub interval: u64,
}

pub async fn start_device_code(client: &reqwest::Client) -> Result<DeviceCode> {
    if !configured() {
        bail!("Microsoft sign-in isn't set up in this build yet");
    }
    client
        .post("https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode")
        .form(&[("client_id", MS_CLIENT_ID), ("scope", SCOPE)])
        .send()
        .await?
        .error_for_status()?
        .json()
        .await
        .context("reading the device code")
}

#[derive(Deserialize)]
struct TokenResponse {
    access_token: Option<String>,
    refresh_token: Option<String>,
    error: Option<String>,
}

/// Waits for the user to enter the code in their browser, then finishes signing in.
pub async fn finish_device_code(client: &reqwest::Client, code: &DeviceCode) -> Result<Account> {
    let deadline = now() + code.expires_in;
    let mut interval = code.interval.max(1);
    loop {
        tokio::time::sleep(Duration::from_secs(interval)).await;
        if now() > deadline {
            bail!("The sign-in code expired. Try again.");
        }
        let response: TokenResponse = client
            .post("https://login.microsoftonline.com/consumers/oauth2/v2.0/token")
            .form(&[
                ("client_id", MS_CLIENT_ID),
                ("grant_type", "urn:ietf:params:oauth:grant-type:device_code"),
                ("device_code", code.device_code.as_str()),
            ])
            .send()
            .await?
            .json()
            .await?;
        match (response.access_token, response.error.as_deref()) {
            (Some(token), _) => return minecraft_account(client, &token, response.refresh_token).await,
            (None, Some("authorization_pending")) => {}
            (None, Some("slow_down")) => interval += 5,
            (None, Some("authorization_declined")) => bail!("Sign-in was cancelled."),
            (None, Some("expired_token")) => bail!("The sign-in code expired. Try again."),
            (None, error) => bail!("Microsoft sign-in failed: {}", error.unwrap_or("unknown error")),
        }
    }
}

/// Gets a fresh Minecraft token from the stored Microsoft refresh token.
pub async fn refresh(client: &reqwest::Client, account: &Account) -> Result<Account> {
    let refresh_token = account.refresh_token.as_deref().context("This account has to sign in again")?;
    let response: TokenResponse = client
        .post("https://login.microsoftonline.com/consumers/oauth2/v2.0/token")
        .form(&[
            ("client_id", MS_CLIENT_ID),
            ("grant_type", "refresh_token"),
            ("refresh_token", refresh_token),
            ("scope", SCOPE),
        ])
        .send()
        .await?
        .json()
        .await?;
    let token = response.access_token.context("This account has to sign in again")?;
    minecraft_account(client, &token, response.refresh_token.or_else(|| account.refresh_token.clone())).await
}

async fn post_json(client: &reqwest::Client, url: &str, body: Value) -> Result<Value> {
    let response = client.post(url).header("Accept", "application/json").json(&body).send().await?;
    let status = response.status();
    let value: Value = response.json().await.unwrap_or(Value::Null);
    if !status.is_success() {
        return Err(anyhow!("{url} returned {status}: {value}"));
    }
    Ok(value)
}

async fn minecraft_account(client: &reqwest::Client, ms_token: &str, refresh_token: Option<String>) -> Result<Account> {
    let xbl = post_json(
        client,
        "https://user.auth.xboxlive.com/user/authenticate",
        json!({
            "Properties": { "AuthMethod": "RPS", "SiteName": "user.auth.xboxlive.com", "RpsTicket": format!("d={ms_token}") },
            "RelyingParty": "http://auth.xboxlive.com",
            "TokenType": "JWT"
        }),
    )
    .await?;
    let xbl_token = xbl["Token"].as_str().context("Xbox Live gave no token")?;
    let user_hash = xbl["DisplayClaims"]["xui"][0]["uhs"].as_str().context("Xbox Live gave no user hash")?;

    let xsts_response = client
        .post("https://xsts.auth.xboxlive.com/xsts/authorize")
        .header("Accept", "application/json")
        .json(&json!({
            "Properties": { "SandboxId": "RETAIL", "UserTokens": [xbl_token] },
            "RelyingParty": "rp://api.minecraftservices.com/",
            "TokenType": "JWT"
        }))
        .send()
        .await?;
    let status = xsts_response.status();
    let xsts: Value = xsts_response.json().await.unwrap_or(Value::Null);
    if !status.is_success() {
        match xsts["XErr"].as_u64() {
            Some(2148916233) => bail!("This Microsoft account has no Xbox profile. Sign in once at minecraft.net to create one."),
            Some(2148916235) => bail!("Xbox Live isn't available in your country."),
            Some(2148916236) | Some(2148916237) => bail!("This account needs adult verification on the Xbox website."),
            Some(2148916238) => bail!("This is a child account; an adult has to add it to a Microsoft family first."),
            _ => bail!("Xbox sign-in failed ({status}): {xsts}"),
        }
    }
    let xsts_token = xsts["Token"].as_str().context("XSTS gave no token")?;

    let login = client
        .post("https://api.minecraftservices.com/authentication/login_with_xbox")
        .json(&json!({ "identityToken": format!("XBL3.0 x={user_hash};{xsts_token}") }))
        .send()
        .await?;
    if login.status() == reqwest::StatusCode::FORBIDDEN {
        bail!("Minecraft refused this launcher. Its Azure app hasn't been approved by Mojang yet.");
    }
    let login: Value = login.error_for_status()?.json().await?;
    let access_token = login["access_token"].as_str().context("Minecraft gave no token")?.to_string();
    let expires_in = login["expires_in"].as_u64().unwrap_or(86_400);

    let profile = client
        .get("https://api.minecraftservices.com/minecraft/profile")
        .bearer_auth(&access_token)
        .send()
        .await?;
    if profile.status() == reqwest::StatusCode::NOT_FOUND {
        bail!("This account doesn't own Minecraft: Java Edition.");
    }
    let profile: Value = profile.error_for_status()?.json().await?;

    Ok(Account {
        kind: "microsoft".into(),
        name: profile["name"].as_str().context("profile without name")?.into(),
        uuid: profile["id"].as_str().context("profile without id")?.into(),
        access_token,
        expires_at: now() + expires_in,
        refresh_token,
        xuid: xbl["DisplayClaims"]["xui"][0]["xid"].as_str().map(str::to_string),
    })
}
