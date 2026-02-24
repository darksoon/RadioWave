# RadioWave - Phasenstatus

Stand: 2026-02-24

## Phase 1 - Core Layer
- Status: abgeschlossen
- Inhalt:
  - `core-model`
  - `core-database`
  - `core-network`
  - `core-data`

## Phase 2 - Player Layer
- Status: abgeschlossen
- Inhalt:
  - `core-player`
  - `core-cast` (Basis)
- Zusatz:
  - Buffer-/Reconnect-Haertung
  - Network-Return-Recovery
  - Wake/Wifi-Lock fuer stabileres Background-Playback
  - Playback-Lost-Guard (Stall-Watchdog + Auto-Recovery)

## Phase 3 - UI Layer
- Status: weitgehend abgeschlossen
- Inhalt:
  - `core-ui` Theme + gemeinsame States
  - Home/Browse/Favorites/Player als produktive Basis
- Zusatz:
  - Premium Home-Hintergrund
  - Mini-Player Glass-Overlay + Runtime
  - Favoriten-Feedback im Player

## Phase 4 - Navigation
- Status: abgeschlossen
- Inhalt:
  - Bottom Navigation (Home, Browse, Favorites, Settings)
  - stabiles Top-Level Routing (`popUpTo`, `restoreState`)
  - Floating Player Overlay

## Phase 5 - Browse & Suche
- Status: abgeschlossen
- Inhalt:
  - Suche mit Debounce
  - Genre-/Land-Filter
  - Ergebnis-Grid
  - Empty-State

## Phase 6 - Features
- Status: in Arbeit (groesstenteils umgesetzt)
- Erledigt:
  - Favoriten-Toggle (bewusst nur Suche + Player)
  - Favoriten-Persistenz in Room (mit Station-Snapshot)
  - Favoriten-Screen (Liste + Remove)
  - Settings-Basisfunktionen (persistierte Toggles)
  - Fullscreen-Player (opening via Mini-Player)
  - Fullscreen-Player Controls: Mute/Unmute, Random-Station, Previous-Station
  - Fullscreen-Player LIVE-Bar mit dezenter Animation
  - Explizites Back-Verhalten im Fullscreen-Overlay
  - Home Header bereinigt (kein Account-Icon)
- Offen:
  - Favoriten Export/Import (JSON)
  - Sleep Timer
  - Share Action im Fullscreen-Player
  - Custom Stations (manuelle URL + M3U/PLS)
  - Settings-Ausbau (Theme, Cache, About, Audio-Qualitaet)

## Phase 7 - Integration
- Status: offen
- Offen:
  - Android Auto (MediaLibraryService + Browse-Tree)
  - Chromecast Integration
  - Widget/Notification Ausbau

## Phase 8 - Polish
- Status: offen
- Offen:
  - Screen-Transitions
  - Player-Animationen
  - Loading Shimmer
  - Unit-/UI-Tests ausbauen

## Naechste Prioritaeten
1. Fullscreen-Player vervollstaendigen (Sleep Timer + Share)
2. Favoriten Export/Import
3. Settings ausbauen
4. Android Auto Basis
5. Chromecast Basis
