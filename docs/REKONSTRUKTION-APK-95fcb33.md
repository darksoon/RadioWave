# Rekonstruktion aus der Handy-APK (Revision 95fcb33)

Der VPS mit dem Arbeitsstand `95fcb33c95bf9ecce801566f3bb37d0219474c6b` ging verloren (Server abgelaufen, 2026-10-26).
Einziger Zeuge ist die auf dem Handy installierte APK (`versionName 1.0.19`, Code 37, gebaut ca. 23.10.2026).
Dieser Branch baut den Stand aus ihr nach. Grundlage ist `main` = v1.0.19 (`944fec5`).

## Grafiken (1:1 aus der APK, unverändert)
Ablage: `core/core-ui/src/main/res/drawable-nodpi/` (Modul geraten: in der APK sind alle Module zusammengeführt).

- `genre_{chillout,jazz,rock,techno}_v1.webp` (768x512) – Genre-Kacheln der anpassbaren Startseite
- `radiowave_header_art_v2.png` (2098x749) – neues Header-Bild (violette Wellen)
- `radiowave_wordmark_v2.png`, `radiowave_wordmark_light_v2.png` (1983x793) – neue Wortmarke dunkel/hell

## Neue Funktionen laut APK-Strings (noch nicht nachgebaut)
Auto-Modus, Audio-Spektrum, Startseite anpassen (Genres, Weiterhören), gemerkte Titel,
Einstellungen in Kategorien mit Suche, Audioausgabe/Bluetooth, Pause bei Navigationsansagen,
WLAN-only, Auto-Qualität WLAN/Mobil, Cover-Suche abschaltbar, Hell-/System-Theme.
Vollständige Liste: Vault `30_Projects/RadioWave - Stand auf dem Handy (APK-Analyse 2026-10-26).md`.
