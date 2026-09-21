# RadioWave Roadmap

[Deutsch](ROADMAP.de.md) | [English](ROADMAP.md)

Stand: 2026-09-21

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
- Sichtbarer Player-Error-Banner mit Retry (Netzwerk, kaputter Stream, etc.)
- Custom-Station-URL-Validierung mit Live-Inline-Feedback
- Android Auto Auto-Resume bei aktivem Telefonat oder Klingeln unterdrueckt
- Android Auto Sender-Revert-Bug behoben (Cancellation-Token-Pattern)
- Android Auto Slow-Start / Endless-Buffer behoben (toleranter Retry, kein Double-Prepare)
- Umfassendes Security-Hardening (AutoService Controller-Whitelist, URI-Scheme-Validierung, Crash-Report-PII-Scrubbing)
- Performance-Pass: Carousel-Smoothness, N+1 DB-Queries entfernt, Heavy-Ranking auf Default-Dispatcher, In-Memory-Tag/Country-Cache, Regex-Hoisting, Settings-Cache
- Player-Listener-Lifecycle und WakeLock-Release-Pfade leak-sicher
- CastManager.release() fuer sauberen Shutdown
- 48dp Touch-Targets in Favoriten-Cards (2026-07-09 erweitert auf Fullscreen-Player, Mini-Player und Browse-Grid)
- Live-Sprachwechsel ohne Activity.recreate
- Datenverlust-Migrationsbug gefixt (Favoriten/Recents von vor dem v1->v3-Upgrade hatten Name/URL verloren; Reparatur-Migration ergaenzt)
- Kostenlose EQ-Presets (Normal, Bass-Boost, Hoehen-Boost, Stimme) — Einstellungen > Ton
- Android Auto: "Weiter hoeren" und "Ueberrasch mich" Kacheln im Browse-Root
- Android Auto: Browse-Baum unterscheidet "Netzwerkfehler" von "wirklich leer" (Top Sender, Genres, Genre-Browsing)
- Android Auto: wiederkehrenden Bug gefixt, bei dem die Wiedergabe nach dem Aussteigen aus dem Auto auf dem Handy-Lautsprecher weiterlief (der Pause-bei-Trennung-Fix konnte durch einen Reconnect-Ausschlag waehrend der Trennsequenz des Autos rueckgaengig gemacht werden)
- Sender-Artwork-URLs werden vor Rendering/Casting auf http/https geprueft (schliesst einen file://\|content://-Injektionspfad)
- Favoriten-Toggle atomar gemacht (schnelles Doppel-Tippen / Android Auto + Handy gleichzeitig konnte den zweiten Tap invertieren)
- GitHub Actions auf Commit-SHAs gepinnt; eine Shell-Injection-Luecke im manuellen Build-Workflow geschlossen
- Einstellungen > Daten hat bereits einen Akku-Optimierungs-Ausnahme-Hinweis mit direktem Link zu den Systemeinstellungen (beim Quick-Wins-Durchgang vom 2026-07-09 als schon vorhanden entdeckt — ein durch Recherche vorgeschlagener Punkt war tatsaechlich schon umgesetzt, nur hier nicht dokumentiert)
- Community-Popularitaetssignal: Sender-Karten zeigen jetzt ein 🔥-Badge aus Radio Browsers oeffentlicher Klick-Zahl bei auffaellig hohen Werten — kein Tracking, nur aggregierte oeffentliche Daten
- Favoriten Export/Import (JSON) — additiver Merge-Restore statt destruktiv, versioniertes Format fuer Abwaertskompatibilitaet
- Custom Stations M3U/PLS-Playlist-Import, zusaetzlich zum bestehenden Einzel-URL-Hinzufuegen
- Bitrate-basierte Datenverbrauchs-Schaetzung, live angezeigt unter der Standard-Qualitaets-Auswahl
- Song-Cover in Medien-Benachrichtigung und Android Auto (Sender-Logo als Fallback, nie leer)
- Live-ICY-Songmetadaten + Sendername als dritte Zeile im Android-Auto-Player
- "Pause bei Auto-Trennung" ueber `androidx.car.app`s `CarConnection`-OS-Signal gehaertet (schliesst das Wireless-AA-Timing-Loch; im Auto verifiziert)
- Wegwischen aus den Recents stoppt die Wiedergabe (aktive Android-Auto-Session bleibt bewusst unberuehrt)
- Optionaler lokaler Spiegel des kompletten Senderverzeichnisses (Opt-in, Intervall + nur WLAN, Onboarding-Schritt) — Suche/Browse ueber alle Sender, auch offline
- Tippfehler-tolerante Fuzzy-Suche, gemischt mit der Verzeichnis-Suche
- TTL-Caching fuer Browse-Listen + Offline-Banner bei Verzeichnis-Ausfaellen
- Content-Policy-Blockliste: Sender designierter Terror-Organisationen auf allen Verzeichnis-Oberflaechen ausgeschlossen und aus lokalen Daten entfernt (Google-Play-Violent-Extremism-Compliance)
- Einstellungen auf DataStore (`SettingsRepository`) mit automatischer Migration aus den alten SharedPreferences
- Stabilitaetsrunde nach dem Audit 2026-09: Sleep-Timer laeuft auf dem Player-Thread und stoppt auch Cast; Mobildaten-Sperre gilt auch bei Fortsetzen, Neustart und Netzwechsel; CI fuehrt die Unit-Tests der Core-Module aus; Sender starten mit ihrer bekannten URL ohne Warten auf das Verzeichnis (Qualitaetsvarianten einmal mit kurzem Zeitlimit geholt und gecacht); Offline-Verzeichnis beachtet "nur WLAN" schon beim ersten Aktivieren, uebersteht "Cache leeren" und entfernt upstream geloeschte Sender; Fuzzy-Suche abseits des Main-Threads; eine gemeinsame Pruefung fuer manuelles Hinzufuegen, Playlist- und Favoriten-Import (http(s) + Host, Groessen-/Eintragslimits, Content-Policy-Blockliste)

## In Arbeit

- Voice- und Assistant-Play-Intents fuer Senderstarts
- Laufende Live-App-Stabilisierung nach echten Geraete- und Nutzerfaellen

## Als Naechstes
- Radio-Timeshift ueber MVP hinaus (laengerer Buffer, optionaler Seek/LIVE)
- Kaltstart liest die Einstellungen noch einmal synchron (`initialSnapshotBlocking`); der Lesevorgang wird jetzt abseits des Main-Threads vorgewaermt. Den Fallback zu entfernen braucht einen per Splash abgewarteten ersten Frame, weil Theme, Onboarding und die Datenrichtlinie des Players nicht mit Standardwerten starten duerfen
- Sleep-Timer verfeinern: eigene Dauer und sanftes Ausblenden

## Spaeter

- Weiterer Android-Auto-Ausbau — verbleibende Kandidaten aus einer Wettbewerbs-Recherche (2026-07, siehe Fertig fuer bereits Erledigtes):
  - Grid-Template fuer Genre-/Tag-/Laender-Browsing statt nur Listen (besser ueberblickbar waehrend der Fahrt)
  - Sprachsuche-Vorschlaege aus lokaler Suchhistorie (kein Tracking)
  - Pruefen, ob der MediaSession-Zustand (Position, Metadaten, Artwork) die neue Swipe-Geste zwischen Medienkarten von Android Autos Coolwalk-Update 2026 uebersteht — neues Bedienmuster im Auto, noch nicht dagegen verifiziert
- Stiller Reconnect-Zustand getrennt vom bestehenden manuellen Fehler-mit-Retry-Banner — nach einem laengeren Verbindungsabbruch (Tunnel, WLAN<->Mobilfunk-Wechsel) N-mal automatisch neu versuchen, bevor der Nutzer manuell auf Retry tippen muss; wiederkehrende Beschwerde bei Konkurrenz-Apps, dort als "gefaehrlich waehrend der Fahrt" eingestuft, weil man aktuell die App neu oeffnen muss
- Podcasts-MVP als eigener Bereich
- Android TV / Google TV Basis (Leanback-Launcher, D-Pad-/Focus-Navigation, TV-Layout-MVP)
- Chromecast-Ausbau
- Schneller Playback-Widget-Zugriff — als favoriten-waehlbar konzipieren (uebersichtliche Liste/Grid zur Auswahl eines beliebigen Favoriten), nicht nur ein "letzten Sender fortsetzen"-Knopf; diese engere Variante ist eine wiederkehrende Beschwerde bei anderen Radio-App-Widgets
- Favoritengruppen (z. B. Auto, Arbeit, Entspannung), gleich auch in Android Auto
- Lokale Song-Merkliste: gehoerte Titel (Metadaten mit Sender und Zeit) merken und teilen — optional, loeschbar, keine Audioaufnahme
- Mehr Transitions, Motion und Shimmer
- Mehr Unit- und UI-Tests
- Baseline Profiles fuer schnelleren Kaltstart
- Play-Console-R8-Empfehlung pruefen (Resource Shrinking + R8-Full/Optimized-Modus) — R8-Minification ist bereits aktiv; die Zusatzstufen brauchen Geraetetests gegen reflection-lastige Deps (Hilt, Media3, Serialization) vor dem Ausliefern
- Echte adaptive Bitrate-Neuwahl bei Netzwerkwechsel (WLAN<->Mobilfunk), nicht nur beim initialen Senderwechsel

## Maybe

- Wear OS Companion:
  - eher Remote-/Companion-Use-Case statt vollwertiger Radio-Client
  - Fokus auf Play/Pause, Favoriten, Recents und Player-Status
- Separate Podcast-Quelle:
  - Radio Browser ist senderzentriert, nicht episodenbasiert
  - Podcasts brauchen voraussichtlich einen eigenen Provider
- Optionaler Lautstaerke-Slider zusaetzlich zu Mute
- Rein lokale Hoerstatistiken (nur auf dem Geraet, kein Server) — passt besser zur Privacy-first-Ausrichtung als die account-gebundenen Insights der Konkurrenz
- "Aehnliche Sender" / Discover-Zeile ueber Radio-Browser-Tag-/Land-/Sprach-Metadaten — kein Tracking oder Account noetig, anders als bei den meisten Empfehlungsfunktionen der Konkurrenz
- Wecker-Funktion / Aufwachen mit Radio — bei manchen Wettbewerbern Standard, aktuell nicht vorhanden
- Hinweis: Offline-Stream-Aufnahme wurde geprueft und bewusst verworfen — Aufnahmerechte variieren je nach Sender-Lizenz, das ist ein rechtliches Risiko, kein Privacy-Thema

## Hinweis

- Diese Roadmap priorisiert Nutzerwert und Stabilitaet.
- Prioritaeten koennen sich durch Testfeedback und Device-Verhalten aendern.
