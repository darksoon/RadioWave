# RadioWave Roadmap

[Deutsch](ROADMAP.de.md) | [English](ROADMAP.md)

Status: 2026-05-10

This roadmap is intended for GitHub and keeps the public product status compact. RadioWave is live on Google Play, so the roadmap prioritizes stability, supportability, and targeted UX polish.

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
- Android Auto quality limit is now configurable in settings for car sessions
- First-run info dialog integrated
- In-app update flow via GitHub Releases
- Update settings page with popup toggle, auto-check toggle, manual check, and version state
- Live download progress for updates
- Stable/beta update channel toggle
- Manual signed release workflow with version metadata in `gradle.properties`
- Release/distribution split for GitHub APK (`github`) and Play Store App Bundle (`play`)
- Android package/applicationId migrated to `de.darksoon.radiowave`
- GitHub Actions workflows prepared for the Node 24 migration
- Android Auto dev-mode guide linked in app and docs
- Launcher quick actions (`Search`, `Favorites`, `Player`, `Settings`)
- Android Auto browse/player polish: localized labels, quick access, stronger search, prev/next
- DE/EN language basis with in-app language selection
- CI and release pipelines hardened
- GPL-3.0-or-later licensing with SPDX headers for Kotlin sources
- Sleep timer (15/30/60/off)
- Share action in the fullscreen player
- Chromecast integration (MVP)
- Favorites reordering (MVP)
- Network recovery and stability improvements
- Player timer pausing during buffering
- Custom stations with stream URL in favorites (MVP)
- Google Play availability with package name `de.darksoon.radiowave`
- Android Auto browse callbacks now run asynchronously instead of blocking
- Android Auto previous/next navigation uses the active in-car queue more reliably
- WiFiLock is only held while the active network is Wi-Fi
- Landscape layout for fullscreen player (artwork left, controls right)
- Landscape-adaptive grid layouts for Browse and Favorites
- Redesigned onboarding as fullscreen experience with icons and animations
- Pull-to-refresh in Home and Browse
- Skeleton loader with shimmer animation in Home, Browse, and Favorites
- Redesigned empty states for Home, Browse, and Favorites with icons and CTAs
- Accessibility labels added to interactive icons across Browse and Favorites
- Automotive mode now correctly takes priority over timeshift guard, reducing battery drain
- Wi-Fi lock type downgraded from high-perf to standard mode for audio streaming
- GitHub updater and What's New dialog removed — Play Store only going forward
- Visible player error banner with retry (network errors, broken streams, etc.)
- Custom station URL validation with live inline error feedback
- Android Auto auto-resume guarded against active phone calls and ringtones
- Android Auto station-switching reverting bug fixed (cancellation-token pattern)
- Android Auto slow-start / endless-buffer bug fixed (buffer-tolerant retry verification, no more double prepare)
- Comprehensive security hardening (AutoService controller whitelist, URI scheme validation, crash-report PII scrubbing)
- Major performance pass: carousel scroll smoothness, N+1 DB queries removed, heavy ranking moved off main thread, in-memory tag/country cache, metadata regex hoisted, settings cached
- Player listener lifecycle and WakeLock release hardened against leaks
- CastManager.release() for clean shutdown
- 48 dp touch targets across Favorites cards (extended 2026-07-09 to the fullscreen player, mini player, and Browse grid)
- Live language change without Activity recreate
- Data-loss migration bug fixed (favorites/recents from before the v1->v3 upgrade had lost their name/URL; repair migration added)
- Free EQ presets (Flat, Bass Boost, Treble Boost, Vocal) — Settings > Sound
- Android Auto: "Continue where you left off" and "Surprise me" tiles on the browse root
- Android Auto: browse tree distinguishes "network error" from "genuinely empty" (Top Stations, Genres, genre browsing)
- Android Auto: fixed a recurring bug where playback kept going on the phone speaker after leaving the car (disconnect-triggered pause could be undone by a reconnect blip during the car's teardown sequence)
- Station artwork URLs validated to http/https before rendering/casting (closes a file://\|content:// injection path)
- Favorite toggling made atomic (rapid double-taps / Android Auto + phone racing could invert the second tap)
- GitHub Actions pinned to commit SHAs; a shell-injection path in the manual build workflow closed
- Settings > Data already has a battery-optimization-exemption tip with a direct system-settings link (found already implemented during the 2026-07-09 quick-wins pass — a research-suggested item turned out to already be shipped, just undocumented here)
- Community popularity signal: station cards now show a 🔥 badge from Radio Browser's public click-count when notably high — no tracking, aggregate public data only
- Favorites export/import (JSON) — additive/merge restore, not destructive, backed by a versioned envelope for forward compatibility
- Custom stations M3U/PLS playlist import, alongside the existing single-URL add flow
- Bitrate-based data usage estimate shown live under the default-quality picker

## In Progress

- Voice and Assistant play intents for station launch
- Live-app stabilization based on real device and user feedback

## Planned Next

- Android Auto smoke test on real head unit
- Harden the "pause on car disconnect" fix with `androidx.car.app`'s `CarConnection` (debounced OS-level car-connection-type signal) instead of inferring disconnect/reconnect purely from MediaSession controller churn + a tuned timeout (`DISCONNECT_RESUME_GUARD_MS`, currently 2.5s) — this bug has now had four separate fix attempts (commits `3df6347`, `b717153`/`131fad5`, and the 2026-07-09 guard rewrite); the current fix is a real improvement but is still a timing heuristic. Needs a new dependency + real head-unit testing to verify before shipping, hence not done inline during the 2026-07-09 pass. Bundle with the smoke-test item above.
- Radio timeshift beyond MVP (longer buffer, optional seek/live)
- Migrate `SharedPreferences` to a single `SettingsRepository` (DataStore) — bigger refactor

## Later

- Further Android Auto expansion — remaining candidates from a 2026-07 competitive review (see Done for what already shipped):
  - Grid template for genre/tag/country browsing instead of only lists (better scannability while driving)
  - Voice-search suggestion chips seeded from local search history (no telemetry)
  - Verify MediaSession state (position, metadata, artwork) survives Android Auto Coolwalk's 2026 swipeable multi-media-card gesture — new car interaction pattern, not yet verified against it
- Silent-retry reconnect state distinct from the existing manual error+retry banner — auto-retry N times after a longer stream drop (tunnel, WiFi<->cellular handoff) before falling back to asking the user to tap retry; recurring complaint on competing radio apps, framed as "dangerous while driving" since it currently needs the user to reopen the app
- Podcasts MVP as a dedicated area
- Android TV / Google TV base (Leanback launcher, D-pad/focus navigation, TV layout MVP)
- Chromecast expansion
- Quick-access playback widget — scope as favorites-selectable (a glanceable list/grid to pick any favorite), not just a resume-last button; that narrower version is a recurring complaint about other radio-app widgets
- More transitions, motion, and shimmer
- More unit and UI tests
- Baseline Profiles for cold-start performance
- True adaptive bitrate reselection on network-type change (WiFi<->cellular), not just at initial station switch

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
- Local-only listening stats (on-device only, no server) — fits the privacy-first positioning better than the account-gated insights competitors offer
- "Similar stations" / discover row using Radio Browser tag/country/language metadata — no tracking or account needed, unlike most competitors' recommendation features
- Alarm-clock / wake-to-radio — table stakes on some competing apps, currently absent
- Note: offline stream recording was considered and explicitly rejected as a feature — stream recording rights vary by station license, treating it as a legal risk rather than a privacy one

## Note

- This roadmap prioritizes user value and stability.
- Priorities may change based on testing feedback and device behavior.
