# RadioWave Roadmap (Public)

Stand: 2026-03-05

Diese Datei ist fuer GitHub gedacht und zeigt den Produktstatus kompakt.

## Fertig
- Core-Architektur (`core-model`, `core-database`, `core-network`, `core-data`)
- Basis-Player (`core-player`) mit stabilisiertem Background-Playback
- Home, Browse/Suche, Favoriten, Fullscreen-Player, Settings-Basis
- Bottom Navigation + stabiles Routing + Floating Mini-Player
- Mini-Player Metadaten als Marquee bei langen Titeln
- Favoriten und Recents in Room
- Fullscreen-Player Controls: Play/Pause, Mute, Random, Previous, Favorite
- LIVE-Fortschritt im Fullscreen-Player (UI-Animation)
- Stream-Qualitaet im Fullscreen-Player sichtbar (Codec/Bitrate)
- **Album-Cover im Fullscreen-Player** (iTunes API, Blur-Hintergrund, Station-Logo)
- Home/Browse/Favorites UI-Polish (Glass-Look, bessere Karten, Performance-Tuning)
- Nebula-Background als app-weites Visual (Home/Browse/Favoriten) mit dezenten Overlays
- Home-Favoritenkarussell weiter optimiert (kompakter, Center-Snap inkl. Rand-Items)
- Security-Basis gehaertet (`allowBackup=false`, HTTP-Streams kompatibel fuer breite Senderabdeckung)
- Optional sichtbare HTTP-Kennzeichnung in Browse + Settings-Toggle fuer unsichere Streams
- Settings Rework als Kategorien-Navigation mit Detailseiten (`Allgemein`, `Sound`, `Benachrichtigung`, `Speicher & Daten`, `Info`)
- Settings-Optionen: Theme (System/Dark/Light), Dynamic Colors, Standard-Qualitaet, Buffer-Profil
- Standard-Qualitaet ist jetzt im Player aktiv (Variante wird bei Wiedergabe nach Bitrate gepickt)
- Settings-Datenaktionen: Sender-Cache leeren, Verlauf loeschen
- Settings-Info: App-Version, GitHub Repo, Issues, Website, Buy a Coffee
- Notification Media Controls inkl. Prev/Play-Pause/Next/Stop
- Audio Focus Verhalten bei Unterbrechungen stabilisiert (kein sofortiges Stop/Pause mehr)
- Mobile-Daten-Policy und Buffer-Profil technisch im Player wirksam
- Room-Migrationen aktiv (kein `fallbackToDestructiveMigration` mehr)
- Unit-Tests erweitert fuer Player-Recovery und Repository-Flows
- Android Auto Basis integriert (Media3 `MediaLibraryService`, Favorites/Recents Browse)
- Android Auto Resume/Autoplay verbessert (letzter Sender + Auto-Connect-Resume)
- Android Auto Player-Pfad vereinheitlicht (kein separater Fallback-Player mehr)
- Doppel-Notification bei Android Auto behoben (nur ein aktiver Media-Notification-Pfad)
- Backup-Defaults weiter gehaertet (SharedPrefs in Cloud-Backup ausgeschlossen)
- Optionaler Hitzemodus in Settings (kleineres Buffer-Profil + reduzierte Metadatenlast)
- Auto-Verbindung aktiviert Low-Load-Verhalten automatisch (Metadaten-/Artwork-Last reduziert)
- Android Auto erzwingt waehrend Car-Session maximal 128 kbps (bitrate-schonender Betrieb)
- Erststart-Info-Dialog integriert (wichtige Hinweise beim ersten App-Start)
- In-App Updateflow ueber GitHub Releases integriert (Hinweisdialog + APK-Download + Installer)
- Update-Settings-Seite integriert (Popup an/aus, Auto-Check an/aus, manuelle Pruefung, Versionsstatus)
- CI/Release Pipelines gehaertet (Push-CI aktiv, Signed-Workflow robuster)

## In Arbeit
- Notification Media Controls Feinschliff (Polish/UX)
- Voice/Assistant-Play-Intents fuer Senderaufrufe (Google Assistant)

## Geplant (naechste Schritte)
- Radio Timeshift MVP (fix 20 Minuten Buffer, Fullscreen-Seek + LIVE-Button)
- Sleep Timer (15/30/60/off)
- Share Action im Fullscreen-Player
- Favoriten Export/Import (JSON)
- Custom Stations (manuelle URL) + M3U/PLS Import
- Datennutzung sichtbar machen (geschaetzt ueber Bitrate)

## Spaeter
- Android Auto Ausbau (Queue/Recommendations, Car-spezifische UX)
- Chromecast Ausbau
- Widget fuer schnellen Playback-Zugriff
- Weitere Animationen/Transitions/Shimmer
- Ausbau von Unit- und UI-Tests

## Ueberlegungen / Maybe Features
- Cache-DB fuer Browse/Suche:
  - `cached_stations` als zentrale Quelle in Room
  - Cache-first Repository-Flow (erst lokal, dann Netzwerk-Refresh)
  - `search_cache_meta` fuer TTL-Logik
  - Verknuepfung mit Favoriten/Recents gegen Daten-Inkonsistenzen
  - Room-Migration bei Schema-Aenderung
- Optionaler Lautstaerke-Slider zusaetzlich zu Mute

## Hinweis
- Diese Roadmap priorisiert Nutzerwert und Stabilitaet.
- Reihenfolge kann sich je nach Test-Feedback und Device-Verhalten aendern.
