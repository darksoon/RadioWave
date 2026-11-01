# RadioWave

[Deutsch](README.de.md) | [English](README.md)

<div align="center">

  <img src="docs/assets/banner.jpg" alt="RadioWave" width="720" />

  <a href="https://github.com/darksoon/RadioWave/actions/workflows/pr-ci.yml">
    <img src="https://github.com/darksoon/RadioWave/actions/workflows/pr-ci.yml/badge.svg?branch=main" alt="CI" />
  </a>
  <a href="https://github.com/darksoon/RadioWave/releases/latest">
    <img src="https://img.shields.io/github/v/release/darksoon/RadioWave?include_prereleases&label=release&color=4CAF50" alt="Neueste Version" />
  </a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white" alt="Android 8.0+" />
  <img src="https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin 2.3" />
  <a href="./LICENSE.txt">
    <img src="https://img.shields.io/badge/License-GPL--3.0--or--later-blue" alt="License GPL-3.0-or-later" />
  </a>
  <a href="https://ko-fi.com/darksoon">
    <img src="https://img.shields.io/badge/Ko--fi-Support%20the%20project-29ABE0?logo=kofi&logoColor=white" alt="Ko-fi" />
  </a>

  <h3>Die moderne, werbefreie Internet-Radio-App fuer Android.</h3>
  <p>Schnell | Privacy-first | Kein Tracking | 45.000+ Sender</p>
  <p>Projekt-Website: <a href="https://radiowave.sven-neurath.de">radiowave.sven-neurath.de</a></p>
</div>

---

## Roadmap

Die oeffentliche Roadmap liegt in [ROADMAP.de.md](./ROADMAP.de.md).

- Fokus: Stabilitaet und Nutzerwert
- Wird laufend mit Releases und Polishing aktualisiert
- Feedback gerne ueber GitHub Issues
- RadioWave ist live auf Google Play; Beta-/Sideload-Hinweise gelten nur noch fuer GitHub-APKs und Testinstallationen

## Verfuegbarkeit

- Website: https://radiowave.sven-neurath.de
- Google Play: https://play.google.com/store/apps/details?id=de.darksoon.radiowave
- GitHub Releases: https://github.com/darksoon/RadioWave/releases/latest
- Android-Paketname / `applicationId`: `de.darksoon.radiowave`

## Highlights

**Neu in 1.0.20:** Auto-Modus mit großen Tasten fürs Armaturenbrett, Audio-Spektrum im Player, anpassbare Startseite (Genres, sortierte Favoriten, „Weiterhören“), gemerkte Titel, neu gegliederte Einstellungen mit Suche, Hell/Dunkel/System, getrennte Qualität für WLAN und Mobil, Wiedergabe nur im WLAN, Ausweich-Streams und ein neues Design.

- Kein Account noetig; Favoriten, Verlauf und Einstellungen bleiben auf dem Geraet
- Komplett werbefrei, keine Analytics, kein Verhaltenstracking
- 45.000+ Sender ueber Radio Browser
- Album-Cover ueber die iTunes Search API
- Launcher Quick Actions fuer `Suche`, `Favoriten`, `Player` und `Settings`
- Android Auto mit Favoriten, Quick Access, Suche und Vor/Zurueck im Car-Player
- Chromecast / Google Cast mit direkter Geräteauswahl aus dem Player
- Sleep Timer und direkte Teilen-Aktion im Fullscreen-Player
- Kostenlose EQ-Presets (Bass-Boost, Höhen-Boost, Stimme) — keine Paywall
- Favoriten-Backup/-Wiederherstellung (JSON) und Sender-Bulk-Import aus M3U/PLS-Playlists
- Lokaler Crash-Report mit Export/Share und vorbereiteter GitHub-Issue-Erstellung

## Screenshots

Eine Tour durch das neue Design gibt es auf der [Website](https://radiowave.sven-neurath.de).

| Android Auto 1 | Android Auto 2 |
|---|---|
| ![Android Auto 1](docs/screenshots/Android_Auto1.png) | ![Android Auto 2](docs/screenshots/Android_Auto2.png) |

| Android Auto 3 | Android Auto 4 |
|---|---|
| ![Android Auto 3](docs/screenshots/Android_Auto3.png) | ![Android Auto 4](docs/screenshots/Android_Auto4.png) |

## Tech Stack

- Kotlin 2.3
- Jetpack Compose + Material 3
- DataStore
- Clean Architecture + MVVM
- Hilt
- Media3 / ExoPlayer
- Room
- Retrofit + Kotlinx Serialization

## Quick Start

```bash
git clone https://github.com/darksoon/RadioWave.git
cd RadioWave
chmod +x gradlew
./gradlew :build-logic:build
./gradlew assembleDebug
```

## Lokale Builds

```bash
# Voller Build
./gradlew build

# Lint + Tests
./gradlew lint
./gradlew test

# Debug APK installieren
./gradlew installDebug

# GitHub-Flavor-Debug-Build
./gradlew :app:assembleGithubDebug

# Play-Store-Debug-Build
./gradlew :app:assemblePlayDebug
```

### Restriktive Netzwerke / Proxy

Wenn Gradle-Downloads blockiert sind, nutze Mirror oder Proxy:

```properties
# ~/.gradle/gradle.properties
systemProp.http.proxyHost=<proxy-host>
systemProp.http.proxyPort=<proxy-port>
systemProp.https.proxyHost=<proxy-host>
systemProp.https.proxyPort=<proxy-port>
```

## Release Signing

Signierte Release-Builds laufen ueber GitHub Actions.

- Fuer GitHub / Direktdownload ist die signierte `github`-APK gedacht
- Fuer die Google-Play-Auslieferung ist das signierte `play`-App-Bundle (`.aab`) gedacht

Nur fuer lokale Experimente:

```bash
cp keystore.properties.example keystore.properties
# keystore.properties bearbeiten
```

## Installation

### Google Play

Installiere RadioWave ueber Google Play:

https://play.google.com/store/apps/details?id=de.darksoon.radiowave

### GitHub-APK

1. Oeffne die aktuelle Release-Seite:
   `https://github.com/darksoon/RadioWave/releases/latest`
2. Lade das aktuelle APK-Asset herunter.
3. Erlaube Installationen aus unbekannten Quellen, falls dein Geraet danach fragt.
4. Oeffne die APK und installiere sie.

## Player-Verbesserungen

- Funkloch und Netzwechsel: Wiederverbindung mit wachsenden Pausen (bis zu 10 Versuche); fällt eine Stream-Adresse aus, wechselt die App auf eine andere desselben Senders, nie unsicherer und nie mit höherer Bitrate
- Navigationsansagen machen den Sender leiser (oder auf Wunsch stumm) statt ihn zu pausieren, er bleibt live
- Getrennte Qualität für WLAN und Mobilfunk, optional Wiedergabe nur im WLAN
- Der Wiedergabe-Timer zeigt jetzt echte Hoerzeit und pausiert bei Buffering
- Im Fullscreen-Player gibt es jetzt Teilen und Sleep Timer
- Favoriten lassen sich im Favoriten-Screen einfacher umsortieren
- Aktive Chromecast-Sessions lassen sich direkt aus dem Fullscreen-Player starten, Senderwechsel werden an den TV uebergeben und Play/Pause in der App steuert das Cast-Ziel

## Android Auto

- Browse ist fuer In-Car-Nutzung optimiert: `Favoriten`, `Quick Access`, `Top Sender`, `Genres`
- Quick Access kombiniert Favoriten und Recents
- Suche fuehrt lokale und entfernte Treffer robuster zusammen
- Vor/Zurueck im Car-Player funktioniert als echte Sendernavigation
- Browse/Search-Callbacks laufen asynchron, damit Android Auto nicht durch Repository- oder Netzwerkzugriffe blockiert
- Der Player haelt den WiFiLock nur noch bei aktiver WLAN-Verbindung; WakeLock bleibt fuer stabile Wiedergabe aktiv

Siehe auch: [docs/ANDROID_AUTO_DEV_MODE.de.md](docs/ANDROID_AUTO_DEV_MODE.de.md)

## Chromecast / Google Cast

- Cast-Geraete koennen direkt aus dem Fullscreen-Player ausgewaehlt werden
- Beim Start einer Cast-Session wechselt der aktuelle Sender auf den TV und die parallele Handy-Wiedergabe wird gestoppt
- Senderwechsel waehrend des Castens aktualisieren den TV-Stream statt lokal einen zweiten Stream zu starten
- Play/Pause in der App steuert die aktive Cast-Session
- Der Player zeigt sichtbar an, wenn die Wiedergabe gerade auf dem TV laeuft
- Alexa / Echo ist **nicht** Teil dieser Cast-Integration und wuerde eine separate Amazon-spezifische Loesung brauchen

## GitHub Actions

### Manual Android Build

Unter `Actions -> Manual Android Build -> Run workflow`.

### PR CI

- Laeuft automatisch bei Pushes und Pull Requests auf `main`
- Kann auch manuell gestartet werden
- Nutzt Gradle Build Cache plus Configuration Cache
- Die Test-Task-Steuerung ist configuration-cache-sicher, damit CI bei Modulen ohne Unit-Tests nicht mehr kippt

### Release Build

- Signierte Releases entstehen ueber `Actions -> Release Build`
- Der Workflow:
  - aktualisiert `app.versionName` und `app.versionCode`
  - baut eine signierte Play-APK plus ein Play-Store-App-Bundle (`.aab`)
  - erstellt Tag und GitHub Release
  - haengt Release-Artefakte inklusive nativer Debug-Symbole fuer die Play Console an
- Die Workflows sind fuer die GitHub-Actions-Node-24-Umstellung vorbereitet (`actions/checkout@v5`)

## Datenschutz

- Keine Analytics, keine Werbung, kein Account, keine Tracking-SDKs
- Favoriten, Verlauf, eigene Sender und Einstellungen werden nur auf dem Geraet gespeichert
- Crash-Reports werden nur lokal gespeichert und nur aktiv vom Nutzer geteilt

Eine Internetradio-App muss mit dem Internet sprechen. Das sind alle Verbindungen, die RadioWave aufbaut:

| Ziel | Wann | Was wird uebertragen | Abschalten |
|---|---|---|---|
| Radio-Browser-API (`all.api.radio-browser.info`) | Stoebern, Suche, Topliste, Genres/Laender, Abfrage der Stream-Qualitaeten, Sync des Offline-Verzeichnisses | Suchbegriffe, Filter und Sender-IDs, dazu die IP-Adresse | Fuer das Verzeichnis noetig; das Offline-Verzeichnis (Einstellungen -> Speicher & Daten) reduziert Live-Anfragen |
| Radio-Browser-Klickzaehler (`json/url/<sender-id>`) | Einmal pro Sendersitzung, nachdem der Stream laeuft | Die Sender-ID (und die IP-Adresse) — fliesst in die Beliebtheitsliste der Community | Einstellungen -> Speicher & Daten -> Datenschutz -> „Senderaufrufe an Radio Browser melden“ |
| iTunes Search API (`itunes.apple.com`) | Wenn ein Sender Interpret/Titel, aber kein eigenes Cover liefert | Interpret und Titel des laufenden Songs (und die IP-Adresse) | Einstellungen -> Speicher & Daten -> Datenschutz -> „Song-Cover online suchen“ (dann erscheint das Senderlogo) |
| Stream-Server der Sender | Waehrend der Wiedergabe | Eine normale HTTP(S)-Anfrage fuer den Stream (IP-Adresse, User-Agent `RadioWave/1.0`) | Gehoert zur Wiedergabe; reine HTTP-Sender lassen sich unter Einstellungen -> Speicher & Daten ausblenden |
| Server fuer Senderlogos/Cover | Wenn eine Liste, der Player, die Benachrichtigung oder Android Auto ein Logo oder Cover zeigt | Eine Bildanfrage an den im Verzeichniseintrag genannten Server (IP-Adresse) | Nicht einstellbar |
| Google Cast (Google-Play-Dienste, Default Media Receiver) | Nur beim Oeffnen des Cast-Dialogs oder beim Casten | Geraetesuche im lokalen Netz; beim Casten gehen Stream-URL, Sendername und Logo-URL an das Cast-Geraet, das sich dann selbst mit dem Stream-Server verbindet | Cast nicht nutzen |
| GitHub | Nur beim Antippen eines GitHub-Links oder von „GitHub-Issue oeffnen“ | Was du dort selbst absendest | Nicht antippen |

Wiedergabeverlauf, Favoriten und Geraetekennungen werden nirgendwohin gesendet.

## Lizenz

RadioWave steht unter der GNU General Public License v3.0 oder spaeter.
Siehe [LICENSE.txt](./LICENSE.txt).

Fuer Kotlin-Quelldateien wird `SPDX-License-Identifier: GPL-3.0-or-later` verwendet.

## Links

- Website: https://radiowave.sven-neurath.de
- Google Play: https://play.google.com/store/apps/details?id=de.darksoon.radiowave
- GitHub Releases: https://github.com/darksoon/RadioWave/releases/latest
- Ko-fi: https://ko-fi.com/darksoon

## Credits

- Senderdaten: [Radio Browser API](https://www.radio-browser.info/)
- Album-Cover: [iTunes Search API](https://developer.apple.com/library/archive/documentation/AudioVideo/Conceptual/iTuneSearchAPI/)
- Icons: Material Design Icons
