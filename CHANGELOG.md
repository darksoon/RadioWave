# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Docs
- Repository documentation cleaned up and aligned with the current public release state.

## [0.1.0-beta.4-HOTFIX] - 2026-03-12

### Fixed
- Release and project docs refreshed to match the current package name, workflow behavior, and distribution targets.

## [0.1.0-beta.4] - 2026-03-12

### Changed
- Android package / `applicationId` renamed from `de.radiowave` to `de.darksoon.radiowave` for Google Play compatibility.
- Documentation updated to reflect the current release workflow, package name, and Google Play distribution split.

### Improved
- GitHub Actions workflows now use `actions/checkout@v5` and opt into Node 24 ahead of the runner default switch.

## [0.1.0-beta.3-HOTFIX] - 2026-03-10

### Fixed
- Play Store distribution now excludes direct APK installation permission and GitHub sideload updater flow.

### Improved
- Release output split finalized for GitHub APK (`github`) and Play Store App Bundle (`play`).
- Session/release docs clarified the two distribution targets and their intended usage.

## [0.1.0-beta.3] - 2026-03-10

### Added
- Stable/Beta update channel toggle in settings for GitHub release checks.
- Local crash-report export flow with share action and prefilled GitHub issue handoff.

### Fixed
- Settings manual update flow now uses the same in-app APK download/install path as the main update dialog.
- In-app APK installer hardened with better package-installer fallback behavior and clearer error states.
- PR CI fixed after Gradle configuration-cache rollout by making test gating configuration-cache safe.

### Improved
- CI/release workflows now use stronger Gradle caching and configuration cache for faster repeat runs.
- Support links now point to Ko-fi in app and GitHub documentation.
- Project relicensed to GPL-3.0-or-later with SPDX headers on Kotlin sources for open-source distribution.

## [0.1.0-beta.2] - 2026-03-09

### Added
- Launcher quick actions via app-icon long press (`Search`, `Favorites`, `Player`, `Settings`).
- Runtime shortcut sync for better launcher compatibility on OEM devices.

### Fixed
- Prevented app startup crash on devices that reject dynamic manipulation of manifest-defined launcher shortcuts.

### Improved
- Playback notification polished with cleaner media controls and direct player open action.
- Notification labels/status texts localized and metadata presentation improved.
- Android Auto browse labels and empty states localized for DE/EN.
- Android Auto `Quick Access` now combines favorites and recents instead of mirroring favorites only.
- Android Auto search ranking improved with combined local/remote results and broader token/country/tag matching.
- Android Auto player controls now expose working previous/next navigation for queue-capable in-car playback.
- Manual signed release workflow now sources app version metadata from `gradle.properties`.

## [0.1.0-beta.1] - 2026-03-05

### Highlights
- First public **beta baseline** after alpha phase hardening.
- Full **GitHub in-app auto-update flow** (check, notes, download with progress, installer handoff).
- Android Auto playback path stabilized further for real in-car scenarios.
- App language stack stabilized with DE/EN resources and in-app language selection.
- Search responsiveness improved with local cache-first behavior and lighter logo rendering.

### Added
- **Auto-Update System (GitHub Releases)**:
  - In-app update check against GitHub releases.
  - Release-notes dialog before update.
  - APK download + installer handoff from app.
  - Live download progress (MB, %, progress bar).
  - Dedicated `Updates` settings area:
    - Popup on/off
    - Automatic check on/off
    - Manual update check
    - Installed vs latest release status
- First-run onboarding/info dialog with setup hints (Android Auto, battery, updates).
- Optional thermal mode (`Hitzemodus`) in settings.
- Optional short-drop timeshift guard (`Netzausfall-Puffer (MVP)`) in settings.
- Active stream quality selection wired to `Standard-Qualitaet` (`Auto/Niedrig/Mittel/Hoch`).
- Android Auto dev-mode guide integrated in-app and in docs (DE/EN).
- In-app language selector (`System`, `Deutsch`, `English`) with immediate apply.

### Fixed
- Android Auto uses one shared session player path (no split fallback path).
- Fixed Android Auto resume edge case: playback appeared active but remained silent until manual switch.
- Removed duplicate media notifications while Android Auto is connected.
- Hardened cloud backup rules by excluding SharedPreferences in `data_extraction_rules.xml`.
- Fixed startup crash after locale migration by aligning `MainActivity` (`AppCompatActivity`) with AppCompat theme base.
- Stabilized language switching behavior and startup handling.
- Fixed CI blocker in player recovery tests (timeshift-dependent watchdog expectation now deterministic).

### Improved
- Android Auto now enables low-load behavior automatically while connected.
- Android Auto playback enforces bitrate cap at `128 kbps` during car sessions.
- Timeshift guard now enforces large buffer profile and longer buffering-stall watchdog.
- Thermal mode now enforces small buffer profile and throttles metadata/artwork updates.
- Android Auto browse flow mirrors favorites in former recents slot for faster in-car access.
- Search flow now uses local DB-first emission + network refresh merge for smoother scrolling/result stability.
- Station logo rendering optimized to reduce scroll jank in browse/search grids.
- UI direction consolidated to dark-focused operation (stable readability baseline for beta).

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

[0.1.0-beta.1]: https://github.com/darksoon/RadioWave/releases/tag/v0.1.0-beta.1
[0.1.0-beta.2]: https://github.com/darksoon/RadioWave/releases/tag/v0.1.0-beta.2
[0.1.0-beta.3]: https://github.com/darksoon/RadioWave/releases/tag/v0.1.0-beta.3
[0.1.0-beta.3-HOTFIX]: https://github.com/darksoon/RadioWave/releases/tag/v0.1.0-beta.3-HOTFIX
[0.1.0-beta.4]: https://github.com/darksoon/RadioWave/releases/tag/v0.1.0-beta.4
[0.1.0-beta.4-HOTFIX]: https://github.com/darksoon/RadioWave/releases/tag/v0.1.0-beta.4-HOTFIX
[0.1.0-alpha.6]: https://github.com/darksoon/RadioWave/releases/tag/v0.1.0-alpha.6
[0.1.0-alpha.5]: https://github.com/darksoon/RadioWave/releases/tag/v0.1.0-alpha.5
[0.1.0-alpha.3]: https://github.com/darksoon/RadioWave/releases/tag/v0.1.0-alpha.3
[0.1.0-alpha.2]: https://github.com/darksoon/RadioWave/releases/tag/v0.1.0-alpha.2
[0.1.0-alpha.1]: https://github.com/darksoon/RadioWave/releases/tag/v0.1.0-alpha.1
