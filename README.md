# RadioWave

<div align="center">
  <a href="https://github.com/darksoon/RadioWave/actions/workflows/pr-ci.yml">
    <img src="https://github.com/darksoon/RadioWave/actions/workflows/pr-ci.yml/badge.svg?branch=main" alt="PR CI" />
  </a>
  <a href="https://github.com/darksoon/RadioWave/actions/workflows/manual-build.yml">
    <img src="https://github.com/darksoon/RadioWave/actions/workflows/manual-build.yml/badge.svg?branch=main" alt="Manual Build" />
  </a>
  <img src="https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin 2.1.0" />
  <a href="./LICENSE.txt">
    <img src="https://img.shields.io/badge/License-Proprietary-red" alt="License Proprietary" />
  </a>
  <a href="https://buymeacoffee.com/darksoon">
    <img src="https://img.shields.io/badge/Buy%20me%20a%20coffee-Support%20the%20project-FFDD00?logo=buymeacoffee&logoColor=000000" alt="Buy Me a Coffee" />
  </a>

  <h3>Die moderne, werbefreie Internet-Radio-App für Android.</h3>
  <p>⚡ Schnell · 🔒 Privacy-first · 🚫 Kein Tracking · 🎧 45.000+ Sender</p>
  <p>🌐 Entwickler-Website: <a href="https://sven-neurath.de">sven-neurath.de</a></p>
</div>

---

## 🗺️ Roadmap

Die öffentliche Roadmap findest du hier: **[ROADMAP.md](./ROADMAP.md)**

- Fokus: Stabilität + Nutzerwert
- Stand wird laufend mit Releases/Hotfixes aktualisiert
- Vorschläge/Feedback gerne über Issues

## ✨ Highlights

- **Kein Account nötig** – alles lokal auf dem Gerät
- **Komplett werbefrei** – kein Tracking, keine Analytics
- **45.000+ Sender** über Radio Browser
- **Album-Cover** – automatisch von iTunes API (basiert auf Song-Metadaten)
- **Launcher Quick Actions** – Suche, Favoriten, Player und Settings direkt per Long-Press
- **Android Auto** Support mit Favorites, Quick Access, Suche und Prev/Next im Car-Player
- **In-App Updater** mit APK-Download, Fortschritt und Installer-Start direkt aus der App
- **Chromecast** Support
- **Material You** UI mit klarem Anthrazit/Weiss-Theme (Dark/Light)

## 📸 Screenshots

| Home | Entdecken |
|---|---|
| ![Home](docs/screenshots/home.jpg) | ![Entdecken](docs/screenshots/discover.jpg) |

| Favoriten | Fullscreen Player | Einstellungen |
|---|---|---|
| ![Favoriten](docs/screenshots/favorites.jpg) | ![Player](docs/screenshots/player.jpg) | ![Einstellungen](docs/screenshots/settings.jpg) |

## 🧱 Tech Stack

- **Kotlin 2.1.0**
- **Jetpack Compose + Material 3**
- **Clean Architecture + MVVM**
- **Hilt**
- **Media3 / ExoPlayer**
- **Room**
- **Retrofit + Kotlinx Serialization**

## 🚀 Quick Start

```bash
git clone https://github.com/darksoon/RadioWave.git
cd RadioWave
chmod +x gradlew
./gradlew :build-logic:build
./gradlew assembleDebug
```

## 🛠️ Lokale Builds

```bash
# Full build
./gradlew build

# Lint + Tests
./gradlew lint
./gradlew test

# Debug APK installieren
./gradlew installDebug
```

### Build in Proxy / Restricted Network

Wenn der Gradle-Wrapper-Download blockiert ist, nutze einen internen Mirror oder Corporate-Proxy:

```properties
# ~/.gradle/gradle.properties
systemProp.http.proxyHost=<proxy-host>
systemProp.http.proxyPort=<proxy-port>
systemProp.https.proxyHost=<proxy-host>
systemProp.https.proxyPort=<proxy-port>
```

Optional (Unternehmens-Mirror):
- `distributionUrl` in `gradle/wrapper/gradle-wrapper.properties` auf internen Gradle-Mirror setzen.
- CI mit persistentem Gradle-Cache betreiben (`~/.gradle/caches`, `~/.gradle/wrapper`).

## 🔐 Release Signing (optional, recommended)

Create a release keystore once:

```bash
keytool -genkeypair \
  -v \
  -keystore radiowave-release.keystore \
  -alias radiowave \
  -keyalg RSA \
  -keysize 4096 \
  -validity 3650
```

Then configure local signing:

```bash
cp keystore.properties.example keystore.properties
# edit values in keystore.properties
```

`keystore.properties` is ignored by Git. If present, release builds are signed automatically.

## 📲 Installation (ohne Play Store)

1. Öffne die aktuelle Release-Seite:  
   **https://github.com/darksoon/RadioWave/releases/latest**
2. Lade die Datei **`RadioWave-v0.1.0-beta.1.apk`** herunter (bzw. die aktuelle Release-APK mit Versionsnamen).
3. Falls nötig: „Installation aus unbekannten Quellen“ für deinen Browser/Dateimanager erlauben.
4. APK öffnen und installieren.

### Update-Hinweis
- Updates kannst du einfach über neue Releases installieren.
- Wichtig: Die APK muss mit demselben Signatur-Key gebaut sein (ist hier der Fall).

### In-App Update-System (DE/EN)
- Beim App-Start wird intervallbasiert auf Updates geprueft (nicht bei jedem Start sofort).
- In den Settings unter `Updates` gibt es:
  - `Jetzt pruefen / Check now` (manuelle Pruefung)
  - `Update-Popup testen / Test update popup` (zeigt den gleichen Dialog wie beim Auto-Treffer)
- Stable/Beta-Kanal ist waehlbar:
  - `Beta-Updates erhalten` aus: nur stabile Releases
  - `Beta-Updates erhalten` an: GitHub Pre-Releases werden mit angeboten
- Wenn ein Update gefunden wird, laedt die App die APK direkt herunter und startet den Installer aus der App.
- Beim ersten Update auf manchen Geraeten muss `Installationen aus unbekannten Quellen` einmal fuer RadioWave erlaubt werden.

### Sicherheit
- Lade die APK nur von der offiziellen GitHub-Release-Seite.
- Prüfe bei Bedarf die Release-Notes und Dateigröße vor der Installation.

## 🎧 Playback Hinweis (2026-02-25)

- Background-Playback wurde für Screen-Off stabilisiert.
- Kernänderung: dedizierter Foreground-Service-Lifecycle für laufendes Streaming.
- Notification Media Controls wurden weiter aufgeräumt:
  - direkter Sprung in den Player aus der Notification
  - sauberere Play/Pause/Prev/Next/Stop-Steuerung
  - bessere Metadaten- und Statusdarstellung

## Android Auto

- Browse ist auf schnelle In-Car-Nutzung ausgelegt: `Favoriten`, `Quick Access`, `Top Sender`, `Genres`
- `Quick Access` kombiniert Favoriten und Recents fuer schnelleren Wiedereinstieg
- Suche kombiniert lokale und entfernte Treffer robuster fuer Voice- und Texteingaben
- Im Car-Player stehen `Prev/Next` jetzt als echte Sendernavigation zur Verfuegung

## 🚀 Launcher Quick Actions

- Per Long-Press auf das App-Icon stehen Schnellaktionen für `Suche`, `Favoriten`, `Player` und `Settings` bereit.
- Auf manchen Launchern hilft es nach einem Update, das Icon einmal neu auf den Homescreen zu ziehen.

## 🤖 GitHub Actions

### Manual Android Build
Im Repository unter **Actions → Manual Android Build → Run workflow**.

- Standard-Task: `assembleDebug`
- Optional: `assembleRelease` oder `build`
- APK/AAB nach dem Lauf unter **Artifacts**

### PR CI
- Läuft automatisch bei **Push** und **Pull Requests** auf `main`
- Zusätzlich manuell startbar über **workflow_dispatch**

### Release Build
- Signed Releases laufen ueber **Actions → Release Build**
- Workflow:
  - setzt `app.versionName` und `app.versionCode`
  - baut eine signierte Release-APK
  - erstellt Tag + GitHub Release
  - haengt die APK direkt als Release-Asset an
- Tag-Format: `v0.1.0-beta.2`
- Wichtig fuer den Update-Checker:
  - stabile App-Updates nur mit normalem Release
  - Beta-Updates nur fuer Nutzer mit aktiviertem Beta-Kanal

## 📦 Projektstruktur

```text
RadioWave/
├── app/                    # Haupt-App Modul
├── core/
│   ├── core-model/
│   ├── core-database/
│   ├── core-network/
│   ├── core-data/
│   ├── core-player/
│   ├── core-cast/
│   └── core-ui/
├── feature/
│   ├── feature-home/
│   ├── feature-browse/
│   ├── feature-favorites/
│   ├── feature-player/
│   ├── feature-custom-stations/
│   └── feature-settings/
└── auto/
```

## 🔐 Datenschutz

- Keine Datenerhebung
- Keine Analytics
- Keine Werbung
- Alle Daten bleiben lokal auf dem Gerät

## 📜 Lizenz

Proprietäre Lizenz mit kommerziellem Nutzungsverbot.  
Siehe [LICENSE.txt](./LICENSE.txt).

## ❤️ Support

Wenn dir RadioWave gefällt und du die Entwicklung unterstützen willst:

☕ **Buy Me a Coffee:** https://buymeacoffee.com/darksoon

## 🙌 Credits

- Sender-Daten: [Radio Browser API](https://www.radio-browser.info/)
- Album-Cover: [iTunes Search API](https://developer.apple.com/library/archive/documentation/AudioVideo/Conceptual/iTuneSearchAPI/)
- Icons: Material Design Icons
