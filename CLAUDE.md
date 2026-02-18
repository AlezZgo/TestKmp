# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build Android debug APK
./gradlew :composeApp:assembleDebug

# Run all common tests (Compose UI tests)
./gradlew :composeApp:allTests

# Run JVM-hosted common tests (Compose UI tests)
./gradlew :composeApp:jvmTest

# Run Android unit tests
./gradlew :composeApp:testDebugUnitTest

# Run kakao-kmp unit tests
./gradlew :kakao-kmp:testDebugUnitTest

# Run all tests (kakao-kmp + composeApp)
./gradlew :kakao-kmp:testDebugUnitTest :composeApp:jvmTest

# iOS build — open iosApp/ in Xcode or use run configuration in Android Studio with KMM plugin
```

## Architecture

**Kotlin Multiplatform** project targeting **Android** and **iOS** with shared UI via **Compose Multiplatform**.

### Module Layout

- `composeApp/` — shared app module containing all platform source sets (`commonMain`, `androidMain`, `iosMain`, `jvmMain`, `commonTest`)
- `kakao-kmp/` — KMP testing library: Page Object pattern (`KmpScreen`/`KNode`), FlakySafety retry, Allure JSON reporting. Zero expect/actual, everything in commonMain.
- `iosApp/` — Swift entry point wrapping the shared Compose UI via `UIViewControllerRepresentable`

### Navigation & Component Architecture (Decompose)

The app uses **Decompose** for component-based architecture with stack navigation:

- `RootComponent` / `DefaultRootComponent` — root navigation, owns `ChildStack` with `Config.Home` and `Config.CreateCard` routes
- `HomeComponent` — displays list of cards, triggers navigation to create card
- `CreateCardComponent` — form with validation, returns created card via callback
- State persistence uses `stateKeeper` with `@Serializable` data classes

### Platform Integration

- **Android**: `MainActivity` → `defaultComponentContext()` → `DefaultRootComponent` → `App()`
- **iOS**: `AppDelegate` creates `IosRootHolder` which manages `LifecycleRegistry` (Essenty) and bridges UIKit lifecycle (`onActive`/`onInactive`/`onBackground`) to Decompose lifecycle. `ContentView` wraps `RootViewController` (ComposeUIViewController).

### UI Layer

All UI is in `composeApp/src/commonMain/.../ui/`:
- `RootContent` — observes `ChildStack`, renders current screen
- `HomeContent`, `CreateCardContent` — screen composables using Material3
- `UiTags` — test tag constants used for both UI identification and Compose UI tests

### Testing

Tests are in `commonTest` and run via `jvmTest` task (JVM target). Two test approaches:

- **Legacy:** `CardCreationUiTest` — uses `runComposeUiTest` directly with `Steps.kt` BDD DSL
- **kakao-kmp:** `CardCreationKakaoTest` — uses `kakaoTest {}` DSL with Page Objects (`HomeScreen`, `CreateCardScreen`), flaky-safe actions, Allure JSON reporting

kakao-kmp provides: `KmpScreen` (page object base), `KNode` (typed UI element with `flakySafely` retry), `MatcherBuilder` DSL, `TestScope` with `step()` for Allure steps, `kakaoTest()` entry point. Allure results are generated in `allure-results/` directories.

## Key Dependencies

| Library | Purpose |
|---------|---------|
| Decompose 3.5.0-alpha01 | Component lifecycle, stack navigation |
| Essenty 2.5.0 | Lifecycle, StateKeeper for iOS bridge |
| Compose Multiplatform 1.10.0 | Shared UI framework |
| Material3 1.10.0-alpha05 | UI components |
| kotlinx-serialization 1.6.3 | State persistence serialization |
| kotlinx-io 0.6.0 | Allure JSON file writing (kakao-kmp) |

## Research Process

When writing code or designing features, always use MCP Context7 (`resolve-library-id` → `query-docs`) to fetch up-to-date documentation for libraries before making assumptions about their API. Do not rely on memory — APIs change between versions.

## Conventions

- `expect`/`actual` pattern for `Platform` interface (platform name/version detection)
- Data models are `@Serializable` data classes in `cards/` package
- Component interfaces define `model: Value<Model>` for state and callback functions for events
- Code comments in English, UI strings in Russian
