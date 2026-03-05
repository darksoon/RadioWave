# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added
- Optional thermal mode setting (`Hitzemodus`) for car/charging scenarios in settings.
- Optional short-drop timeshift guard setting (`Netzausfall-Puffer (MVP)`) in settings.
- Active stream quality selection pipeline wired to `Standard-Qualitaet` setting (`Auto/Niedrig/Mittel/Hoch`).
- First-run info dialog with key setup hints (Android Auto profile, battery optimization, update flow).
- GitHub release updater flow: update check, release-notes dialog, APK download and installer handoff.
- New dedicated `Updates` settings section: popup toggle, automatic check toggle, manual check action, installed/latest version status.
- Update dialog now shows live APK download progress (MB + percentage + progress bar).
- Added Android Auto developer-mode guidance for sideload/beta builds (in-app + docs).
- Android Auto quick action in Settings now includes robust fallbacks (settings intent + app details) for devices without launcher entry.
- i18n foundation added for DE/EN in app and settings resources (system-language based selection).
- Settings update section texts are now resource-driven (localized DE/EN).
- Android Auto developer-mode documentation is now bilingual (DE/EN).

### Fixed
- Android Auto now uses the same session player as the app player (removed split fallback-player path).
- Resolved Android Auto resume case where UI looked active but no audible playback started until manual station switch.
- Removed duplicate concurrent media notifications while Android Auto is connected (single active transport notification path).
- Hardened cloud backup rules by excluding SharedPreferences in `data_extraction_rules.xml`.

### Improved
- Thermal mode now enforces small player buffer profile and throttles metadata/artwork updates to reduce runtime load.
- Android Auto now enables low-load behavior automatically while connected (no manual toggle required).
- Timeshift guard now enforces large buffer profile and extends buffering-stall watchdog for short connection drops.
- Refactored player recovery unit tests away from private reflection access to explicit test hooks.
- Android Auto playback now enforces a hard bitrate cap at `128 kbps` while connected.

## [0.1.0-alpha.6] - 2026-03-03

### Highlights
- First Android Auto integration via Media3 `MediaLibraryService` with Favorites/Recents browsing.
- Auto playback handoff stabilized (item mapping, URI fallback, startup retry, reconnect resume).
- Last played station persistence + optional autoplay on Android Auto connect.
- Android Auto browse/search expanded with Top Stations + Genres and improved search reliability.

### Added
- **Android Auto Media Service** (`RadioWaveAutoService`) with:
  - Root library + Favorites/Recents/Top Stations/Genres nodes.
  - Station metadata including favicon artwork for car UI.
  - Media item resolution from `mediaId`, `uri`, and request metadata fallback.
- **Auto Resume Settings**:
  - New setting: `Autoplay bei Android Auto Verbindung` (default: enabled).
  - Last station is persisted (`uuid`, `name`, `streamUrl`, `favicon`, `country`) for resume.
- **Automotive manifest setup**:
  - Car app descriptor + media service registration in app manifest.
- **Android Auto Search Flow**:
  - Media3 `onSearch` + `onGetSearchResult` wiring for car search integration.
  - Query normalization for quoted queries and robust fallback behavior.
- **Android Auto Metadata/Commands**:
  - Extended metadata composition for station rows (country/codec/bitrate when available).
  - Next/Previous media commands mapped to adjacent favorites.

### Fixed
- Auto favorite selection no longer stalls in endless loading path.
- Resolved Android Auto resume edge case where playback only worked after manually opening app on phone.
- Added retry verification for auto-start to reduce silent-start race conditions after reconnect.
- Fixed Android Auto search loading loop on cache miss by adding direct search fallback and explicit no-result item.
- CI required check issue addressed by running `build-lint-test` on `push` to `main`.
- Signed release workflow improved with explicit missing-secret errors and more robust `apksigner` lookup.

## [0.1.0-alpha.5] - 2026-03-01

### Highlights
- Nebula background image integrated as app atmosphere layer (Home, Browse, Favorites).
- Settings reworked to category navigation with dedicated detail pages.
- Home favorites carousel tuned (smaller cards, stronger center focus).
- Carousel snapping now reliably reaches first/last items.

### Added
- **iTunes Cover-Art Integration**: Album-Cover werden automatisch von der iTunes Search API geladen und im Fullscreen-Player angezeigt.
  - Dynamischer Blur-Hintergrund vom Album-Cover (80dp Blur-Effekt)
  - Großes Cover-Bild (600x600px) im Player
  - Station-Logo (16dp) wird vor dem Stationsnamen angezeigt
  - In-Memory Cache verhindert wiederholte API-Aufrufe für denselben Song
  - Fallback-Kette: iTunes Cover → Stream-Metadata Cover → Station-Logo
  - Kostenlos, kein API-Key erforderlich
- **Nebula Background Asset**: Echtes Nebula-Bild als visueller Hintergrund integriert (mit dezenten Overlays/Stars).
- **Settings Kategorien-Navigation**: Settings als Kategorie-Startseite mit separaten Detailseiten fuer bessere Uebersicht.

### Fixed
- Playback no longer keeps running after app removal from recents (`onTaskRemoved` stop path in foreground playback service).
- Removed dead/unused `RadioPlayerService` branch to avoid parallel playback architecture drift.
- Replaced destructive Room fallback with explicit migrations (`1->2`, `2->3`) and enabled schema export/versioning.
- Stabilized audio focus behavior to prevent instant stop/pause loops on play.
- Favorites-Karussell Snap-Verhalten verbessert: Center-Snap funktioniert nun robust bis zum ersten/letzten Item.
- Fixed unit test compilation errors in `PlayerControllerImplRecoveryTest`:
  - Changed `MainDispatcherRule` from `private class` to `public class` to match public property visibility.
  - Removed invalid override of final `getSystemService` method in anonymous `ContextWrapper`.
- Fixed unit test runtime error "Method getSystemService in android.content.Context not mocked":
  - Added Robolectric test framework for Android context simulation in unit tests.
  - Added `@RunWith(RobolectricTestRunner::class)` and `@Config(sdk = [34])` annotations.
  - Replaced manual `ContextWrapper` mock with `ApplicationProvider.getApplicationContext()`.

### Improved
- Network callback lifecycle now pause-aware in player control flow.
- API client timeout tuned down for faster failure feedback.
- HTTP logging interceptor now debug-only wiring.
- Home UI weiter poliert (Header reduziert, Karussell kompakter).
- Hintergrund-Rendering vereinheitlicht fuer Home/Browse/Favoriten.
- Added targeted unit tests for player recovery paths (`scheduleReconnect`, playback-lost recovery, buffering watchdog).
- Added targeted repository flow tests for favorites merge/toggle/reorder and station DTO mapping behavior.

### Dependencies
- Added Robolectric 4.14.1 for Android unit testing support.
- Added androidx.test:core 1.6.1 for `ApplicationProvider` in tests.

## [0.1.0-alpha.3] - 2026-02-25

### Added
- Extended settings architecture with dedicated `SettingsViewModel`.
- New settings options including insecure streams toggle and improved user controls.
- HTTP badge/visibility improvements in browse results.

### Fixed
- Restored HTTP stream compatibility and station visibility issues.
- Stabilized playback start and reduced idle-loop/logging overhead.
- Decoupled startup recovery from initial load for more reliable app start behavior.
- Cleanup in home collectors to reduce duplicate updates.

### Improved
- Settings screen expanded and reorganized for better UX.
- Browse/Search flow refined (quick genres + filter behavior polish).
- Security hardening updates (backup/cleartext/debug log gating).
- Public roadmap/readme updates for release transparency.

## [0.1.0-alpha.2] - 2026-02-25

### Fixed
- Stabilized background playback with dedicated `PlaybackForegroundService`.
- Improved screen-off playback behavior and lifecycle handling.
- Kept browse search state stable when toggling favorites.

### Improved
- Reworked browse UI with quick genre chips and collapsible filter section.
- Updated README/support/install documentation and release-signing workflow reliability.

## [0.1.0-alpha.1] - 2026-02-25

### Added
- Initial public alpha release setup for RadioWave.
- Manual GitHub Actions workflow for Android builds (`workflow_dispatch`).
- PR CI workflow (build/lint/test on pull requests).
- Contribution guide, issue templates, and PR template.
- Screenshot gallery and improved project documentation in README.

### Improved
- Home screen polished with clearer sections and denser card layout.
- Subtle animated/glow background for improved visual quality.
- Floating mini-player overlay with improved readability and runtime display.
- Fullscreen player enhancements (mute/unmute, random station, previous station, live progress bar).
- Better back handling: Android back closes player overlay first.

### Playback & Stability
- Increased buffering and stronger network timeout/retry handling.
- Auto-recovery on short network interruptions via network callback.
- Background playback hardening with wake lock and Wi-Fi lock.
- Playback stall watchdog with automatic recovery.
- Added dedicated playback foreground service lifecycle to keep audio stable with screen off.

### Favorites & UX
- Production-ready favorites with Room persistence.
- Clearer favorites interaction and feedback in player/search.
- Home play flow separated from favorites flow.

### Security/Infra
- Added `ACCESS_NETWORK_STATE` permission declaration in `core-player` to satisfy lint and ensure network callback safety.

[0.1.0-alpha.6]: https://github.com/darksoon/RadioWave/releases/tag/v0.1.0-alpha.6
[0.1.0-alpha.5]: https://github.com/darksoon/RadioWave/releases/tag/v0.1.0-alpha.5
[0.1.0-alpha.3]: https://github.com/darksoon/RadioWave/releases/tag/v0.1.0-alpha.3
[0.1.0-alpha.2]: https://github.com/darksoon/RadioWave/releases/tag/v0.1.0-alpha.2
[0.1.0-alpha.1]: https://github.com/darksoon/RadioWave/releases/tag/v0.1.0-alpha.1
