# RadioWave Roadmap (Public)

Stand: 2026-03-09

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
- Settings-Info: App-Version, GitHub Repo, Issues, Website, Ko-fi
- Lokaler Crash-Report mit Share-/Issue-Export in Settings-Info integriert
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
- Update-Dialog zeigt Download-Fortschritt live (MB/% + Progressbar)
- Stable/Beta-Updatekanal in Settings integriert (Pre-Releases optional)
- Manueller Signed-Release-Workflow mit Versionspflege ueber `gradle.properties` integriert
- Android Auto Developer-Mode Anleitung fuer Sideload/Beta hinterlegt (Settings + Docs)
- Android Auto Shortcut in Settings mit Fallbacks (kein Launcher -> Settings/App-Info)
- Launcher Quick Actions per App-Icon-Long-Press integriert (`Search`, `Favorites`, `Player`, `Settings`)
- Android Auto Browse/Player weiter poliert (lokalisierte Labels, `Quick Access`, robustere Suche, Prev/Next im Car-Player)
- i18n-Basis gestartet (DE/EN ueber Systemsprache, Ressourcenstruktur eingefuehrt)
- Settings-Update-Bereich bereits auf lokalisierte Strings umgestellt (DE/EN)
- Sprachwahl in Settings integriert (System/Deutsch/English)
- Locale-Engine auf AppCompat-Basis stabilisiert (Crashfix bei Aktivitaetsstart nach Sprachwechsel)
- Light-Mode Lesbarkeit in Home/Browse/Favoriten auf Theme-Farben umgestellt (keine harten Weiss-Kontraste mehr)
- App-Shell auf schlichtes Visual reduziert (kein Sterne/Nebula-Hintergrund mehr im Hauptlayout)
- Android Auto Browse-Slot `Recents` auf `Favorites` gemappt fuer schnelleren Zugriff waehrend der Fahrt
- CI/Release Pipelines gehaertet (Push-CI aktiv, Signed-Workflow robuster)

## In Arbeit
- Voice/Assistant-Play-Intents fuer Senderaufrufe (Google Assistant)

## Geplant (naechste Schritte)
- Radio Timeshift Ausbau (ueber MVP hinaus: laengerer Buffer, optionaler Seek/LIVE im Fullscreen)
- Sleep Timer (15/30/60/off)
- Share Action im Fullscreen-Player
- Podcasts MVP als eigener Bereich (Bottom-Bar Eintrag + Suche + Episodenliste)
- Favoriten Export/Import (JSON)
- Custom Stations (manuelle URL) + M3U/PLS Import
- Datennutzung sichtbar machen (geschaetzt ueber Bitrate)

## Spaeter
- Android Auto Ausbau (Recommendations, weitere Car-spezifische UX)
- Android TV / Google TV Basis (Leanback-Launcher, D-Pad-/Focus-Navigation, TV-Layout-MVP)
- Chromecast Ausbau
- Widget fuer schnellen Playback-Zugriff
- Weitere Animationen/Transitions/Shimmer
- Ausbau von Unit- und UI-Tests

## Ueberlegungen / Maybe Features
- Podcast-Quelle separat anbinden:
  - RadioBrowser API liefert primaer Radiosender, keine saubere Podcast-Episodenstruktur
  - fuer Podcasts daher eigener Provider noetig (z. B. RSS-basiert / dedizierte Podcast-API)
  - UI-Konzept: getrennte Listen `Sender` / `Podcasts` in Favoriten moeglich
- Wear OS Companion:
  - eher Remote-/Companion-Use-Case statt vollwertiger Radio-Client
  - Fokus auf Play/Pause, Favoriten, Recents und Player-Status
  - nur sinnvoll nach Phone/Auto/TV/Chromecast-Basis
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
