# Android Auto Developer Mode for Beta Builds

[Deutsch](ANDROID_AUTO_DEV_MODE.de.md) | [English](ANDROID_AUTO_DEV_MODE.md)

This guide applies to sideloaded alpha/beta APKs and GitHub release builds, not Play Store installs.

## Why this is required

Android Auto blocks non-verified media apps by default.  
For test builds, you must enable developer mode and allow unknown sources.

## Steps

1. Open Android Auto on your phone.
2. Open Android Auto settings.
3. Tap the version entry multiple times until developer mode is enabled.
4. Open Developer settings from the three-dot menu.
5. Enable **Unknown sources**.
6. Unplug the cable, close Android Auto, then reconnect.

## Notes

- For GitHub sideload builds there is currently no stable workaround without these steps.
- For Play Store distribution this is usually not required.
- On some devices Android Auto has no regular launcher entry. In RadioWave, the in-app shortcut falls back to app details.
