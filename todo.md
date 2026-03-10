RadioWave - Master Backlog (Lokal)

Stand: 2026-03-09

Ziel: Eine zentrale lokale Datei statt separatem `PHASEN.md` + `todo.md`.

## 1) Wichtig (jetzt)

### Playback & Stabilitaet
- [x] Audio-Focus sauber finalisieren (Sprachnachricht/Anruf: pausieren, kontrolliert fortsetzen)
- [x] Notification Media Controls (Play/Pause/Stop) im Benachrichtigungsbereich
- [x] Notification Media Controls erweitert (Prev/Next/Stop + Settings-Toggles)
- [x] Notification Media Controls Feinpolish (UX/Details)
- [x] Mini-Player Metadaten als Lauftext / Marquee (DE/EN): aktiviert, nur bei langen Texten + Edge-Fade-Polish

### Settings-Engine (technische Wirksamkeit)
- [x] Mobile-Daten-Policy technisch im Player erzwingen (nur WLAN wenn deaktiviert)
- [x] Buffer-Profil aus Settings technisch im Player anwenden
- [x] Qualitaets-Praeferenz aus Settings technisch nutzen, falls Sender mehrere Streams hat
- [x] Open-Source-Lizenzen im Info-Bereich anzeigen

### Datenfunktionen
- [ ] Favoriten Export/Import (JSON)

## 2) Als Naechstes

### Player-Feature-Set abrunden
- [ ] Radio Timeshift MVP (fix 20 Minuten Buffer, Fullscreen-Seek + LIVE-Button)
- [ ] Sleep Timer (15/30/60/off)
- [ ] Share Action im Fullscreen-Player
- [ ] Stream-Qualitaet sichtbar im Fullscreen-Player (z.B. AAC 128 kbps)

### Senderverwaltung
- [ ] Custom Stations (manuelle URL)
- [ ] M3U/PLS Import

### Daten & Transparenz
- [ ] Datennutzung sichtbar machen (geschaetzt ueber Bitrate)

## 3) Nice to have

### Integrationen
- [x] Android Auto Basis (Media3 LibraryService, Favorites/Recents, Resume/Autoplay)
- [x] Android Auto: doppelte Notification entfernt (ein einheitlicher Media-Notification-Pfad)
- [x] Android Auto: Resume-/Autoplay-Startpfad stabilisiert (kein "spielt, aber stumm"-Zustand)
- [x] Android Auto: Low-Load-Modus automatisch aktiv waehrend Verbindung
- [x] Android Auto Feinschliff (lokalisierte Browse-Texte, Quick Access, robustere Suche, Prev/Next im Car-Player)
- [x] Launcher Quick Actions per App-Icon-Long-Press (Suche, Favoriten, Player, Settings)
- [x] In-App Updater: Settings-Check nutzt denselben APK-Download/Installer wie der Hauptdialog
- [x] Stable/Beta-Updatekanal fuer GitHub Releases
- [x] Lokaler Crash-Report mit Share-Export und vorbereiteter GitHub-Issue-Erstellung
- [ ] Android TV / Google TV Basis (Leanback-Launcher, D-Pad-/Focus-Navigation, TV-UI-MVP)
- [ ] Podcasts MVP als eigener Bereich (Bottom-Bar + Suche + Episodenliste)
- [ ] Favoriten getrennt darstellbar machen: `Sender` / `Podcasts`
- [ ] Chromecast Basis
- [ ] Widget fuer schnellen Playback-Zugriff

### UI/Polish
- [x] Home-Hintergrundanimation lifecycle-aware machen (nur bei `RESUMED`)
- [ ] Weitere Animationen/Transitions/Shimmer
- [ ] Optional: Lautstaerke-Slider zusaetzlich zu Mute

### Qualitaet
- [x] Unit-Tests fuer Player-Flow und Repositories ausbauen
- [x] Release-/CI-Workflows fuer Gradle-Cache und Signed Release Build auf aktuellen Stand gebracht
- [x] PR-CI nach Configuration-Cache-Einfuehrung repariert (test gating config-cache-safe)
- [x] Distribution-Split fuer GitHub-APK (`github`) und Play-Store-AAB (`play`) eingefuehrt
- [ ] Play Store: Native Debug Symbols fuer AAB/Crash- und ANR-Auswertung bereitstellen
- [ ] Compose UI-Tests fuer Home/Player/Favoriten erweitern
- [ ] Device-Testmatrix fuer aggressive Akku-Optimierer (Xiaomi/Huawei/Samsung)

## 4) Maybe / Spaeter

### Wearables
- [ ] Wear OS Companion / Remote Controls (Play/Pause, Favoriten, Recents, Player-Status)

### Podcasts
- [ ] Eigene Podcast-Datenquelle anbinden (RadioBrowser ist senderzentriert, nicht episodenbasiert)
- [ ] Playback fuer On-Demand Episoden erweitern (Resume-Position, Episoden-Metadaten)

### Browse/Suche Cache-DB (Room)
- [ ] `cached_stations` als zentrale Quelle
- [ ] Cache-first Flow: erst lokal emittieren, dann Netzwerk-Refresh
- [ ] `search_cache_meta` fuer TTL-Logik
- [ ] Favoriten + Recents sauber mit Cache verknuepfen (Inkonsistenzen vermeiden)
- [x] Room-Migration bei Schema-Aenderung

## 5) Bereits fertig (Kurzstand)
- [x] Core-Architektur (`core-model`, `core-database`, `core-network`, `core-data`)
- [x] Stabilisiertes Background-Playback inkl. Lost-Guard/Wake-Wifi-Lock/Foreground-Service-Lifecycle
- [x] Home/Browse/Favoriten/Fullscreen-Player produktiv
- [x] Fullscreen Controls: Favorite, Previous, Play/Pause, Mute, Random
- [x] LIVE-Bar Animation im Fullscreen-Player
- [x] Settings-Rework als Kategorien-Navigation mit Detailseiten
- [x] Theme (System/Dark/Light), Dynamic Colors, HTTP-Streams Toggle
- [x] Settings Info-Links (GitHub, Issues, Website, Ko-fi)
