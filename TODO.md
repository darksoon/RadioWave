# RadioWave - Projektstand & TODO

## ✅ Erledigt (Phase 1-3)

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
- ✅ **core-ui**: Theme, LoadingState, ErrorState, StationItem (Card-Design)
- ✅ **feature-home**: HomeScreen mit LazyColumn, Suchleiste (Debounce 500ms), StationCard, StationListItem
- ✅ **feature-player**: BottomPlayerBar mit Play/Pause, Sender-Info, Metadaten
- ✅ **MainActivity**: Scaffold mit BottomPlayerBar, Navigation

### Infrastruktur
- ✅ **Build-Logic**: Convention Plugins (AndroidApplication, AndroidLibrary, Hilt, Room)
- ✅ **Gradle**: Alle Module mit korrekten Dependencies
- ✅ **Manifest**: Permissions (Internet, ForegroundService), NetworkSecurityConfig
- ✅ **Icons**: Launcher Icons (ic_launcher, ic_launcher_round)
- ✅ **Git**: Repository auf GitHub (darksoon/RadioWave)

---

## 🚧 TODO (Phase 4 - Features)

### Navigation
- [ ] **Navigation Graph**: Vollständige Navigation zwischen Screens
- [ ] **Bottom Navigation**: Home, Browse, Favorites, Settings Tabs
- [ ] **Player Screen**: Fullscreen Player mit Album Art, Controls, Sleep Timer

### Features
- [ ] **Favoriten**: Speichern/Laden, Toggle in UI, Drag-to-Reorder
- [ ] **Recent Stations**: Zuletzt gehört anzeigen
- [ ] **Browse**: Genres, Länder, Sprachen als Filter
- [ ] **Suche**: Erweiterte Suche mit Filter (Codec, Bitrate)
- [ ] **Custom Stations**: Manuelle URL-Eingabe, M3U/PLS Import
- [ ] **Einstellungen**: Theme (Dark/Light/System), Audio-Qualität, Datennutzung
- [ ] **Sleep Timer**: Automatisches Stoppen nach X Minuten
- [ ] **Alarm**: Wecker mit Radiosender

### Android Auto
- [ ] **Auto-Browse-Tree**: MediaLibrarySession Callback
- [ ] **Auto-Integration**: AndroidManifest Einträge
- [ ] **Auto-UI**: Browse-Tree für Favoriten, Recent, Genres

### Chromecast
- [ ] **Cast-Integration**: Media3 Cast Extension
- [ ] **Cast-Button**: In Player UI
- [ ] **Cast-Session**: Sender wechseln

### Polish
- [ ] **Animationen**: Übergänge, Lade-Animationen
- [ ] **Fehlerbehandlung**: Retry-Logik, Offline-Modus
- [ ] **Widgets**: Home Screen Widget
- [ ] **Notifications**: Rich Media Notification
- [ ] **ProGuard**: Release-Build optimieren

### Testing
- [ ] **Unit Tests**: ViewModels, Repositories, Use Cases
- [ ] **UI Tests**: Compose Testing
- [ ] **Integration Tests**: API, Database

---

## 🐛 Bekannte Bugs

1. **DNS/Netzwerk**: Manchmal Verbindungsprobleme zur API (all.api.radio-browser.info)
2. **Audio-Stuttering**: Buffer-Optimierung sollte helfen, aber bei sehr schlechter Verbindung kann es noch krachen
3. **UI**: Keine Animationen beim Wechsel zwischen Screens

---

## 📱 Aktueller Status

**Funktioniert:**
- App startet
- Top-Sender werden geladen
- Suche funktioniert (mit Debounce)
- Sender können abgespielt werden
- Bottom Player Bar zeigt aktuellen Sender
- Play/Pause funktioniert
- Buffer-Optimierung für stabile Wiedergabe

**Nicht implementiert:**
- Favoriten-Verwaltung
- Vollständige Navigation
- Settings
- Android Auto
- Chromecast
- Sleep Timer
- Alarm

---

## 🎯 Nächste Schritte (Empfohlen)

1. **Favoriten implementieren** - Wichtig für UX
2. **Settings Screen** - Theme, Audio-Qualität
3. **Vollständige Navigation** - Bottom Tabs
4. **Android Auto** - Für Auto-Nutzung
5. **Testing** - Unit & UI Tests

---

## 📝 Notizen

- **Tech Stack**: Kotlin, Jetpack Compose, Hilt, Room, Retrofit, Media3/ExoPlayer
- **Architektur**: Clean Architecture mit MVVM
- **API**: Radio Browser API (all.api.radio-browser.info)
- **Min SDK**: 26, Target SDK: 35

**Letzte Session:** 2026-02-23
**Status:** Core Features funktionieren, UI ist poliert, bereit für Feature-Erweiterungen
