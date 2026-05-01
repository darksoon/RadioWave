# RadioWave Roadmap

[Deutsch](ROADMAP.de.md) | [English](ROADMAP.md)

Stand: 2026-05-01

Diese Roadmap ist fuer GitHub gedacht und zeigt den oeffentlichen Produktstatus kompakt. RadioWave ist live auf Google Play; die Roadmap priorisiert deshalb Stabilitaet, Supportfaehigkeit und gezieltes UX-Polishing.

## Fertig

- Core-Architektur (`core-model`, `core-database`, `core-network`, `core-data`)
- Stabiler Player-Unterbau (`core-player`) mit gehaertetem Background-Playback
- Home, Browse/Suche, Favoriten, Fullscreen-Player und Settings-Basis
- Bottom Navigation, stabiles Routing und Floating Mini-Player
- Marquee-Metadaten im Mini-Player bei langen Titeln
- Favoriten und Recents in Room
- Fullscreen-Player-Controls: Play/Pause, Mute, Random, Previous, Favorite
- LIVE-Fortschritt als Animation im Fullscreen-Player
- Stream-Qualitaet im Fullscreen-Player sichtbar
- Album-Cover im Fullscreen-Player (iTunes API, Blur-Hintergrund, Stationslogo)
- UI-Polish und Performance-Tuning fuer Home, Browse und Favoriten
- Security-Basis gehaertet (`allowBackup=false`, HTTP-Stream-Kompatibilitaet)
- Optionale HTTP-Kennzeichnung in Browse plus Settings-Toggle fuer unsichere Streams
- Settings-Kategorien mit separaten Detailseiten
- Funktionierende Settings fuer Theme, Dynamic Colors, Standard-Qualitaet und Buffer-Profil
- Datenaktionen in Settings: Sender-Cache leeren und Verlauf loeschen
- Settings-Info-Links: Version, GitHub-Repo, Issues, Website, Ko-fi
- Lokaler Crash-Report mit Share-Export und GitHub-Issue-Handoff
- Notification-Media-Controls inklusive Previous/Play-Pause/Next/Stop
- Audio-Focus-Verhalten fuer Unterbrechungen stabilisiert
- Mobile-Daten-Policy und Buffer-Profil wirken technisch im Player
- Room-Migrationen aktiv, kein destruktiver Fallback
- Erweiterte Unit-Tests fuer Player-Recovery und Repository-Flows
- Android Auto Basis ueber Media3 `MediaLibraryService`
- Android Auto Resume/Autoplay mit Letzt-Sender-Wiederaufnahme verbessert
- Vereinheitlichter Android-Auto-Player-Pfad
- Doppelte Media-Notification waehrend Android Auto entfernt
- Optionaler Hitzemodus in Settings
- Android-Auto-Low-Load-Verhalten automatisch waehrend Verbindung
- Android-Auto-Qualitaetsbegrenzung in den Einstellungen fuer Car-Sessions konfigurierbar
- Erststart-Info-Dialog integriert
- In-App-Updateflow ueber GitHub Releases
- Update-Settings mit Popup-Toggle, Auto-Check-Toggle, manueller Pruefung und Versionsstatus
- Live-Download-Fortschritt fuer Updates
- Stable/Beta-Updatekanal
- Manueller Signed-Release-Workflow mit Versionspflege in `gradle.properties`
- Release-/Distributions-Split fuer GitHub-APK (`github`) und Play-Store-AAB (`play`)
- Android-Auto-Dev-Mode-Anleitung in App und Doku verlinkt
- Launcher Quick Actions (`Suche`, `Favoriten`, `Player`, `Settings`)
- Android-Auto-Browse/Player-Polish: lokalisierte Labels, Quick Access, bessere Suche, Prev/Next
- DE/EN-Sprachbasis mit Sprachwahl in der App
- CI- und Release-Pipelines gehaertet
- Android-Paketname / `applicationId` auf `de.darksoon.radiowave` umgestellt
- GitHub-Actions-Workflows fuer die Node-24-Umstellung vorbereitet
- Sleep Timer (15/30/60/off)
- Share-Action im Fullscreen-Player
- Chromecast-Integration (MVP)
- Favoriten-Sortierung (MVP)
- Verbesserte Netzwerk-Recovery und Stabilitäten
- Player-Timer pausiert bei Buffering
- Eigene Sender mit Stream-URL in Favoriten (MVP)
- Google-Play-Verfuegbarkeit mit Paketname `de.darksoon.radiowave`
- Android-Auto-Browse-Callbacks laufen asynchron statt blockierend
- Android-Auto-Prev/Next nutzt die aktive Auto-Queue zuverlaessiger
- WiFiLock wird nur noch bei aktiver WLAN-Verbindung gehalten
- Landscape-Layout fuer Vollbild-Player (Artwork links, Controls rechts)
- Landscape-adaptives Grid-Layout in Browse und Favorites
- Onboarding als Fullscreen-Erlebnis mit Icons und Animationen neu gestaltet
- Pull-to-Refresh in Home und Browse
- Skeleton-Loader mit Shimmer-Animation in Home, Browse und Favorites
- Empty States in Home, Browse und Favorites mit Icons und CTAs neu gestaltet
- Accessibility-Labels fuer interaktive Icons in Browse und Favorites ergaenzt
- Automotive-Modus hat jetzt Vorrang vor Timeshift-Guard, reduziert Akku-Verbrauch im Auto
- WiFi-Lock-Typ von High-Perf auf Standard reduziert (genuegt fuer Audio-Streaming)
- GitHub-Updater und What's-New-Dialog entfernt — nur noch Play Store

## In Arbeit

- Voice- und Assistant-Play-Intents fuer Senderstarts
- Laufende Live-App-Stabilisierung nach echten Geraete- und Nutzerfaellen

## Als Naechstes

- Favoriten Export/Import (JSON)
- Custom Stations M3U/PLS-Import
- Bitrate-basierte Datenverbrauchs-Schaetzung
- Android Auto Smoke-Test auf echtem Head-Unit
- Radio-Timeshift ueber MVP hinaus (laengerer Buffer, optionaler Seek/LIVE)

## Spaeter

- Weiterer Android-Auto-Ausbau (Recommendations, mehr car-spezifische UX)
- Podcasts-MVP als eigener Bereich
- Android TV / Google TV Basis (Leanback-Launcher, D-Pad-/Focus-Navigation, TV-Layout-MVP)
- Chromecast-Ausbau
- Schneller Playback-Widget-Zugriff
- Mehr Transitions, Motion und Shimmer
- Mehr Unit- und UI-Tests

## Maybe

- Wear OS Companion:
  - eher Remote-/Companion-Use-Case statt vollwertiger Radio-Client
  - Fokus auf Play/Pause, Favoriten, Recents und Player-Status
- Separate Podcast-Quelle:
  - Radio Browser ist senderzentriert, nicht episodenbasiert
  - Podcasts brauchen voraussichtlich einen eigenen Provider
- Cache-DB fuer Browse/Suche:
  - `cached_stations` als zentrale Room-Quelle
  - Cache-first-Flow mit lokalem Emit und Netzwerk-Refresh
  - TTL-Metadaten ueber `search_cache_meta`
  - saubere Verknuepfung zwischen Cache, Favoriten und Recents
- Optionaler Lautstaerke-Slider zusaetzlich zu Mute

## Hinweis

- Diese Roadmap priorisiert Nutzerwert und Stabilitaet.
- Prioritaeten koennen sich durch Testfeedback und Device-Verhalten aendern.
