# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

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

[0.1.0-alpha.3]: https://github.com/darksoon/RadioWave/releases/tag/v0.1.0-alpha.3
[0.1.0-alpha.2]: https://github.com/darksoon/RadioWave/releases/tag/v0.1.0-alpha.2
[0.1.0-alpha.1]: https://github.com/darksoon/RadioWave/releases/tag/v0.1.0-alpha.1
