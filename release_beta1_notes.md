## RadioWave v0.1.0-beta.1

### DE
Dieses Beta-Release ist der erste stabile Zwischenstand nach der Alpha-Phase und bringt vor allem ein voll nutzbares Auto-Update-System sowie weitere Android-Auto-, Performance- und Stabilitaetsverbesserungen.

Highlights:
- **Auto-Update-System (GitHub Releases)**
  - Update-Pruefung direkt in der App
  - Release-Notes Dialog vor dem Update
  - APK-Download und Installer-Weitergabe
  - Live-Downloadfortschritt (MB / % / Progressbar)
  - Neue Update-Sektion in den Settings (Popup, Auto-Check, manuelle Pruefung, Versionsstatus)
- **Android Auto Stabilitaet**
  - Resume/Silent-Playback-Fall behoben
  - Doppelte Notifications entfernt
  - Low-Load-Modus bei Auto-Verbindung automatisch aktiv
  - Bitrate im Auto auf max. 128 kbps begrenzt
  - Favorites-Priorisierung im Browse-Flow fuer schnelleren Zugriff waehrend der Fahrt
- **Akkuschonung & Netzwerk**
  - Hitzemodus (Thermal Mode) in Settings
  - Netzausfall-Puffer (Timeshift MVP) fuer kurze Unterbrechungen
  - Aktiv nutzbare Streaming-Qualitaetsauswahl (Auto/Niedrig/Mittel/Hoch)
- **Sprache & UX**
  - DE/EN Basis ausgebaut, Sprachwahl in den Settings (System/Deutsch/English)
  - App-Start/Locale-Pfad stabilisiert (AppCompat-Theme/Locale-Fixes)
  - Erststart-Dialog mit den wichtigsten Setup-Hinweisen
- **Performance**
  - Suche jetzt lokal-cache-first mit Netzwerk-Refresh (stabilere Ergebnisliste)
  - Logo-Rendering in der Senderliste optimiert (weniger Scroll-Ruckler)

Wichtige Commits seit `v0.1.0-alpha.6`:
- 113890a feat(update): GitHub In-App Updateflow + Erststart-Dialog / GitHub in-app updater + first-run dialog
- 80c80c3 feat(settings): Update-Center in Settings / add update center in settings
- 9c29807 feat(update): Download-Fortschritt + Android Auto Beta Guide / download progress + Android Auto beta guide
- 7ecfa70 Fix Android Auto resume audio path and remove duplicate notifications
- 360a031 feat(auto): Auto-Streaming auf max 128 kbps begrenzen / cap Android Auto stream bitrate to 128 kbps
- 2a85815 Auto: Low-Load-Modus bei Verbindung automatisch aktivieren / Auto: enable low-load mode automatically on connect
- 0238619 Hitzemodus fuer Auto/Laden eingebaut / Add thermal mode for car/charging
- 5974045 Timeshift-MVP fuer kurze Netzausfaelle / Timeshift MVP for short network drops
- 4005487 feat(streaming): Qualitaetsauswahl aktivieren / enable quality-based stream selection
- ae7a156 feat(i18n): Sprachwahl in Settings / add language selector in settings
- b3d46c0 perf(search): lokale DB-first Suche + stabiles Logo-Rendering / local DB-first search + stable logo rendering
- aa3c76a test(core-player): Timeshift-Guard im Recovery-Test deterministisch setzen / make recovery test deterministic with timeshift guard

### EN
This beta release is the first stable milestone after the alpha phase, with a complete in-app update system plus additional Android Auto, performance, and reliability improvements.

Highlights:
- **Auto-update system (GitHub Releases)**
  - In-app update checks
  - Release-notes dialog before updating
  - APK download and installer handoff
  - Live download progress (MB / % / progress bar)
  - New updates section in settings (popup, auto-check, manual check, version status)
- **Android Auto stability**
  - Fixed resume/silent-playback edge case
  - Removed duplicate notifications
  - Auto low-load mode enabled automatically on car connect
  - Bitrate capped to max 128 kbps while connected to car
  - Favorites-prioritized browse flow for faster in-car access
- **Battery & network**
  - Thermal mode in settings
  - Short-drop timeshift guard (MVP)
  - Active stream-quality selection (Auto/Low/Medium/High)
- **Language & UX**
  - Expanded DE/EN foundation with in-app language selector (System/German/English)
  - Stabilized app startup/locale flow (AppCompat locale/theme fixes)
  - First-run info dialog with key setup hints
- **Performance**
  - Search now emits cache-first local results followed by network refresh
  - Optimized station-logo rendering for smoother list/grid scrolling

