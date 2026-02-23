# RadioWave - Projektstand & TODO

## ✅ Erledigt (Phase 1-4)

### Core Layer (Phase 1)
- ✅ **core-model**: Station, PlayerState, StreamMetadata, Genre, Country, PlayerError (Sealed Class)
- ✅ **core-database**: Room Entities (Station, Favorite, Recent, CustomStation), DAOs, DatabaseModule mit Hilt
- ✅ **core-network**: RadioBrowserApi mit Retrofit, DTOs, NetworkModule, Base URL auf all.api.radio-browser.info
- ✅ **core-data**: Repository Interfaces, OfflineFirstStationRepository, Mappers, RepositoryModule

### Player Layer (Phase 2)
- ✅ **core-player**: PlayerController, PlayerControllerImpl, RadioPlayerService (MediaSession), RadioPlayerManager
- ✅ **Buffer-Optimierung**: DefaultLoadControl mit 15s/50s Buffern, Logging für alle Player-Zustände
- ✅ **DI**: PlayerModule mit @Binds und @Provides

### UI Layer (Phase 3)
- ✅ **core-ui**: Premium Dark Theme, LoadingState, ErrorState
- ✅ **Premium Color Palette**: DarkBackground (#121212), DarkCardBackground (#252525), TealAccent (#00BCD4)
- ✅ **feature-home**: TuneIn-Style mit horizontalen Scroll-Listen, Kategorie-Entdecken
- ✅ **feature-browse**: Suchleiste + Genre-Chips (Techno, Dance, Rock, Jazz, 80s, etc.)
- ✅ **feature-player**: Floating Player Bar mit Elevation 16dp, Fortschrittsbalken
- ✅ **StationCard**: 16dp abgerundete Ecken, Gradient-Overlay, Teal-Akzente

### Navigation (Phase 4)
- ✅ **Bottom Navigation**: Home, Browse, Favorites, Settings Tabs
- ✅ **Navigation Graph**: NavHost mit allen Routes
- ✅ **Floating Player**: Schwebt über Content, nicht über Navigation
- ✅ **Screen-Stubs**: Browse, Favorites, Settings Placeholder-Screens

### Infrastruktur
- ✅ **Build-Logic**: Convention Plugins (AndroidApplication, AndroidLibrary, Hilt, Room)
- ✅ **Gradle**: Alle Module mit korrekten Dependencies
- ✅ **Manifest**: Permissions (Internet, ForegroundService), NetworkSecurityConfig
- ✅ **Icons**: Launcher Icons (ic_launcher, ic_launcher_round)
- ✅ **Git**: Repository auf GitHub (darksoon/RadioWave)

---

## 🚧 TODO (Phase 5 - Features)

### Favoriten-System
- [ ] **Favoriten Toggle**: Herz-Button in StationCards/StationListItem
- [ ] **Favoriten persistieren**: Room Database Integration
- [ ] **Favoriten Screen**: Drag-to-Reorder, Swipe-to-Remove
- [ ] **Favoriten-Export/Import**: JSON Backup

### Browse & Suche
- [ ] **Länder-Filter**: Flaggen, alphabetisch sortiert
- [ ] **Sprachen-Filter**: Deutsch, Englisch, etc.
- [ ] **Erweiterte Filter**: Codec, Bitrate, Nur funktionierende Streams
- [ ] **Pull-to-Refresh**: SwipeRefresh in Listen

### Fullscreen Player
- [ ] **Player Sheet**: Expandable Bottom Sheet
- [ ] **Album Art**: Großes Cover mit Blur-Background
- [ ] **Sleep Timer**: Timer-Auswahl (15, 30, 60 min)
- [ ] **Share Button**: Stream-URL teilen
- [ ] **Equalizer Animation**: Animierte Bars beim Abspielen

### Custom Stations
- [ ] **Manuelle URL-Eingabe**: Validierung, Test-Button
- [ ] **M3U/PLS Import**: Playlist-Dateien parsen
- [ ] **Custom Station Management**: Edit, Delete

### Einstellungen
- [ ] **Theme-Umschaltung**: Dark/Light/System
- [ ] **Audio-Qualität**: Stream-Qualität wählen
- [ ] **Datennutzung**: Mobile Data Warning
- [ ] **Cache leeren**: Database, Images
- [ ] **Über die App**: Version, GitHub-Link, Lizenzen

---

## 🔮 TODO (Phase 6 - Integration)

### Android Auto
- [ ] **MediaLibraryService**: Browse-Tree implementieren
- [ ] **Auto Manifest**: automotive_app_desc.xml
- [ ] **Auto-Browse**: Favoriten, Recent, Genres als Roots
- [ ] **Auto-Playback**: Steuerung vom Auto aus

### Chromecast
- [ ] **Cast Extension**: Media3 Cast Integration
- [ ] **Cast Button**: In Player UI
- [ ] **Cast Session**: Sender an Cast-Gerät übergeben

### Notifications & Widgets
- [ ] **Media Notification**: Album Art, Play/Pause
- [ ] **Home Widget**: Favoriten schnell starten

---

## 🎨 TODO (Phase 7 - Polish)

### Animationen
- [ ] **Screen Transitions**: Slide, Fade zwischen Tabs
- [ ] **Player Animation**: Expand/Collapse Sheet
- [ ] **Loading Shimmer**: Skeleton-Loading für Cards
- [ ] **Card Press Animation**: Scale-Effekt

### Fehlerbehandlung
- [ ] **Retry-Logik**: Automatischer Reconnect bei Netzwerkfehler
- [ ] **Offline-Modus**: Cached Stations anzeigen
- [ ] **Broken Stream Report**: An API melden

### Testing
- [ ] **Unit Tests**: ViewModels, Repositories
- [ ] **UI Tests**: Compose Testing mit Turbine
- [ ] **Integration Tests**: API, Database

---

## 🐛 Bekannte Bugs

1. ~~DNS/Netzwerk~~: API funktioniert stabil
2. **Smart-Casts**: Bei Modul-übergreifenden Properties lokale Kopien nötig
3. **Station Logos**: Manche URLs sind defekt (normales Internet-Verhalten)

---

## 📱 Aktueller Status

**Funktioniert:**
- ✅ Premium Dark Theme (Teal/Mint Akzente)
- ✅ Bottom Navigation (Home, Browse, Favorites, Settings)
- ✅ Suche mit Debounce (500ms)
- ✅ Genre-Chips für schnelle Suche
- ✅ Floating Player Bar mit Progress-Indicator
- ✅ TuneIn-Style Home Screen
- ✅ Kategorie-Entdecken (News, Sport, Musik, etc.)
- ✅ Sender abspielen & streamen
- ✅ Play/Pause Steuerung

**Teilweise implementiert:**
- ⚠️ Favoriten: Repository existiert, UI fehlt noch
- ⚠️ Recent Stations: Werden in DB gespeichert, aber nicht korrekt angezeigt
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

**Letzte Session:** 2026-02-23
**Status:** High-End UI fertig, Navigation komplett, bereit für Feature-Implementierung
