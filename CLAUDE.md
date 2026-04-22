# CLAUDE.md
This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands
- Build debug APK (normal flavor): `./gradlew :app:assembleNormalDebug`
- Build release APK (normal flavor): `./gradlew :app:assembleNormalRelease`
- Build debug APK (python flavor): `./gradlew :app:assemblePythonDebug`
- Install debug APK to device: `./gradlew :app:installNormalDebug`
- Lint: `./gradlew :app:lintNormalDebug`
- Clean: `./gradlew clean`
- Run tests: `./gradlew test`
- Run one unit test class if tests are added later: `./gradlew :app:testNormalDebugUnitTest --tests 'com.example.YourTest'`
- Run instrumented tests if present: `./gradlew :app:connectedNormalDebugAndroidTest`

## Architecture
- Multi-module Android project: `:app`, `:player`, `:quickjs`, `:pyramid`.
- `:app` is the main TVBox app. Most changes land here.
- `:player` carries ExoPlayer/DKPlayer playback dependencies.
- `:quickjs` provides QuickJS/native integration.
- `:pyramid` embeds Python via Chaquopy and is only included in the `python` flavor.

## Build shape
- Main Android config is in `app/build.gradle`.
- Product flavors under `mode`:
 - `normal`: packaged as the `java` flavor in APK naming.
 - `python`: uses `proguard-python.pro`, raises `minSdkVersion` to 19, and adds `:pyramid`.
- Current Gradle config builds only `armeabi-v7a` for both debug and release.
- APK names are emitted as `TVBox_<buildType>-<flavor>.apk`.
- The repo currently has no `app/src/test`, no `app/src/androidTest`, and no explicit test dependencies.

## App flow
- Startup begins in `app/src/main/java/com/github/tvbox/osc/base/App.java`.
 - Initializes Hawk, OkGo/network helpers, EPG, local HTTP server plumbing, Room access, player helpers, and QuickJS.
- `HomeActivity` is the launcher and runtime bootstrap.
 - It starts the local control server, loads source config through `ApiConfig`, then loads external spider JARs if configured.
- `ApiConfig` is the config center.
 - It fetches/parses source config JSON, owns source/parse/live state, normalizes config URLs, caches remote payloads, and coordinates dynamic loader state.
- `SourceViewModel` is the main orchestration layer used by UI screens.
 - It hides differences between XML, JSON, JAR, and remote sources and exposes LiveData for sort/list/search/detail/play results.

## Source/plugin system
- Dynamic crawler code lives under `com.github.catvod.crawler`.
- `JarLoader` downloads and caches external spider JARs, builds `DexClassLoader`s, calls `com.github.catvod.spider.Init.init(Context)`, and instantiates spider classes.
- `JsLoader` provides a similar path for JS-capable spiders backed by QuickJS.
- `ApiConfig.loadJar()` is the main entry for the configured spider package.
- Changes around config parsing, jar caching, spider lookup, or proxy logic often affect startup, search, detail, and playback together.

## UI and playback
- Main screen flow is Activity + Fragment + ViewModel:
 - `HomeActivity` manages source/category navigation.
 - `GridFragment` renders category/folder/video grids and delegates fetching to `SourceViewModel`.
 - `DetailActivity`, `SearchActivity`, `FastSearchActivity`, `PlayActivity`, and `LivePlayActivity` cover detail/search/VOD/live flows.
- Playback centers on `PlayActivity` (VOD) and `LivePlayActivity` (live).
- `MyVideoView` wraps DKPlayer `VideoView`; controllers live under `app/src/main/java/com/github/tvbox/osc/player/controller/`.
- Subtitle parsing/rendering is implemented in-app under `app/src/main/java/com/github/tvbox/osc/subtitle/`.

## Local server and persistence
- The app runs an embedded NanoHTTPD server through `ControlManager` and `RemoteServer`.
- That server handles local proxying, push URLs, DoH forwarding, remote-control pages, and some file/upload operations.
- Persistent flags live mostly in Hawk (`HawkConfig`). Structured local data uses Room under `data/` plus DAO/cache classes under `cache/`.
- Config URLs, downloaded config files, and spider jars are cached in app-private storage; config bugs are often caused by cached state rather than current in-memory objects alone.

## Repository-specific notes
- This codebase is TV-first. Many screens assume D-pad focus and TV RecyclerView behavior; avoid mobile-only interaction changes unless the screen already supports them.
- Playback, proxying, the local server, and dynamic spiders are tightly coupled. Small changes in one layer can break unrelated-looking flows.
- `README.md` is mostly a schema reference for external config JSON (`spider`, `sites`, `parses`, `lives`, `rules`, `doh`, etc.); use it when changing config parsing.
- `pyramid/build.gradle` hardcodes a Windows Python path for Chaquopy: `D:/Programs/Python/Python38/python.exe`. If the `python` flavor fails on another machine, check that first.
