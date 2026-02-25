# RadioWave Roadmap (Public)

Stand: 2026-02-25

Diese Datei ist fuer GitHub gedacht und zeigt den Produktstatus kompakt.

## Fertig
- Core-Architektur (`core-model`, `core-database`, `core-network`, `core-data`)
- Basis-Player (`core-player`) mit stabilisiertem Background-Playback
- Home, Browse/Suche, Favoriten, Fullscreen-Player, Settings-Basis
- Bottom Navigation + stabiles Routing + Floating Mini-Player
- Favoriten und Recents in Room
- Fullscreen-Player Controls: Play/Pause, Mute, Random, Previous, Favorite
- LIVE-Fortschritt im Fullscreen-Player (UI-Animation)
- Home/Browse/Favorites UI-Polish (Glass-Look, bessere Karten, Performance-Tuning)
- Security-Basis gehaertet (`allowBackup=false`, `cleartextTrafficPermitted=false`)
- Optional sichtbare HTTP-Kennzeichnung in Browse + Settings-Toggle fuer unsichere Streams

## In Arbeit
- Audio Focus Verhalten bei Unterbrechungen (Sprachnachrichten/Anrufe)
- Notification Media Controls (Play/Pause/Next/Stop)
- Settings Rework als Card-Sektionen (`Allgemein`, `Sound`, `Speicher & Daten`, `Info`)
- Mini-Player Metadaten als Marquee bei langen Titeln
- Stream-Qualitaet im Fullscreen-Player anzeigen

## Geplant (naechste Schritte)
- Sleep Timer (15/30/60/off)
- Share Action im Fullscreen-Player
- Favoriten Export/Import (JSON)
- Custom Stations (manuelle URL) + M3U/PLS Import
- Mobile-Daten-Policy (nur WLAN / mobile Daten erlauben)
- Datennutzung sichtbar machen (geschaetzt ueber Bitrate)
- Settings Info-Links: GitHub Repo, GitHub Issues, Website, Support/Buy a Coffee

## Spaeter
- Android Auto Ausbau (MediaLibraryService + Browse-Tree)
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
- Optionale Qualitaets-Presets pro Sender, wenn mehrere Stream-Varianten vorhanden sind
- Optionaler Lautstaerke-Slider zusaetzlich zu Mute

## Hinweis
- Diese Roadmap priorisiert Nutzerwert und Stabilitaet.
- Reihenfolge kann sich je nach Test-Feedback und Device-Verhalten aendern.
