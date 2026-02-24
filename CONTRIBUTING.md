# Contributing to RadioWave

Danke für deinen Beitrag zu RadioWave! 🎧

## Workflow

1. Fork/Branch erstellen
2. Änderungen lokal bauen und testen
3. Commit mit klarer Message (Conventional Commits bevorzugt)
4. Pull Request öffnen

## Branch Naming

- `feat/<kurze-beschreibung>`
- `fix/<kurze-beschreibung>`
- `chore/<kurze-beschreibung>`

## Commit Style (empfohlen)

- `feat: ...`
- `fix: ...`
- `chore: ...`
- `docs: ...`
- `refactor: ...`
- `test: ...`
- `ci: ...`

## Lokale Checks vor PR

```bash
./gradlew :build-logic:build
./gradlew build
./gradlew lint
./gradlew test
```

## Pull Request Regeln

- PR klein und fokussiert halten
- Kurz beschreiben: Problem, Lösung, Risiken
- Bei UI-Änderungen Screenshots/GIF anhängen
- Keine Secrets/Keystores committen

## Code Guidelines

- Kotlin + Compose best practices
- Saubere, modulare Architektur beibehalten
- Öffentliche APIs und komplexe Stellen dokumentieren
