# RadioWave

Eine moderne, werbefreie Internet-Radio App für Android.

## Features

- **Kein Account nötig** - Alle Daten lokal auf dem Gerät
- **Keine Werbung** - Komplett werbefrei, kein Tracking
- **45.000+ Sender** - Über die Radio Browser API
- **Android Auto** - Vollständige Auto-Unterstützung
- **Chromecast** - Stream an Smart Speaker senden
- **Material You** - Dynamic Colors & modernes Design

## Tech Stack

- **Language**: Kotlin 2.1.0
- **UI**: Jetpack Compose + Material 3
- **Architecture**: Clean Architecture mit MVVM
- **DI**: Hilt
- **Audio**: Media3 / ExoPlayer
- **Database**: Room
- **Network**: Retrofit + Kotlinx Serialization

## Projektstruktur

```
RadioWave/
├── app/                    # Haupt-App Modul
├── core/
│   ├── core-model/        # Datenmodelle
│   ├── core-database/     # Room Database
│   ├── core-network/      # API Client
│   ├── core-data/         # Repositories
│   ├── core-player/       # Audio Player
│   ├── core-cast/         # Chromecast
│   └── core-ui/           # UI Komponenten
├── feature/
│   ├── feature-home/      # Startbildschirm
│   ├── feature-browse/    # Durchsuchen
│   ├── feature-favorites/ # Favoriten
│   ├── feature-player/    # Player UI
│   ├── feature-custom-stations/ # Eigene Sender
│   └── feature-settings/  # Einstellungen
└── auto/                  # Android Auto
```

## Build

```bash
# Build-Logic zuerst bauen
./gradlew :build-logic:build

# Gesamtes Projekt bauen
./gradlew build

# Lint
./gradlew lint

# Unit Tests
./gradlew test

# Debug APK installieren
./gradlew installDebug
```

## Audit-Status (2026-02-24)

- `./gradlew :build-logic:build` erfolgreich
- `./gradlew build` erfolgreich
- `./gradlew lint` erfolgreich
- `./gradlew test` erfolgreich
- `ktlintCheck` Task ist aktuell nicht im Projekt konfiguriert

Details: siehe `AUDIT_2026-02-24.md`.

## Lizenz

Dieses Projekt ist unter einer proprietären Lizenz mit kommerziellem Nutzungsverbot lizenziert.
Siehe LICENSE.txt für Details.

## Datenschutz

- Keine Datenerhebung
- Keine Analytics
- Keine Werbung
- Alle Daten bleiben lokal auf dem Gerät

## Credits

- Sender-Daten: [Radio Browser API](https://www.radio-browser.info/)
- Icons: Material Design Icons
