# Changelog

All notable changes to this project will be documented in this file.

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

[0.1.0-alpha.2]: https://github.com/darksoon/RadioWave/releases/tag/v0.1.0-alpha.2
[0.1.0-alpha.1]: https://github.com/darksoon/RadioWave/releases/tag/v0.1.0-alpha.1
