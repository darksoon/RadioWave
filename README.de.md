# RadioWave

[Deutsch](README.de.md) | [English](README.md)

<div align="center">
  <a href="https://github.com/darksoon/RadioWave/actions/workflows/pr-ci.yml">
    <img src="https://github.com/darksoon/RadioWave/actions/workflows/pr-ci.yml/badge.svg?branch=main" alt="PR CI" />
  </a>
  <a href="https://github.com/darksoon/RadioWave/actions/workflows/manual-build.yml">
    <img src="https://github.com/darksoon/RadioWave/actions/workflows/manual-build.yml/badge.svg?branch=main" alt="Manual Build" />
  </a>
  <img src="https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin 2.1.0" />
  <a href="./LICENSE.txt">
    <img src="https://img.shields.io/badge/License-GPL--3.0--or--later-blue" alt="License GPL-3.0-or-later" />
  </a>
  <a href="https://ko-fi.com/darksoon">
    <img src="https://img.shields.io/badge/Ko--fi-Support%20the%20project-29ABE0?logo=kofi&logoColor=white" alt="Ko-fi" />
  </a>

  <h3>Die moderne, werbefreie Internet-Radio-App fuer Android.</h3>
  <p>Schnell | Privacy-first | Kein Tracking | 45.000+ Sender</p>
  <p>Entwickler-Website: <a href="https://sven-neurath.de">sven-neurath.de</a></p>
</div>

---

## Roadmap

Die oeffentliche Roadmap liegt in [ROADMAP.de.md](./ROADMAP.de.md).

- Fokus: Stabilitaet und Nutzerwert
- Wird laufend mit Releases und Hotfixes aktualisiert
- Feedback gerne ueber GitHub Issues

## Highlights

- Kein Account noetig, alles bleibt lokal auf dem Geraet
- Komplett werbefrei, keine Analytics, kein Verhaltenstracking
- 45.000+ Sender ueber Radio Browser
- Album-Cover ueber die iTunes Search API
- Launcher Quick Actions fuer `Suche`, `Favoriten`, `Player` und `Settings`
- Android Auto mit Favoriten, Quick Access, Suche und Vor/Zurueck im Car-Player
- Eingebauter Updater mit APK-Download, Fortschritt und Installer-Start
- Lokaler Crash-Report mit Export/Share und vorbereiteter GitHub-Issue-Erstellung
- Chromecast-Unterstuetzung

## Screenshots

| Home | Suche |
|---|---|
| ![Home](docs/screenshots/home.jpg) | ![Suche](docs/screenshots/search.jpg) |

| Favoriten | Fullscreen Player | Einstellungen |
|---|---|---|
| ![Favoriten](docs/screenshots/favorites.jpg) | ![Fullscreen Player](docs/screenshots/fullscreen_player.jpg) | ![Einstellungen](docs/screenshots/settings.jpg) |

| Android Auto 1 | Android Auto 2 |
|---|---|
| ![Android Auto 1](docs/screenshots/Android_Auto1.png) | ![Android Auto 2](docs/screenshots/Android_Auto2.png) |

| Android Auto 3 | Android Auto 4 |
|---|---|
| ![Android Auto 3](docs/screenshots/Android_Auto3.png) | ![Android Auto 4](docs/screenshots/Android_Auto4.png) |

## Tech Stack

- Kotlin 2.1.0
- Jetpack Compose + Material 3
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

Nur fuer lokale Experimente:

```bash
cp keystore.properties.example keystore.properties
# keystore.properties bearbeiten
```

## Installation

1. Oeffne die aktuelle Release-Seite:
   `https://github.com/darksoon/RadioWave/releases/latest`
2. Lade das aktuelle APK-Asset herunter.
3. Erlaube Installationen aus unbekannten Quellen, falls dein Geraet danach fragt.
4. Oeffne die APK und installiere sie.

## In-App Update-System

- Die App prueft Updates intervallbasiert, nicht bei jedem einzelnen Start
- In `Settings -> Updates` kannst du:
  - manuell nach Updates suchen
  - das Update-Popup testen
  - automatische Pruefungen aktivieren oder deaktivieren
  - Popup-Verhalten aktivieren oder deaktivieren
- Stable- und Beta-Kanal sind getrennt:
  - Beta aus: nur stabile Releases
  - Beta an: GitHub Pre-Releases werden mit beruecksichtigt
- Wenn ein Update gefunden wird, laedt die App die APK direkt herunter und startet den Installer

## Android Auto

- Browse ist fuer In-Car-Nutzung optimiert: `Favoriten`, `Quick Access`, `Top Sender`, `Genres`
- Quick Access kombiniert Favoriten und Recents
- Suche fuehrt lokale und entfernte Treffer robuster zusammen
- Vor/Zurueck im Car-Player funktioniert als echte Sendernavigation

Siehe auch: [docs/ANDROID_AUTO_DEV_MODE.de.md](docs/ANDROID_AUTO_DEV_MODE.de.md)

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
  - baut eine signierte Release-APK
  - erstellt Tag und GitHub Release
  - haengt die APK als Release-Asset an

## Datenschutz

- Keine Analytics
- Keine Werbung
- Keine versteckte Telemetrie
- Crash-Reports werden nur lokal gespeichert und nur aktiv vom Nutzer geteilt

## Lizenz

RadioWave steht unter der GNU General Public License v3.0 oder spaeter.
Siehe [LICENSE.txt](./LICENSE.txt).

Fuer Kotlin-Quelldateien wird `SPDX-License-Identifier: GPL-3.0-or-later` verwendet.

## Support

Wenn du das Projekt unterstuetzen willst:

- Ko-fi: https://ko-fi.com/darksoon

## Credits

- Senderdaten: [Radio Browser API](https://www.radio-browser.info/)
- Album-Cover: [iTunes Search API](https://developer.apple.com/library/archive/documentation/AudioVideo/Conceptual/iTuneSearchAPI/)
- Icons: Material Design Icons
