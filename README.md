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

  <h3>The modern, ad-free internet radio app for Android.</h3>
  <p>Fast | Privacy-first | No tracking | 45,000+ stations</p>
  <p>Project website: <a href="https://radiowave.sven-neurath.de">radiowave.sven-neurath.de</a></p>
</div>

---

## Roadmap

The public roadmap is available in [ROADMAP.md](./ROADMAP.md).

- Focus: stability and user value
- Updated continuously with releases and polish passes
- Feedback is welcome through GitHub Issues

## Availability

- Website: https://radiowave.sven-neurath.de
- Google Play: https://play.google.com/store/apps/details?id=de.darksoon.radiowave
- GitHub Releases: https://github.com/darksoon/RadioWave/releases/latest
- Android package / `applicationId`: `de.darksoon.radiowave`

## Highlights

- No account required, everything stays on the device
- Completely ad-free, no analytics, no behavior tracking
- 45,000+ stations via Radio Browser
- Album art lookup via the iTunes Search API
- Launcher quick actions for `Search`, `Favorites`, `Player`, and `Settings`
- Android Auto support with favorites, quick access, search, and previous/next in the car player
- Sleep timer and direct share action in the fullscreen player
- Local crash-report export with share action and prefilled GitHub issue handoff

## Screenshots

| Home | Search |
|---|---|
| ![Home](docs/screenshots/home.jpg) | ![Search](docs/screenshots/search.jpg) |

| Favorites | Fullscreen Player | Settings |
|---|---|---|
| ![Favorites](docs/screenshots/favorites.jpg) | ![Fullscreen Player](docs/screenshots/fullscreen_player.jpg) | ![Settings](docs/screenshots/settings.jpg) |

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

## Local Builds

```bash
# Full build
./gradlew build

# Lint + tests
./gradlew lint
./gradlew test

# Install debug APK
./gradlew installDebug

# GitHub-flavor debug build
./gradlew :app:assembleGithubDebug

# Play Store debug build
./gradlew :app:assemblePlayDebug
```

### Restricted Networks / Proxy

If Gradle downloads are blocked, use a mirror or proxy:

```properties
# ~/.gradle/gradle.properties
systemProp.http.proxyHost=<proxy-host>
systemProp.http.proxyPort=<proxy-port>
systemProp.https.proxyHost=<proxy-host>
systemProp.https.proxyPort=<proxy-port>
```

## Release Signing

Signed release builds are handled through GitHub Actions.

- For GitHub/direct download use the signed `github` APK
- For Google Play distribution use the signed `play` App Bundle (`.aab`)

For local signing experiments only:

```bash
cp keystore.properties.example keystore.properties
# edit keystore.properties
```

## Installation

### Google Play

Install RadioWave from Google Play:

https://play.google.com/store/apps/details?id=de.darksoon.radiowave

### GitHub APK

1. Open the latest release page:
   `https://github.com/darksoon/RadioWave/releases/latest`
2. Download the current APK asset.
3. Allow installs from unknown sources if your device asks for it.
4. Open the APK and install it.

## Player Improvements

- Short network interruptions are handled more gracefully with improved reconnect tuning
- The playback timer now reflects actual listening time and pauses during buffering
- Fullscreen player includes a share action and a sleep timer
- Favorites can be reordered more easily from the favorites screen

## Android Auto

- Browse is optimized for in-car usage: `Favorites`, `Quick Access`, `Top Stations`, `Genres`
- Quick Access combines favorites and recents
- Search merges local and remote results more robustly
- Previous/next in the car player now works as real station navigation

See also: [docs/ANDROID_AUTO_DEV_MODE.md](docs/ANDROID_AUTO_DEV_MODE.md)

## GitHub Actions

### Manual Android Build

Available under `Actions -> Manual Android Build -> Run workflow`.

### PR CI

- Runs automatically on pushes and pull requests to `main`
- Can also be started manually
- Uses Gradle build cache plus configuration cache
- Test-task gating is configuration-cache-safe, so CI no longer breaks on modules without unit tests

### Release Build

- Signed releases are created through `Actions -> Release Build`
- The workflow:
  - updates `app.versionName` and `app.versionCode`
  - builds a signed Play APK plus a Play Store App Bundle (`.aab`)
  - creates a tag and GitHub release
  - uploads release assets, including native debug-symbol artifacts for Play Console troubleshooting
- Workflows are prepared for the GitHub Actions Node 24 migration (`actions/checkout@v5`)

## Privacy

- No analytics
- No ads
- No hidden telemetry
- Crash reports are stored locally only and shared explicitly by the user

## License

RadioWave is licensed under the GNU General Public License v3.0 or later.
See [LICENSE.txt](./LICENSE.txt).

For source files, Kotlin code uses `SPDX-License-Identifier: GPL-3.0-or-later`.

## Links

- Website: https://radiowave.sven-neurath.de
- Google Play: https://play.google.com/store/apps/details?id=de.darksoon.radiowave
- GitHub Releases: https://github.com/darksoon/RadioWave/releases/latest
- Ko-fi: https://ko-fi.com/darksoon

## Credits

- Station data: [Radio Browser API](https://www.radio-browser.info/)
- Album art: [iTunes Search API](https://developer.apple.com/library/archive/documentation/AudioVideo/Conceptual/iTuneSearchAPI/)
- Icons: Material Design Icons
