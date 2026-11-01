# Rekonstruktion aus der Handy-APK (Revision 95fcb33)

Der VPS mit dem Arbeitsstand `95fcb33c95bf9ecce801566f3bb37d0219474c6b` ging verloren (Server abgelaufen, 2026-10-26).
Einziger Zeuge ist die auf dem Handy installierte APK (`versionName 1.0.19`, Code 37, gebaut ca. 23.10.2026).
Dieser Branch baut den Stand aus ihr nach. Grundlage ist `main` = v1.0.19 (`944fec5`).

## Grafiken (1:1 aus der APK, unverändert)
Ablage: `core/core-ui/src/main/res/drawable-nodpi/` (Modul geraten: in der APK sind alle Module zusammengeführt).

- `genre_{chillout,jazz,rock,techno}_v1.webp` (768x512) – Genre-Kacheln der anpassbaren Startseite
- `radiowave_header_art_v2.png` (2098x749) – neues Header-Bild (violette Wellen)
- `radiowave_wordmark_v2.png`, `radiowave_wordmark_light_v2.png` (1983x793) – neue Wortmarke dunkel/hell

## Quellcode (rekonstruiert 2026-11-01)
Aus dem R8-Dekompilat der Handy-APK nachgebaut, mit der Release-APK von `944fec5` als Brücke
zwischen verschleiertem Code und bekanntem Quellcode. 21 neue, 45 geänderte Dateien.
Alle 590 englischen und 550 deutschen Strings sowie alle Grafiken sind deckungsgleich mit der APK.
Stellen mit `REA-UNSICHER` haben geratene Namen, ihr Verhalten ist aus der APK belegt.

Enthalten: Auto-Modus, Audio-Spektrum, anpassbare Startseite (Genres, Weiterhören),
gemerkte Titel, Einstellungen in Kategorien mit Suche, Audioausgabe/Bluetooth,
WLAN-only, Auto-Qualität WLAN/Mobil, Cover-Suche abschaltbar, Hell-/System-Theme,
Stream-Failover, bereinigte Sendernamen.

## Korrekturen gegenüber der Handy-APK (Markierung `FIX 2026-11-01`)
Ein Review fand Fehler, die schon in der Handy-APK steckten:
- Lost-Recovery startete nach jedem Failover die alte URL neu und würgte den Ausweich-Stream ab.
- Nach einem Dauerfehler blieb die Wiederverbindung gesperrt, wenn über Lenkrad, Android Auto,
  Benachrichtigung oder BT-Taste neu gestartet wurde.
- Ohne Ausweich-URL nur 2 statt bis zu 10 Wiederverbindungen mit Backoff (wie v1.0.19).
- Container-/Decoderfehler gelten nur als dauerhaft, solange die URL noch nie lief.
- Play-Taste im Fehlerzustand brauchte zwei Tipper.
- Navigationsansagen pausieren nicht mehr (der Live-Stream lief danach hinterher),
  die Einstellung schaltet jetzt stumm statt leiser.

Verifiziert: `testDebugUnitTest` und `:app:assembleGithubDebug` grün. Kein Gerätetest.
Vollständige Liste: Vault `30_Projects/RadioWave - Stand auf dem Handy (APK-Analyse 2026-10-26).md`.
