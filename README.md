# Lucent Client

Minecraft 클라이언트 모드 (Fabric, Minecraft 26.3).

## 요구 사항

- JDK 25 이상
- Minecraft 26.3 + Fabric Loader 0.19.5 이상 + Fabric API

## 빌드 / 실행

```sh
./gradlew build        # build/libs/ 에 jar 생성
./gradlew runClient    # 개발용 클라이언트 실행
```

## 구조

- `src/main` — 공통 코드 (`LucentClient`: 모드 ID, 로거)
- `src/client` — 클라이언트 전용 코드와 mixin

## 라이선스

Apache-2.0
