# Lucent Client

Minecraft 클라이언트 모드 (Fabric, Minecraft 26.3).

## 지원 버전

Minecraft 26.1 ~ 26.3 (Fabric). 버전별 빌드와 호환 범위는 `stonecutter.properties.toml`에 있습니다.
[Stonecutter](https://github.com/kikugie/stonecutter)로 소스 한 벌에서 버전마다 jar를 만들고,
버전마다 다른 API는 `client/compat/Mc.java`와 `//? if` 조건 주석으로 처리합니다.

## 빌드 / 실행

JDK 25 이상으로 Gradle을 실행하세요. 다른 버전에 필요한 JDK는 Gradle이 알아서 받습니다.

```sh
./gradlew build               # 모든 버전 빌드 → versions/<버전>/build/libs/
./gradlew buildAndCollect     # 모든 jar를 build/libs/<모드 버전>/ 에 모으기
./gradlew :26.3:runClient     # 특정 버전으로 개발용 클라이언트 실행
./gradlew "Set active project to 26.2"   # 편집기에서 보는 소스를 다른 버전으로 전환
```

기본(커밋되는) 소스 상태는 26.3입니다. 다른 버전으로 전환해서 작업했다면 커밋 전에 26.3으로 되돌리세요.

## 구조

- `src/main` — 모드 코드 (클라이언트 전용)
- `versions/<버전>` — Stonecutter가 만드는 버전별 빌드 폴더
- `launcher/` — 데스크톱 런처 (Tauri)
- `tools/` — 치장품 텍스처 생성 스크립트

## 라이선스

Apache-2.0
