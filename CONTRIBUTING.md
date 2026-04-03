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

- `feat(scope): ...`
- `fix(scope): ...`
- `perf(scope): ...`
- `refactor(scope): ...`
- `docs(scope): ...`
- `test(scope): ...`
- `ci(scope): ...`
- `chore(scope): ...`

## Release-Notes Hygiene

Die GitHub-Release-Notes werden aus den Commit-Betreffzeilen erzeugt.
Deshalb fuer user-sichtbare Aenderungen bitte bevorzugt:

- `feat(...)` fuer neue Funktionen
- `fix(...)` fuer echte Bugfixes
- `perf(...)` fuer spuerbare Performance-Verbesserungen
- `refactor(...)` nur wenn sich das Nutzerverhalten merklich verbessert

Empfehlungen fuer den Betreff:

- kurz und klar
- moeglichst nutzerorientiert statt nur intern-technisch
- gerne bilingual im Format `DE / EN`
- keine generischen Titel wie `misc changes`, `update stuff`, `bugfix`

Beispiele:

- `fix(player): lockscreen-controls reparieren / fix lockscreen controls`
- `fix(auto): stationsnamen bereinigen / sanitize station names`
- `feat(settings): sleep timer hinzufuegen / add sleep timer`

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
