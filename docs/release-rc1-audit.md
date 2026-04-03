# RadioWave 1.0.0-rc1 Audit

Status: 2026-04-03

This audit summarizes the current public release readiness for `1.0.0-rc1`.

## Summary

RadioWave is close to release-candidate quality.

Current strengths:
- Public repository cleaned and release-focused
- Stable player baseline with working notification/lockscreen controls
- Audio focus handling improved for calls/navigation interruptions
- Android Auto significantly hardened and polished
- Search scrolling performance improved
- Settings UX, supporter section, and update flows are much more mature
- GitHub APK / Play Store split is already implemented cleanly
- Google Play listing and dedicated project website are nearly ready

Main remaining work before tagging `1.0.0-rc1`:
- Finalize public documentation for the RC line
- Update version metadata (`versionName`, `versionCode`)
- Add a dedicated RC1 changelog entry
- Run one focused smoke-test pass on the most important user flows

## Release Readiness Assessment

### Ready / Strong
- Core playback and background behavior
- Favorites, browse/search, fullscreen player, settings
- In-app updater split by distribution flavor
- Android Auto favorites/search/queue/navigation baseline
- Local crash-report export flow
- Public repo hygiene and release workflow structure

### Needs final RC pass
- README / README.de content still referenced old beta/hotfix status before this audit pass
- CHANGELOG needs a clear RC1 section
- App version metadata still needs to be moved from beta/hotfix to RC1
- Android Auto should get one more real-world verification pass after the latest quality toggle and metadata priority changes
- Play Store text/screenshots should be checked against the current UI

## RC1 Checklist

### Must do before tag
- [ ] Set `app.versionName` to `1.0.0-rc1`
- [ ] Increase `app.versionCode`
- [ ] Add `1.0.0-rc1` section to `CHANGELOG.md`
- [ ] Verify README / README.de links and current product wording
- [ ] Confirm project website link: `https://radiowave.sven-neurath.de`
- [ ] Confirm Play Store link: `https://play.google.com/store/apps/details?id=de.darksoon.radiowave`
- [ ] Run a focused smoke test on device
- [ ] Verify GitHub flavor updater once more
- [ ] Verify Play flavor manifest/update gating once more

### Strongly recommended before final 1.0.0
- [ ] Test Android Auto in a real in-car/head-unit session again
- [ ] Review screenshots for Settings / Search / Favorites
- [ ] Review Play Store listing text one final time
- [ ] Decide whether any known issues should be documented explicitly for RC1

## Suggested Smoke Test Matrix

### Core app
- [ ] App launch
- [ ] Browse/search input
- [ ] Browse scrolling
- [ ] Favorites grid
- [ ] Fullscreen player controls
- [ ] Notification controls
- [ ] Lockscreen controls
- [ ] Background playback over several minutes

### Settings
- [ ] Language switching
- [ ] Default quality switching
- [ ] Android Auto quality-limit toggle
- [ ] Update settings page
- [ ] Support / Ko-fi / inline supporter links

### Android Auto
- [ ] Browse favorites / quick access
- [ ] Search result quality
- [ ] Previous / next navigation
- [ ] Text rendering
- [ ] Metadata visibility (`kbps`, codec where supported)
- [ ] Auto quality toggle behavior

### Distribution
- [ ] GitHub debug/release updater path
- [ ] Play flavor without sideload permission/updater
- [ ] Release workflow still matches intended distribution split

## Public Messaging Recommendation

For public docs, avoid pinning the README to a specific beta/hotfix line unless the version itself is the point.

Recommended style:
- Keep README generally product-focused
- Link to latest GitHub release
- Link to project website
- Link to Play Store
- Keep version-specific details in:
  - release tags
  - changelog
  - release notes
  - workflow metadata

## Recommendation

Proceed with:
1. public documentation refresh
2. RC1 changelog preparation
3. version bump to `1.0.0-rc1`
4. one final smoke-test pass
5. RC1 tag/release
