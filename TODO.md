# RadioWave - Projektstand & TODO

## Audit & Stabilisierung (2026-02-24)

### Erledigt
- [x] Build-Blocker behoben: Compose BOM fuer transitive Compose-Abhaengigkeiten (Material3) stabilisiert
- [x] Lint-Blocker behoben: Media3 UnstableApi Opt-in in core-player ergaenzt
- [x] Lint-Blocker behoben: Backup-Regeln in app/src/main/res/xml/data_extraction_rules.xml korrigiert
- [x] Test-Pipeline stabilisiert: Test-Tasks laufen nur bei vorhandenen src/test oder src/androidTest
- [x] Deprecated Compose Live Literals aus Convention Plugin entfernt
- [x] Validierung erfolgreich: :build-logic:build, build, lint, test
- [x] Navigation-Bug behoben: Ruecksprung auf Home via Bottom Navigation nach Entdecken->Browse funktioniert wieder

### Offen / Follow-up
- [ ] Ktlint im Projekt konfigurieren (Task ktlintCheck existiert aktuell nicht)
- [ ] Echte Unit- und UI-Tests ergaenzen (aktuell werden viele Test-Tasks ohne Testquellen uebersprungen)

### Session-Update (2026-02-24)
- [x] Home-Screen visuell stark ueberarbeitet (TuneIn-Style, engere Karten/Abstaende, klarerer Look)
- [x] Home-Background mit dezenter Animation (langsamer Glow/Drift statt statischer Farbblende)
- [x] Floating Mini-Player als echtes Overlay ueber Content positioniert
- [x] Mini-Player Glass-Design iteriert und fuer bessere Lesbarkeit abgedunkelt
- [x] Mini-Player Laufzeitanzeige ergaenzt (`mm:ss` / `hh:mm:ss`)
- [x] Buffer/Recovery in `core-player` gehaertet (groessere Buffer + Retry/Backoff)
- [x] Auto-Recovery bei kurzer Netzunterbrechung via Network-Callback umgesetzt
- [x] Manifest um `ACCESS_NETWORK_STATE` erweitert

## ✅ Erledigt (Phase 1-5)

### Core Layer (Phase 1)
- ✅ **core-model**: Station, PlayerState, StreamMetadata, Genre, Country, PlayerError (Sealed Class)
- ✅ **core-database**: Room Entities (Station, Favorite, Recent, CustomStation), DAOs, DatabaseModule mit Hilt
- ✅ **core-network**: RadioBrowserApi mit Retrofit, DTOs, NetworkModule, Base URL auf all.api.radio-browser.info
- ✅ **core-data**: Repository Interfaces, OfflineFirstStationRepository, Mappers, RepositoryModule

### Player Layer (Phase 2)
- ✅ **core-player**: PlayerController, PlayerControllerImpl, RadioPlayerService (MediaSession), RadioPlayerManager
- ✅ **Buffer-Optimierung**: 30s min / 90s max Buffer, Reconnect-Logik (3 Versuche, 2s Delay)
- ✅ **DI**: PlayerModule mit @Binds und @Provides

### UI Layer (Phase 3)
- ✅ **core-ui**: Premium Dark Theme, LoadingState, ErrorState
- ✅ **Premium Color Palette**: DarkBackground (#121212), DarkCardBackground (#252525), TealAccent (#00BCD4)
- ✅ **feature-home**: TuneIn-Style mit horizontalen LazyRow-Sektionen (Entdecken, Zuletzt gehört, Favoriten)
- ✅ **feature-browse**: Suchleiste + Genre-Chips (Techno, Dance, Rock, Jazz, 80s, etc.), 2-spaltiges Grid
- ✅ **feature-player**: Floating Player Bar mit Elevation 16dp, Progress-Indicator, Buffer-Animation
- ✅ **StationCard**: 140x140dp, 16dp abgerundete Ecken, Gradient-Overlay (Transparent → Schwarz)

### Navigation (Phase 4)
- ✅ **Bottom Navigation**: Home, Browse, Favorites, Settings Tabs
- ✅ **Navigation Graph**: NavHost mit Google Standard Pattern (popUpTo, launchSingleTop, restoreState)
- ✅ **Floating Player**: Schwebt über Content, korrektes Layering
- ✅ **Genre-Redirect**: Klick auf Entdecken-Kategorie → Browse mit automatischer Suche
- ✅ **Back-Handling**: Zurück-Button führt immer zurück zu Home

### Browse & Suche (Phase 5)
- ✅ **Genre-Chips**: 10 Kategorien (Techno, Dance, Rock, Jazz, 80s, Pop, Classical, Hip Hop, Chill, House)
- ✅ **Länder-Filter**: Top 10 mit Flaggen (🇩🇪🇺🇸🇬🇧🇫🇷🇮🇹🇪🇸🇳🇱🇵🇱🇦🇹🇨🇭)
- ✅ **Ergebnis-Anzeige**: 2-spaltiges Grid mit Sender-Logo, Name, Land
- ✅ **Empty-State**: "Keine Sender gefunden" mit Vorschlägen
- ✅ **Debounce**: 500ms für Suche

### Infrastruktur
- ✅ **Build-Logic**: Convention Plugins (AndroidApplication, AndroidLibrary, Hilt, Room)
- ✅ **Gradle**: Alle Module mit korrekten Dependencies
- ✅ **Manifest**: Permissions, NetworkSecurityConfig, enableOnBackInvokedCallback=true
- ✅ **Icons**: Launcher Icons (ic_launcher, ic_launcher_round)
- ✅ **Git**: Repository auf GitHub (darksoon/RadioWave)

---

## 🚧 TODO (Phase 6 - Features)

### Favoriten-System
- [x] **Favoriten Toggle**: Herz-Button in StationCards
- [x] **Favoriten persistieren**: Room Database Integration (inkl. Station-Snapshot fuer Offline-Anzeige)
- [x] **Favoriten Screen**: Liste mit allen Favoriten (Play + Remove via Heart)
- [ ] **Favoriten-Export/Import**: JSON Backup

### Fullscreen Player
- [ ] **Player Sheet**: Expandable Bottom Sheet
- [ ] **Album Art**: Großes Cover mit Blur-Background
- [ ] **Sleep Timer**: Timer-Auswahl (15, 30, 60 min)
- [ ] **Share Button**: Stream-URL teilen

### Custom Stations
- [ ] **Manuelle URL-Eingabe**: Validierung, Test-Button
- [ ] **M3U/PLS Import**: Playlist-Dateien parsen

### Einstellungen
- [ ] **Theme-Umschaltung**: Dark/Light/System
- [ ] **Audio-Qualität**: Stream-Qualität wählen
- [ ] **Cache leeren**: Database, Images
- [ ] **Über die App**: Version, GitHub-Link, Lizenzen

---

## 🔮 TODO (Phase 7 - Integration)

### Android Auto
- [ ] **MediaLibraryService**: Browse-Tree implementieren
- [ ] **Auto Manifest**: automotive_app_desc.xml
- [ ] **Auto-Browse**: Favoriten, Recent, Genres als Roots
- [ ] **Auto-Playback**: Steuerung vom Auto aus

### Chromecast
- [ ] **Cast Extension**: Media3 Cast Integration
- [ ] **Cast Button**: In Player UI

### Notifications & Widgets
- [ ] **Media Notification**: Album Art, Play/Pause
- [ ] **Home Widget**: Favoriten schnell starten

---

## 🎨 TODO (Phase 8 - Polish)

### Animationen
- [ ] **Screen Transitions**: Slide, Fade zwischen Tabs
- [ ] **Player Animation**: Expand/Collapse Sheet
- [ ] **Loading Shimmer**: Skeleton-Loading für Cards

### Testing
- [ ] **Unit Tests**: ViewModels, Repositories
- [ ] **UI Tests**: Compose Testing

---

## 📱 Aktueller Status

**Funktioniert:**
- ✅ Premium Dark Theme mit Teal/Mint Akzenten
- ✅ Bottom Navigation (perfekt synchronisiert)
- ✅ Suche mit Debounce (500ms)
- ✅ Genre-Chips für schnelle Suche
- ✅ Länder-Filter mit Flaggen
- ✅ Floating Player Bar mit Buffer-Animation
- ✅ TuneIn-Style Home Screen (3 LazyRow-Sektionen)
- ✅ Kategorie-Entdecken → automatische Suche
- ✅ Sender abspielen & streamen
- ✅ Play/Pause mit visuellem Feedback
- ✅ Back-Button führt zu Home

**Teilweise implementiert:**
- ⚠️ Favoriten: Repository & DAO existieren, UI fehlt noch
- ⚠️ Recent Stations: Werden in DB gespeichert
- ⚠️ Settings: Placeholder Screen

**Nicht implementiert:**
- ❌ Android Auto
- ❌ Chromecast
- ❌ Sleep Timer
- ❌ Fullscreen Player
- ❌ Custom Stations

---

## 🎯 Nächste Schritte (Priorität)

1. **Favoriten Toggle** - Herz-Button zum Speichern
2. **Favoriten Screen** - Liste mit allen Favoriten
3. **Fullscreen Player** - Expandable Sheet
4. **Settings Screen** - Basis-Einstellungen
5. **Android Auto** - Für Auto-Nutzung

---

## 📝 Notizen

- **Tech Stack**: Kotlin, Jetpack Compose, Hilt, Room, Retrofit, Media3/ExoPlayer
- **Architektur**: Clean Architecture mit MVVM
- **API**: Radio Browser API (all.api.radio-browser.info)
- **Min SDK**: 26, Target SDK: 35
- **Design**: Premium Dark Theme mit Teal/Mint Akzenten

**Letzte Session:** 2026-02-24
**Status:** Audit-Pipeline gruen (build/lint/test), bereit fuer Feature-Umsetzung



