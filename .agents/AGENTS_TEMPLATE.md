# AGENTS.md – Projektkontext für KI-Agenten

<!--
  Diese Datei liegt im Root jedes Projekts.
  Sie gibt Kimi / Claude Code / anderen KI-Tools den projektspezifischen Kontext.
  Die generischen Agenten in .agents/ bleiben unverändert.

  AUSFÜLLEN: Alles in <spitze Klammern> ersetzen.
  LÖSCHEN: Diese Kommentarblöcke vor dem Committen entfernen.
-->

## Projekt-Übersicht

**Name:** <Projektname>
**Typ:** <!-- Flutter App | Godot Game | Website | Web App | API -->
**Status:** <!-- Planung | In Entwicklung | Beta | Live -->
**Primäre Plattform:** <!-- Android | Web | Desktop | Cross-Platform -->

**Kurzbeschreibung:**
<Ein Satz was das Projekt macht und für wen>

## Aktive Agenten

<!--
  Welche Agenten aus .agents/ sollen für dieses Projekt genutzt werden?
  Nicht benötigte Zeilen löschen.
-->

- `.agents/00-orchestrator.md` – Koordination
- `.agents/01-requirements.md` – Anforderungsanalyse
- `.agents/02-architect.md` – Architektur
- `.agents/13-flutter-firebase.md` – Flutter + Firebase
<!-- - `.agents/14-flutter-supabase.md` – Flutter + Supabase (Alternative) -->
<!-- - `.agents/03-frontend.md` – Web Frontend -->
<!-- - `.agents/04-backend.md` – Python Backend -->
<!-- - `.agents/05-godot.md` – Godot Game -->
- `.agents/06-security.md` – Security Review
- `.agents/07-testing.md` – Testing & QA
- `.agents/08-devops.md` – CI/CD & Deployment
- `.agents/09-docs.md` – Dokumentation

## Tech Stack

| Komponente | Technologie |
|------------|-------------|
| <!-- App / Frontend --> | <!-- Flutter / React / Godot --> |
| <!-- Backend --> | <!-- Firebase / Supabase / FastAPI --> |
| <!-- Auth --> | <!-- Firebase Auth / Supabase Auth --> |
| <!-- Datenbank --> | <!-- Firestore / PostgreSQL --> |
| <!-- State --> | <!-- Riverpod / Zustand --> |
| <!-- Region --> | <!-- europe-west3 (Frankfurt) --> |

## Datenmodell

<!--
  Hier das Datenmodell des Projekts beschreiben.
  Für Firebase: Firestore Collections und Felder.
  Für Supabase: PostgreSQL Tabellen und Spalten.
  Der Agent leitet daraus Models, Repositories und Security Rules ab.
-->

### Firebase (Firestore)

```
/<collection>/{id}
  - <feld>: <typ>
  - <feld>: <typ>
  - createdAt: timestamp

/<collection>/{id}/<subcollection>/{id}
  - <feld>: <typ>
```

### Supabase (PostgreSQL) – falls genutzt

```sql
-- <tabelle>
create table public.<tabelle> (
  id uuid primary key,
  -- Felder
  created_at timestamptz default now()
);
```

## Features & Anforderungen

<!--
  Kurze Zusammenfassung der wichtigsten Features.
  Details stehen in FEATURES.md – hier nur das Wesentliche für den Agenten.
-->

### MVP (muss gebaut werden)
- [ ] <Feature 1>
- [ ] <Feature 2>
- [ ] <Feature 3>

### Nice-to-Have (später)
- [ ] <Feature A>
- [ ] <Feature B>

## Security & Datenschutz

- **Region:** <!-- europe-west3 (Frankfurt) für DSGVO -->
- **Auth:** <!-- Email/Passwort | Google | Apple -->
- **Datenschutz:** <!-- DSGVO relevant? User-Löschfunktion nötig? -->
- **Besondere Anforderungen:** <!-- z.B. Einladungslinks nur 1x verwendbar -->

## Aktueller Stand & nächste Schritte

<!--
  Was ist schon fertig? Was ist der nächste Task?
  Wird bei jeder Session aktuell gehalten.
-->

**Fertig:**
- <Was wurde bereits implementiert>

**In Arbeit:**
- <Was wird gerade gebaut>

**Nächster Schritt:**
- <Was soll in dieser Session gebaut werden>

## Offene Fragen / Entscheidungen

<!--
  Technische oder Design-Entscheidungen die noch offen sind.
  Der Agent kann hier Empfehlungen geben.
-->

- [ ] <Offene Frage 1>
- [ ] <Offene Frage 2>

## Wichtige Hinweise für den Agenten

<!--
  Projektspezifische Regeln, Konventionen oder Einschränkungen.
-->

- <Hinweis 1, z.B. "Keine externen Libraries ohne Rückfrage">
- <Hinweis 2, z.B. "Code-Kommentare immer auf Deutsch">
