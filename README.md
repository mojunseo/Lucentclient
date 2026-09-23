<p align="center"><img src="launcher/src-tauri/icons/128x128.png" width="96" alt=""></p>

# Lucent Client

**English** | [한국어](README.ko.md) | [日本語](README.ja.md)

A Fabric client mod for Minecraft: Java Edition, with its own desktop launcher.

## Features

Press **Right Shift** in game to open the menu. Click a module's lamp to turn it on or off, or the rest of its row to change its settings.

- **HUD**: FPS, coordinates, keystrokes with CPS, armor and durability, status effects, ping, clock. Drag them anywhere with the layout editor, and change text color, background, shadow and size per module.
- **PvP**: reach display, toggle sprint and sneak, zoom (scroll to change it while zoomed), FOV changer with separate amounts for walking, sprinting, Speed and flying.
- **Performance**: entity and block entity culling by distance, fewer particles, hide rain and snow.
- **Other**: fullbright, Discord Rich Presence.
- **Cosmetics**: capes drawn as moving cloth, feathered, dragon and butterfly wings, hats and halos. For now only you can see yours.

## Install

### Mod

Supports Minecraft **26.1 to 26.3**.

1. Install [Fabric Loader](https://fabricmc.net/use/) 0.19.5 or newer and [Fabric API](https://modrinth.com/mod/fabric-api).
2. Download the jar for your Minecraft version from [Releases](https://github.com/mojunseo/Lucentclient/releases) and put it in your `mods` folder.

| Minecraft | File |
|---|---|
| 26.1, 26.1.1, 26.1.2 | `lucentclient-<version>+26.1.2.jar` |
| 26.2 | `lucentclient-<version>+26.2.jar` |
| 26.3 | `lucentclient-<version>+26.3.jar` |

### Launcher

Download it from [Releases](https://github.com/mojunseo/Lucentclient/releases): the `.exe` or `.msi` for Windows, the `.dmg` for macOS, or the `.AppImage`, `.deb` or `.rpm` for Linux.

The launcher signs in with your Microsoft account, then installs the game, Fabric, Java and Lucent Client on its own. Pick any Minecraft version from 1.19 on, and add mods from Modrinth: your mod list is shared by every version, and the right file for each version is downloaded when you play.

> Microsoft sign-in is waiting for Mojang to approve the launcher, so the launcher can't start the game yet. Until then, use the mod jar with any launcher. The launcher isn't code-signed yet: on Windows choose "More info, Run anyway"; on macOS right-click the app and choose Open.

## Build

Run Gradle with JDK 25 or newer; it downloads the other JDKs it needs.

```sh
./gradlew build               # every Minecraft version, into versions/<version>/build/libs/
./gradlew buildAndCollect     # collect all jars into build/libs/<mod version>/
./gradlew :26.3:runClient     # start a development client for one version
tools/mixin_check.sh          # start each version once and check every mixin applies
```

The mod is built for several Minecraft versions from one source tree with [Stonecutter](https://github.com/kikugie/stonecutter). The builds and the versions each one runs on are listed in `stonecutter.properties.toml`. Code that differs between versions lives in `client/compat/Mc.java` or behind `//? if` comments. The committed source is in the 26.3 state; switch back before committing if you switched versions.

The launcher is in [`launcher/`](launcher/README.md).

## Project layout

- `src/main`: the mod (client only)
- `versions/<version>`: per-version build directories made by Stonecutter
- `launcher/`: the desktop launcher (Tauri)
- `tools/`: texture and icon generators, the mixin check

## License

[Apache-2.0](LICENSE)
