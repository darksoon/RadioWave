# RadioWave Roadmap

[Deutsch](ROADMAP.de.md) | [English](ROADMAP.md)

Status: 2026-03-12

This roadmap is intended for GitHub and keeps the public product status compact.

## Done

- Core architecture (`core-model`, `core-database`, `core-network`, `core-data`)
- Stable player foundation (`core-player`) with hardened background playback
- Home, browse/search, favorites, fullscreen player, and settings base
- Bottom navigation, stable routing, and floating mini-player
- Marquee metadata in the mini-player for long titles
- Favorites and recents persisted in Room
- Fullscreen player controls: play/pause, mute, random, previous, favorite
- Live progress animation in the fullscreen player
- Stream quality visible in the fullscreen player
- Album art in the fullscreen player (iTunes API, blur background, station logo)
- Home, browse, and favorites UI polish and performance tuning
- Security baseline hardened (`allowBackup=false`, HTTP stream compatibility)
- Optional HTTP indicator in browse and settings toggle for insecure streams
- Settings category navigation with dedicated detail pages
- Working settings for theme, dynamic colors, default quality, and buffer profile
- Data actions in settings: clear station cache and clear history
- Settings info links: version, GitHub repo, issues, website, Ko-fi
- Local crash-report export with share action and GitHub issue handoff
- Notification media controls including previous/play-pause/next/stop
- Audio focus behavior stabilized for interruptions
- Mobile-data policy and buffer profile applied in the actual player
- Room migrations enabled, no destructive fallback
- Extended unit tests for player recovery and repository flows
- Android Auto base via Media3 `MediaLibraryService`
- Android Auto resume/autoplay improved with last-station restore
- Unified Android Auto player path
- Duplicate media notification removed while Android Auto is connected
- Optional thermal mode in settings
- Android Auto low-load behavior applied automatically while connected
- Android Auto bitrate capped at 128 kbps during car sessions
- First-run info dialog integrated
- In-app update flow via GitHub Releases
- Update settings page with popup toggle, auto-check toggle, manual check, and version state
- Live download progress for updates
- Stable/beta update channel toggle
- Manual signed release workflow with version metadata in `gradle.properties`
- GPL-3.0-or-later licensing with SPDX headers for Kotlin sources
- Android Auto dev-mode guide linked in app and docs
- Launcher quick actions (`Search`, `Favorites`, `Player`, `Settings`)
- Android Auto browse/player polish: localized labels, quick access, stronger search, prev/next
- DE/EN language basis with in-app language selection
- CI and release pipelines hardened, including configuration-cache-safe test gating
- Release/distribution split for GitHub APK (`github`) and Play Store App Bundle (`play`)
- Android package/applicationId migrated to `de.darksoon.radiowave`
- GitHub Actions workflows prepared for the Node 24 migration

## In Progress

- Voice and Assistant play intents for station launch
- Google Play Closed Testing preparation

## Planned Next

- Radio timeshift beyond MVP (longer buffer, optional seek/live in fullscreen)
- Sleep timer (15/30/60/off)
- Share action in the fullscreen player
- Podcasts MVP as a dedicated area
- Favorites export/import (JSON)
- Custom stations and M3U/PLS import
- Visible data-usage estimate based on bitrate
- Play Store readiness: native debug symbols for App Bundle crash/ANR diagnostics

## Later

- Further Android Auto expansion (recommendations, more car-specific UX)
- Android TV / Google TV base (Leanback launcher, D-pad/focus navigation, TV layout MVP)
- Chromecast expansion
- Quick-access playback widget
- More transitions, motion, and shimmer
- More unit and UI tests

## Maybe

- Wear OS companion:
  - remote/companion use case rather than full radio client
  - focus on play/pause, favorites, recents, and player status
- Separate podcast source:
  - Radio Browser is station-focused, not episode-focused
  - podcasts likely need a dedicated provider
- Cache DB for browse/search:
  - `cached_stations` as central Room source
  - cache-first flow with local emit plus network refresh
  - TTL metadata via `search_cache_meta`
  - clean links between cache, favorites, and recents
- Optional volume slider in addition to mute

## Note

- This roadmap prioritizes user value and stability.
- Priorities may change based on testing feedback and device behavior.
