# Session Start

Nutze fuer neue Chat-Sessions diesen Start:

`Bitte lies SESSION_START.md und folge den Schritten.`

## Schritte fuer den Agenten

1. Lies diese Dateien in genau dieser Reihenfolge:
   - `AGENTS.md`
   - `README.md`
   - `todo.md`
   - `Audits/AUDIT_ALPHA5.md`
   - `.agents/WORKFLOW_NOTES.md` (lokale Commit/Release-Regeln)
2. Fasse den aktuellen Stand in 5-10 Bullet Points zusammen.
3. Nenne offene Blocker und Risiken (falls vorhanden).
4. Starte direkt mit der zuletzt genannten Aufgabe aus der Session oder frage nach der naechsten Prioritaet.

## Wichtige Arbeitsregeln (Merker)

- Commit-Messages immer bilingual (DE + EN).
- Signierte Release-Artefakte immer ueber GitHub Workflow bauen (`Release Build`), nicht lokal signieren.
- Release Build erzeugt jetzt zwei Distributionen:
  - `github`: signierte APK mit eingebautem GitHub-Updater
  - `play`: signiertes AAB ohne direkten APK-Updater / ohne `REQUEST_INSTALL_PACKAGES`
- Release-Status explizit pruefen (`Latest` / `prerelease`) und wie gewuenscht setzen.
- Update-Checker beachten:
  - stable = normaler Release
  - beta = nur fuer Nutzer mit aktiviertem Beta-Kanal

## Optionaler Zusatz vom User

Wenn du direkt ein Thema setzen willst:

`Bitte lies SESSION_START.md und arbeite dann an: <Aufgabe>.`
