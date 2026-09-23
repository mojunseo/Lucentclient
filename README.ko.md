<p align="center"><img src="launcher/src-tauri/icons/128x128.png" width="96" alt=""></p>

# Lucent Client

[English](README.md) | **한국어** | [日本語](README.ja.md)

Minecraft: Java Edition용 Fabric 클라이언트 모드와 전용 데스크톱 런처입니다.

## 기능

게임 안에서 **오른쪽 Shift**를 누르면 메뉴가 열립니다. 모듈의 램프를 누르면 켜고 끄고, 나머지 부분을 누르면 설정 화면이 열립니다.

- **HUD**: FPS, 좌표, 키 입력 표시와 CPS, 갑옷과 내구도, 효과, 핑, 시계. 위치 편집 화면에서 원하는 곳으로 드래그하고, 모듈마다 글자 색, 배경, 그림자, 크기를 바꿀 수 있습니다.
- **PvP**: 리치 표시, 토글 달리기와 웅크리기, 줌(줌 중 휠로 배율 조절), 걷기·달리기·신속·비행마다 따로 조절하는 FOV 체인저.
- **성능**: 거리에 따른 엔티티와 블록 엔티티 컬링, 파티클 줄이기, 비와 눈 숨기기.
- **기타**: 풀브라이트, 디스코드 Rich Presence.
- **치장품**: 천처럼 움직이는 망토, 깃털·드래곤·나비 날개, 모자, 헤일로. 지금은 자신에게만 보입니다.

## 설치

### 모드

Minecraft **26.1 ~ 26.3**을 지원합니다.

1. [Fabric Loader](https://fabricmc.net/use/) 0.19.5 이상과 [Fabric API](https://modrinth.com/mod/fabric-api)를 설치합니다.
2. [Releases](https://github.com/mojunseo/Lucentclient/releases)에서 사용하는 Minecraft 버전에 맞는 jar를 받아 `mods` 폴더에 넣습니다.

| Minecraft | 파일 |
|---|---|
| 26.1, 26.1.1, 26.1.2 | `lucentclient-<버전>+26.1.2.jar` |
| 26.2 | `lucentclient-<버전>+26.2.jar` |
| 26.3 | `lucentclient-<버전>+26.3.jar` |

### 런처

[Releases](https://github.com/mojunseo/Lucentclient/releases)에서 받으세요. Windows는 `.exe` 또는 `.msi`, macOS는 `.dmg`, Linux는 `.AppImage`, `.deb`, `.rpm`입니다.

런처는 Microsoft 계정으로 로그인한 뒤 게임, Fabric, Java, Lucent Client를 알아서 설치합니다. 1.19 이상의 모든 Minecraft 버전을 고를 수 있고, Modrinth에서 모드를 추가할 수 있어요. 모드 목록은 모든 버전이 함께 쓰고, 플레이할 때 버전에 맞는 파일을 받아 넣습니다.

> Microsoft 로그인은 Mojang의 런처 승인을 기다리는 중이라, 아직 런처로 게임을 켤 수 없습니다. 그동안은 모드 jar를 다른 런처에서 사용하세요. 런처는 아직 코드 서명이 없습니다. Windows에서는 "추가 정보 → 실행"을, macOS에서는 앱을 우클릭한 뒤 "열기"를 누르세요.

## 빌드

JDK 25 이상으로 Gradle을 실행하세요. 다른 버전에 필요한 JDK는 Gradle이 알아서 받습니다.

```sh
./gradlew build               # 모든 Minecraft 버전 빌드 → versions/<버전>/build/libs/
./gradlew buildAndCollect     # 모든 jar를 build/libs/<모드 버전>/ 에 모으기
./gradlew :26.3:runClient     # 특정 버전으로 개발용 클라이언트 실행
tools/mixin_check.sh          # 버전마다 한 번씩 실행해서 모든 mixin이 적용되는지 확인
```

[Stonecutter](https://github.com/kikugie/stonecutter)로 소스 한 벌에서 버전마다 jar를 만듭니다. 빌드 목록과 각 빌드가 실행되는 버전은 `stonecutter.properties.toml`에 있습니다. 버전마다 다른 코드는 `client/compat/Mc.java`나 `//? if` 조건 주석에 있습니다. 커밋되는 소스는 26.3 상태이니, 다른 버전으로 전환해서 작업했다면 커밋 전에 되돌리세요.

런처는 [`launcher/`](launcher/README.md)에 있습니다.

## 구조

- `src/main`: 모드 코드 (클라이언트 전용)
- `versions/<버전>`: Stonecutter가 만드는 버전별 빌드 폴더
- `launcher/`: 데스크톱 런처 (Tauri)
- `tools/`: 텍스처와 아이콘 생성기, mixin 검사

## 라이선스

[Apache-2.0](LICENSE)
