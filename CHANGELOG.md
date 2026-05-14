# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

## [1.0.5] - 2026-05-10

### Added
- Player error banner with retry button — playback errors (network, broken stream, etc.) are now visible in the fullscreen player instead of showing an endless spinner.
- Custom station URL validation — `Add station` dialog now validates `http(s)://` schemes live with inline error feedback.
- `Retry` button in Favorites is now functional (was a dead handler before).
- Accessibility labels on all fullscreen player controls (favorite, skip-previous, mute, shuffle).

### Fixed
- **Security:** Android Auto service now rejects connections from non-trusted controller packages. URI schemes other than `http`/`https` are blocked end-to-end to prevent injection of local files via `MediaItem`.
- **Crash:** Stations with invalid stream URIs no longer crash the app — surface a `StreamBroken` error in the player instead.
- **Android Auto:** Auto-resume on car connect no longer triggers if a phone call is active (or ringing).
- **Android Auto:** Station switching no longer reverts after a few seconds — replaced serialising mutex with cancellation-token pattern so the newest user intent always wins.
- **Android Auto:** Slow stream start / endless buffering on car cellular fixed — verification heuristic now tolerates `STATE_BUFFERING`/`STATE_READY` instead of restarting playback after 1.8 s. Removed double `prepare()` race between `playStation` and `applyAutoQueue`.
- **Memory:** WakeLock could leak after `release()` if a Player.Listener callback fired post-cancel — `isReleased` flag now guards re-acquisition.
- **Memory:** `PlaybackForegroundService` now calls `startForeground()` before action handling — prevents `ForegroundServiceDidNotStartInTimeException` on Android 12+ after process kill.
- **Memory:** `CastManager.release()` added for clean teardown of session listeners on app shutdown.
- **Memory:** `Player.Listener` is now stored as a field and removed before `ExoPlayer.release()`.
- **UX:** LIVE indicator no longer shown during a player error.
- **UX:** Touch targets in Favorites cards extended to 48 dp via `minimumInteractiveComponentSize()` (visual size unchanged).
- **UX:** Notification permission is now requested only after onboarding completes — no more overlap with the welcome flow.
- **UX:** Language change no longer recreates the activity — `setApplicationLocales` is sufficient. Preserves scroll position, fullscreen player state, etc.
- **UX:** Favorites cards have consistent height again (was inconsistent due to a landscape fix that allowed cards to grow with content).
- **UX:** Empty start card now navigates to top stations instead of a literal "popular" search.
- Removed unused `CHANGE_WIFI_MULTICAST_STATE` permission.
- `LocalIssueReporter` crash handler no longer re-throws — uses `Process.killProcess` fallback to avoid ANR loops.
- Stream URLs are now scrubbed from crash reports before sharing.

### Improved
- **Performance:** Levenshtein-heavy ranking in `HomeViewModel` now runs on `Dispatchers.Default` — keeps the main thread responsive during search/country filtering.
- **Performance:** Carousel scroll transformation moved into `graphicsLayer` block — eliminates per-frame recompositions of the home favorites carousel.
- **Performance:** N+1 DAO query pattern in recent stations replaced with a single `combine()` against the custom stations flow.
- **Performance:** `FavoriteDao.getMaxSortOrder()` added — `toggleFavorite` no longer loads the full favorites list to compute the next sort index.
- **Performance:** `searchStations` DAO converted from `Flow` to `suspend` (only used via `.first()`) — eliminates Room subscription churn on every keystroke.
- **Performance:** `favoriteStationIds` derived directly from the favorites repository instead of from `uiState` — unrelated UI state changes no longer trigger `HashSet` rebuilds.
- **Performance:** ICY metadata regex compiled once (hoisted to companion object) instead of per metadata frame.
- **Performance:** Player settings (`thermalMode`, `timeshiftGuard`) cached with `OnSharedPreferenceChangeListener` instead of reading SharedPreferences on every metadata frame.
- **Performance:** Tags and countries cached in memory with 6-hour TTL.
- **Performance:** Pull-to-refresh debounced to 5-second minimum interval.
- API failures are now logged via `Log.w` instead of silently swallowed.
- `registerClick` / `reportBrokenStream` are now actually called — radio-browser community rankings reflect actual usage.
- Custom stream titles use the typed `IcyInfo.title` field as primary source; fragile `toString()` fallback only as last resort.

## [1.0.4] - 2026-05-01

### Added
- Fullscreen player now adapts to landscape orientation: artwork on the left, controls and metadata on the right.
- Pull-to-refresh in Home and Browse — pull down to reload content at any time.
- Skeleton loader with shimmer animation replaces the plain spinner while Home, Browse, and Favorites load for the first time.
- Onboarding redesigned as a fullscreen experience with per-step icons, ambient glow background, page-dot indicator, and animated transitions.

### Fixed
- Android Auto buffer profile was incorrectly overridden by the timeshift guard, causing the automotive low-load mode to have no effect. The automotive mode now correctly takes priority, reducing CPU load and battery drain during car sessions.
- Browse and Favorites grid now uses adaptive column counts instead of a fixed three-column layout, preventing overcrowded or cut-off grids in landscape orientation.

### Improved
- Empty states for Home, Browse, and Favorites redesigned with icons, glow effects, and contextual call-to-action elements.
- Accessibility: favorite toggle, filter expand/collapse, and play/pause buttons across Browse and Favorites now include descriptive labels for screen readers.
- Wi-Fi lock type downgraded from `WIFI_MODE_FULL_HIGH_PERF` to `WIFI_MODE_FULL` — sufficient for audio streaming, lower power draw.
- Onboarding no longer mentions the GitHub updater, which was removed in a previous update.
- `contentType` hints added to all lazy list and grid items for better Compose scroll performance.
- Favorites card minimum height is now flexible instead of fixed, preventing content clipping on narrow columns in landscape.
- Onboarding layout constrained to a maximum width of 520 dp and centered on wide tablet screens.

### Removed
- GitHub release checker and What's New dialog removed. RadioWave is now distributed exclusively via Google Play; updates are handled by the Play Store.
- GitHub release notes section removed from Settings.

### Added
- Fullscreen player now includes a direct share action for the current station.
- Fullscreen player now includes a built-in sleep timer with quick presets.
- Favorites can now be moved up or sent directly to the top for faster manual ordering.
- Chromecast / Google Cast support now exists as a first MVP with route discovery, direct casting from the player, station handover to TV, and in-app play/pause routing for active cast sessions.

### Fixed
- Playback timer now pauses correctly during buffering instead of continuing through interruptions.
- Player recovery tests were aligned with the updated reconnect/recovery tuning so CI passes again.
- Local phone playback now pauses correctly when a cast session starts or when the station is changed during active casting, preventing parallel playback on phone and TV.
- Dismissing the player while casting now ends the cast session cleanly instead of only stopping local playback state.

### Improved
- Playback recovery is more tolerant of short network interruptions with more reconnect attempts, milder backoff, faster network-return recovery, and slightly larger buffers.
- Release workflow and manual build artifacts now also preserve native debug-symbol outputs for Play Console debugging.
- Player UI now shows when playback is running on TV during an active cast session, instead of looking like ordinary local playback.
- Android Auto browse/search callbacks now avoid blocking repository and network work on the callback path.
- Android Auto previous/next navigation now prefers the active in-car queue before falling back to quick access.
- Playback only holds the WiFiLock while the active network is Wi-Fi, reducing unnecessary radio lock usage on mobile data.
- Public docs now reflect the live Google Play status instead of current beta/closed-test planning.

## [1.0.1a] - 2026-04-09

### Fixed
- Notification and lockscreen media controls now respond reliably again via MediaSession callbacks.
- Play/pause, previous, and next actions in the playback notification were repaired.
- HLS playback crash fixed by bundling the missing `media3-exoplayer-hls` module for `.m3u8` streams.

### Improved
- Android 15 edge-to-edge handling was modernized to align better with current system bar and inset behavior.

## [1.0.0-rc1] - 2026-04-03

### Added
- Restartable multi-step onboarding flow with a compact in-app product tour.
- In-app "What’s new?" dialog plus release-notes access from settings.
- Release readiness audit for the upcoming 1.0 line.

### Changed
- Public README files now point to the dedicated RadioWave website and Google Play listing instead of pinning the project overview to an older beta/hotfix line.
- Public roadmap refreshed for the current RC preparation phase.
- Branding was polished around the existing purple/white app icon, including slogan integration and a refined splash screen.

### Improved
- Android Auto quality handling is now configurable in settings.
- Android Auto metadata prioritizes bitrate more clearly where car surfaces support it.
- Browse scrolling and station-card sizing were polished further.
- Browse and favorites grids now keep more consistent card heights.
- Settings support/community area now uses cleaner inline links and stronger Ko-fi emphasis.

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

[1.0.1a]: https://github.com/darksoon/RadioWave/releases/tag/v1.0.1a
[1.0.0-rc1]: https://github.com/darksoon/RadioWave/releases/tag/v1.0.0-rc1
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
