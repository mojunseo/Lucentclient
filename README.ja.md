<p align="center"><img src="launcher/src-tauri/icons/128x128.png" width="96" alt=""></p>

# Lucent Client

[English](README.md) | [한국어](README.ko.md) | **日本語**

Minecraft: Java Edition 向けの Fabric クライアント MOD と、専用のデスクトップランチャーです。

## 機能

ゲーム内で **右 Shift** を押すとメニューが開きます。モジュールのランプを押すとオン／オフ、行のそれ以外の部分を押すと設定画面が開きます。

- **HUD**: FPS、座標、キー入力表示と CPS、防具と耐久値、ステータス効果、Ping、時計。レイアウト編集画面で好きな位置にドラッグでき、モジュールごとに文字色、背景、影、サイズを変更できます。
- **PvP**: リーチ表示、ダッシュとスニークのトグル、ズーム（ズーム中にホイールで倍率を変更）、歩き・ダッシュ・移動速度上昇・飛行ごとに調整できる FOV チェンジャー。
- **パフォーマンス**: 距離によるエンティティとブロックエンティティのカリング、パーティクルの削減、雨と雪の非表示。
- **その他**: 明るさ最大化（フルブライト）、Discord Rich Presence。
- **コスメ**: 布のように揺れるマント、羽・ドラゴン・蝶の翼、帽子、ヘイロー。現在は自分にだけ表示されます。

## インストール

### MOD

Minecraft **26.1〜26.3** に対応しています。

1. [Fabric Loader](https://fabricmc.net/use/) 0.19.5 以降と [Fabric API](https://modrinth.com/mod/fabric-api) をインストールします。
2. [Releases](https://github.com/mojunseo/Lucentclient/releases) から使用する Minecraft のバージョンに合った jar をダウンロードし、`mods` フォルダに入れます。

| Minecraft | ファイル |
|---|---|
| 26.1, 26.1.1, 26.1.2 | `lucentclient-<バージョン>+26.1.2.jar` |
| 26.2 | `lucentclient-<バージョン>+26.2.jar` |
| 26.3 | `lucentclient-<バージョン>+26.3.jar` |

### ランチャー

[Releases](https://github.com/mojunseo/Lucentclient/releases) からダウンロードしてください。Windows は `.exe` または `.msi`、macOS は `.dmg`、Linux は `.AppImage`、`.deb`、`.rpm` です。

ランチャーは Microsoft アカウントでサインインした後、ゲーム、Fabric、Java、Lucent Client を自動でインストールします。1.19 以降の任意の Minecraft バージョンを選べ、Modrinth から MOD を追加できます。MOD リストはすべてのバージョンで共通で、プレイ時にそのバージョン用のファイルをダウンロードします。

> Microsoft サインインは Mojang によるランチャーの承認待ちのため、現在ランチャーからゲームを起動することはできません。それまでは MOD の jar を他のランチャーでご利用ください。ランチャーはまだコード署名されていません。Windows では「詳細情報 → 実行」を、macOS ではアプリを右クリックして「開く」を選んでください。

## ビルド

JDK 25 以降で Gradle を実行してください。他のバージョンに必要な JDK は Gradle が自動でダウンロードします。

```sh
./gradlew build               # すべての Minecraft バージョンをビルド → versions/<バージョン>/build/libs/
./gradlew buildAndCollect     # すべての jar を build/libs/<MOD バージョン>/ に集める
./gradlew :26.3:runClient     # 指定したバージョンで開発用クライアントを起動
tools/mixin_check.sh          # バージョンごとに一度起動し、すべての mixin が適用されるか確認
```

[Stonecutter](https://github.com/kikugie/stonecutter) を使い、1 つのソースからバージョンごとに jar をビルドしています。ビルドの一覧と各ビルドが動作するバージョンは `stonecutter.properties.toml` にあります。バージョンによって異なるコードは `client/compat/Mc.java` か `//? if` 条件コメントにあります。コミットされるソースは 26.3 の状態です。別のバージョンに切り替えて作業した場合は、コミット前に戻してください。

ランチャーは [`launcher/`](launcher/README.md) にあります。

## 構成

- `src/main`: MOD 本体（クライアント専用）
- `versions/<バージョン>`: Stonecutter が作るバージョン別のビルドフォルダ
- `launcher/`: デスクトップランチャー（Tauri）
- `tools/`: テクスチャとアイコンの生成スクリプト、mixin チェック

## ライセンス

[Apache-2.0](LICENSE)
