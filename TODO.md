# RadioWave - Projektstand und TODO

Hinweis: Kompakter Phasenstatus in `PHASEN.md`.

Stand: 2026-02-24

## Heute erledigt
- [x] Fullscreen Player als echtes Overlay ohne Click-Through
- [x] Fullscreen Player UI deutlich aufgewertet (Cover bevorzugt, Station-Logo als Fallback)
- [x] Fullscreen Controls erweitert: Favorit, Previous, Play/Pause, Mute, Random
- [x] Android Back schliesst zuerst den Fullscreen Player
- [x] LIVE-Zeitbalken im Fullscreen Player: voll + dezente Puls-Animation
- [x] Mini-Player weiter verfeinert (Glass/Lesbarkeit/Laufzeit)
- [x] Playback-Robustheit ausgebaut (Buffering, Reconnect, Lost-Guard, Network-Recovery)
- [x] Build-Artefakte aus Git entfernt (`build-logic/.gradle`, `build-logic/build`)
- [x] `.gitignore` erweitert (lokale Agent-/Session-Dateien und Root-Mockups)

## Aktueller Produktstatus

### Funktioniert
- [x] Home, Suche, Favoriten, Settings-Basis
- [x] Favoriten-Persistenz in Room (inkl. Snapshot)
- [x] Recent Stations in Room
- [x] Floating Mini-Player
- [x] Fullscreen Player mit erweiterten Controls
- [x] Stabileres Background-Playback (Wake/Wifi Lock + Recovery)

### In Arbeit
- [ ] Settings-Ausbau (mehr als Basis-Toggles)
- [ ] Custom Stations
- [ ] Export/Import fuer Favoriten

## TODO - Prioritaet

### 1) Player
- [ ] Sleep Timer (15/30/60 Minuten + Off)
- [ ] Share Action im Fullscreen Player
- [ ] Optional: Lautstaerke-Slider statt nur Mute Toggle

### 2) Daten und Nutzerfunktionen
- [ ] Favoriten Export/Import (JSON)
- [ ] Custom Stations (manuelle URL)
- [ ] M3U/PLS Import

### 3) Settings-Ausbau
- [ ] Theme (Dark/Light/System)
- [ ] Audio-Optionen
- [ ] Cache/Storage Aktionen
- [ ] About (Version, GitHub, Lizenzen)

### 4) Integrationen
- [ ] Android Auto Basis (MediaLibraryService + Browse-Tree)
- [ ] Chromecast Basis (Cast Button + Session)

### 5) Qualitaet
- [ ] Unit-Tests fuer Player-Flow und Repositories
- [ ] Compose UI-Tests fuer Home/Player/Favoriten

## Naechste Schritte (empfohlen)
1. Sleep Timer + Share im Fullscreen Player abschliessen.
2. Favoriten Export/Import implementieren.
3. Custom Stations (URL + M3U/PLS) starten.
