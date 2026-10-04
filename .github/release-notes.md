**한국어** · [English](#english) · [日本語](#日本語)

Lucent Client는 Minecraft: Java Edition용 Fabric 클라이언트 모드와 데스크톱 런처입니다. 기능은 [README](https://github.com/mojunseo/Lucentclient#readme)를 참고하세요.

### 이번 버전에서 바뀐 것
- **모드 화면을 둘로 나눴습니다**: 왼쪽에는 설치한 모드, 오른쪽에는 Modrinth 검색이 함께 보여서 더 이상 아래로 오르내리지 않아도 됩니다.

v0.1.3에서 바뀐 것:
- **런처 디자인을 새로 그렸습니다**: 유리판(글래스모피즘) 화면에, 보라·시안·핑크 오로라가 떠다니는 밤하늘 배경(별, 떠있는 블록, 블록 스카이라인, 빛나는 해)을 깔았습니다. 메뉴는 화면 위쪽 가운데로 옮기고, 화면 전환과 목록에 움직임을 더했습니다.
- **서버 목록 맨 위에 PurityMC(puritymc.kr)가 자동으로 고정**됩니다. 지울 수 없어요.

v0.1.2에서 바뀐 것:
- **런처 자동 업데이트**: 이제 런처가 켜질 때 새 버전을 스스로 받아 설치하고 다시 시작합니다. 모드도 함께 새 버전으로 바뀌어요. (Linux .deb, .rpm 설치본은 새 버전 안내만 표시합니다.) v0.1.1 이하를 쓰고 있다면 이번 한 번만 직접 받아 주세요.

v0.1.1에서 바뀐 것:
- **경량 모드**: 저사양 PC용 모듈. 켜 있는 동안 그래픽 설정을 가장 가볍게 하고, 끄면 원래 설정으로 돌아갑니다.
- **런처가 성능 모드를 함께 설치**: Sodium, Lithium, FerriteCore, ImmediatelyFast, Entity Culling, More Culling을 버전에 맞춰 설치합니다. 설정에서 끌 수 있어요.
- 26.1, 26.2에서 날개·모자·헤일로를 끼면 캐릭터에 큰 덧씌움이 생기던 문제 수정 (3D Skin Layers와 함께 쓸 때)
- Windows에서 디스코드 상태 표시가 뜨지 않던 문제 수정
- 갑옷, 효과 HUD가 비어 있을 때 빈 상자가 남던 문제 수정
- 새 로고

### 모드
Minecraft 26.1 ~ 26.3을 지원합니다. Fabric Loader 0.19.5 이상과 Fabric API를 설치하고, 버전에 맞는 jar를 `mods` 폴더에 넣으세요. 게임에서 오른쪽 Shift를 누르면 메뉴가 열립니다.

| Minecraft | 파일 |
|---|---|
| 26.1, 26.1.1, 26.1.2 | `lucentclient-*+26.1.2.jar` |
| 26.2 | `lucentclient-*+26.2.jar` |
| 26.3 | `lucentclient-*+26.3.jar` |

### 런처
Windows: `*_x64-setup.exe` 또는 `.msi` · macOS(Apple Silicon과 Intel): `*_universal.dmg` · Linux: `.AppImage`, `.deb`, `.rpm`

런처는 Microsoft 계정으로 로그인해서 게임을 바로 실행합니다. 런처는 코드 서명이 없습니다. Windows에서는 "추가 정보 → 실행"을, macOS에서는 앱을 우클릭한 뒤 "열기"를 누르세요.

---

### English

Lucent Client is a Fabric client mod for Minecraft: Java Edition, with a desktop launcher. See the [README](https://github.com/mojunseo/Lucentclient/blob/main/README.en.md) for features.

**What's new**: the Mods screen is now split in two — installed mods on the left, Modrinth search on the right — side by side instead of stacked.

**From v0.1.3**: the launcher has a new look — glassmorphic panels over a night-sky background with a drifting violet/cyan/pink aurora, stars, floating blocks, a block skyline and a glowing sun. The menu moved to the top of the window and is centered, and screens and lists now animate. PurityMC (puritymc.kr) is now pinned to the top of your server list automatically and can't be removed.

**From v0.1.2**: the launcher now updates itself: when it starts it downloads and installs a new release and restarts, and the mod updates with it (Linux .deb and .rpm installs only show a notice). If you have v0.1.1 or older, download this version by hand once.

**From v0.1.1**: Lightweight Mode (the lightest graphics settings while it's on, your own settings back when it's off); the launcher installs Sodium, Lithium, FerriteCore, ImmediatelyFast, Entity Culling and More Culling for your version (can be turned off in Settings); fixed a large overlay on the player with wings, hats or halos on 26.1 and 26.2 alongside 3D Skin Layers; fixed Discord status on Windows; empty armor and effect boxes are no longer drawn; new logo.

**Mod**: supports Minecraft 26.1 to 26.3. Install Fabric Loader 0.19.5 or newer and Fabric API, then put the jar for your version into `mods` (see the table above). Press Right Shift in game to open the menu.

**Launcher**: Windows `*_x64-setup.exe` or `.msi`, macOS (Apple silicon and Intel) `*_universal.dmg`, Linux `.AppImage`, `.deb` or `.rpm`.

The launcher signs in with your Microsoft account and starts the game. It isn't code-signed: on Windows choose "More info, Run anyway"; on macOS right-click the app and choose Open.

---

### 日本語

Lucent Client は Minecraft: Java Edition 向けの Fabric クライアント MOD とデスクトップランチャーです。機能は [README](https://github.com/mojunseo/Lucentclient/blob/main/README.ja.md) をご覧ください。

**今回の変更**: MOD画面を左右に分けました。左に導入済みMOD、右に Modrinth 検索が並んで表示され、上下にスクロールしなくて済みます。

**v0.1.3 の変更**: ランチャーのデザインを一新しました。ガラス風(グラスモーフィズム)のパネルに、紫・シアン・ピンクのオーロラが漂う夜空の背景(星、浮かぶブロック、ブロックのスカイライン、光る太陽)を重ねています。メニューはウィンドウ上部中央に移動し、画面切り替えやリストにも動きを加えました。サーバー一覧には PurityMC(puritymc.kr)が自動で一番上に固定されるようになり、削除できません。

**v0.1.2 の変更**: ランチャーが自動で更新されるようになりました。起動時に新しいリリースをダウンロード・インストールして再起動し、MOD も一緒に更新されます（Linux の .deb、.rpm 版はお知らせのみ表示）。v0.1.1 以前をお使いの場合は、今回だけ手動でダウンロードしてください。

**v0.1.1 の変更**: 軽量モードを追加（オンの間はグラフィック設定を最も軽くし、オフで元の設定に戻ります）。ランチャーが Sodium、Lithium、FerriteCore、ImmediatelyFast、Entity Culling、More Culling をバージョンに合わせて一緒にインストールします（設定でオフにできます）。26.1・26.2 で 3D Skin Layers と併用時に翼・帽子・ヘイローを付けるとプレイヤーに大きな重なりが出る問題を修正。Windows で Discord のステータスが表示されない問題を修正。防具・効果 HUD が空のとき空の枠が残る問題を修正。新しいロゴ。

**MOD**: Minecraft 26.1〜26.3 に対応しています。Fabric Loader 0.19.5 以降と Fabric API をインストールし、バージョンに合った jar を `mods` フォルダに入れてください（ファイルは上の表をご覧ください）。ゲーム内で右 Shift を押すとメニューが開きます。

**ランチャー**: Windows は `*_x64-setup.exe` または `.msi`、macOS（Apple シリコンと Intel）は `*_universal.dmg`、Linux は `.AppImage`、`.deb`、`.rpm` です。

ランチャーは Microsoft アカウントでサインインしてゲームを起動します。ランチャーはコード署名されていません。Windows では「詳細情報 → 実行」を、macOS ではアプリを右クリックして「開く」を選んでください。
