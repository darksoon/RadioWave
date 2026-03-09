# Android Auto Entwicklermodus fuer Beta-Builds

[Deutsch](ANDROID_AUTO_DEV_MODE.de.md) | [English](ANDROID_AUTO_DEV_MODE.md)

Diese Anleitung gilt fuer sideloaded Alpha/Beta-APKs und GitHub-Release-Builds, nicht fuer Play-Store-Installationen.

## Warum das noetig ist

Android Auto blockiert standardmaessig nicht verifizierte Medien-Apps.  
Fuer Test-Builds muessen deshalb der Entwicklermodus aktiviert und **Unbekannte Quellen** erlaubt werden.

## Schritte

1. Android Auto auf dem Handy oeffnen.
2. Die Android-Auto-Einstellungen oeffnen.
3. Mehrfach auf den Versionseintrag tippen, bis der Entwicklermodus aktiviert ist.
4. Ueber das Drei-Punkte-Menue die Entwicklereinstellungen oeffnen.
5. **Unbekannte Quellen** aktivieren.
6. Kabel trennen, Android Auto schliessen und erneut verbinden.

## Hinweise

- Fuer GitHub-Sideload-Builds gibt es derzeit keinen stabilen Workaround ohne diese Schritte.
- Bei Play-Store-Distribution ist dieser Schritt in der Regel nicht noetig.
- Auf manchen Geraeten hat Android Auto keinen normalen Launcher-Einstieg. In RadioWave faellt der In-App-Shortcut dann auf die App-Info zurueck.
