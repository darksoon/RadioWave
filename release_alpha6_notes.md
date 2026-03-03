## RadioWave v0.1.0-alpha.6

### DE
Dieses Alpha-Release bringt die erste nutzbare Android Auto Einbindung inklusive Favoriten/Recents und stabilerem Playback-Resume beim Wiederverbinden.

Wichtigste Punkte:
- Android Auto Integration auf Basis Media3 (`MediaLibraryService`)
- Browse-Struktur in Auto: Favorites + Recents
- Sender-Logos (Favicon) werden als Artwork an Auto uebergeben
- Startpfad fuer Auto-Playback gehaertet (URI-Fallbacks, Mapping, Retry)
- Letzten Sender persistieren + optionales Autoplay bei Android Auto Verbindung
- CI-Workflow gefixt: Required Check `build-lint-test` laeuft jetzt auf `push` nach `main`
- Signed Release Workflow robuster (klare Secret-Fehler + apksigner-Fallback-Lookup)

Commits seit v0.1.0-alpha.5:
- d55b250 feat(auto): resume last station on car reconnect with autoplay toggle
- 48d96f6 feat(auto): add media3 auto service and stabilize playback handoff
- f4ed83b Audit 2-5: Backup/Doku/Build-Doku/Tests verbessern (DE/EN)
- 3acf946 Hotfix: nur aeusserer Play-Ring rotiert (DE/EN)
- 246c8c5 Fullscreen: Qualitaetsanzeige + Play-Ring-Animation (DE/EN)
- 0d428a5 Security config härten + BuyMeACoffee-Link korrigieren (DE/EN)
- 78c94dc Gitignore: lokalen Audits-Ordner ausschliessen
- 9e4e40b Mini-Player Lauftext/Marquee fixen und polieren (DE/EN)

### EN
This alpha release introduces the first usable Android Auto integration, including Favorites/Recents browsing and more stable playback resume on reconnect.

Highlights:
- Android Auto integration based on Media3 (`MediaLibraryService`)
- Auto browse structure: Favorites + Recents
- Station logos (favicons) are forwarded as artwork to Auto
- Hardened Auto playback startup path (URI fallbacks, item mapping, retry)
- Last-station persistence + optional autoplay on Android Auto connect
- CI fix: required `build-lint-test` now runs on `push` to `main`
- Signed release workflow is more robust (explicit secret errors + apksigner fallback lookup)
