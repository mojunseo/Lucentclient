# Lucent Launcher

Desktop launcher for Lucent Client, built with Tauri 2 (Rust backend, TypeScript UI).

It signs in with a Microsoft account, installs Minecraft, Fabric, the assets, Mojang's Java runtime,
Fabric API and Lucent Client into its own data directory, then starts the game. The Minecraft,
Fabric and mod versions come from the repository's `gradle.properties`, so the launcher always
matches the mod.

## Develop

```sh
cd launcher
npm install
npm run tauri dev
```

Development builds install the mod jar from `../build/libs` (run `./gradlew build` first) and offer
offline accounts for testing. Release builds download the latest GitHub release instead.

Run the install check (downloads everything into a directory and prints the java command):

```sh
cd launcher/src-tauri
LUCENT_TEST_DIR=/tmp/lucent-test cargo test install_and_build_command -- --ignored --nocapture
```

## Microsoft sign-in

Set `MS_CLIENT_ID` in `src-tauri/src/auth.rs` to an Azure app registration (personal Microsoft
accounts, public client flows allowed) that Mojang has approved for the Minecraft API.

## Data

Everything lives in the app data directory (`~/.local/share/io.github.mojunseo.lucentlauncher` on
Linux): `game/` is the game directory (saves, options, mods), `accounts.json` holds sign-in tokens
and is readable only by the current user.
