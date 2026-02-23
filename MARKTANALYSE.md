# Radio App Projekt - Marktanalyse

## 📊 Marktanalyse: Online Radio Apps für Android

---

## 🎯 Wettbewerber Analyse

### 1. TuneIn Radio
| Aspekt | Details |
|--------|---------|
| **Stärken** | 50.000+ Sender, Podcasts |
| **Schwächen** | ❌ Android Auto Bugs (Black Screen, reconnects) |
| | ❌ Account-Pflicht (seit 2024) |
| | ❌ Favoriten sync unzuverlässig |
| | ❌ "Rotting" - User beklagen weniger Updates |
| | ❌ Werbung |
| **Preis** | Free + Pro (€9,99/Jahr) |

### 2. Replario / Replaio Radio
| Aspekt | Details |
|--------|---------|
| **Stärken** | ✅ 50.000+ Sender |
| | ✅ Android Auto Support |
| | ✅ Anpassbare Farbschemata |
| | ✅ Rewrite/Pause live radio |
| | ✅ Track History |
| **Schwächen** | ❌ Nicht mehr aktuell entwickelt? |
| | ❌ Langsames Zappen |
| **Preis** | €4,99 (Premium) |

### 3. Non Stop Radio ⭐ (NEU entdeckt!)
| Aspekt | Details |
|--------|---------|
| **Stärken** | ✅ Kostenlos, keine Ads, kein Account |
| | ✅ Android Auto funktioniert! |
| | ✅ 60.000+ Sender |
| | ✅ Podcasts integriert |
| | ✅ Clean UI |
| **Schwächen** | ❌ Hobby-Entwickler (keine professionelle Wartung) |
| | ❌ "Master Search" Feature - nur Favoriten |
| **Preis** | Komplett kostenlos |

### 4. VRadio ⭐
| Aspekt | Details |
|--------|---------|
| **Stärken** | ✅ Android Auto Support |
| | ✅ Custom Stations (eigene URLs) |
| | ✅ Sleep Timer, Widget, Recording |
| | ✅ Volume Control |
| **Schwächen** | ❌ Werbung |
| | ❌ App sieht alt aus |
| | ❌ "Pro" Version für Features |
| **Preis** | Free + Pro |

### 5. Radio Mobi
| Aspekt | Details |
|--------|---------|
| **Stärken** | ✅ Viele Sender |
| | ✅ Sleep Timer |
| **Schwächen** | ❌ **KEIN Android Auto!** ("leider kein...") |
| | ❌ Wenig Features |

### 6. Transistor (Open Source) ⭐
| Aspekt | Details |
|--------|---------|
| **Stärken** | ✅ Open Source (MIT License) |
| | ✅ Android Auto Support |
| | ✅ F-Droid verfügbar |
| | ✅ Material Design |
| **Schwächen** | ❌ Keine eingebaute Sender-DB |
| | ❌ Nur manuell Sender eintragen |
| | ❌ Sehr minimalistisch |
| **Preis** | Kostenlos |

### 7. RadioDroid (Open Source)
| Aspekt | Details |
|--------|---------|
| **Stärken** | ✅ Kostenlos, Open Source |
| | ✅ Nutzt Radio Browser API |
| | ✅ Keine Accounts, keine Werbung |
| **Schwächen** | ❌ **Kein Android Auto Support!** |
| | ❌ Veraltetes UI |
| | ❌ Wenig Features |

### 8. radio.net
| Aspekt | Details |
|--------|---------|
| **Stärken** | ✅ Android Auto Support |
| | ✅ Viele Sender |
| **Schwächen** | ❌ **Werbung (viele Beschwerden!)** |
| | ❌ "Commercials continually cut in" |
| | ❌ Notification Probleme |
| **Preis** | Free (mit Werbung) |

---

## 😤 User Beschwerden (Reddit, Play Store, Forums)

### TuneIn
- "TuneIn doesn't work with Android Auto - black screen"
- "Favorites missing in car but show on phone"
- "Requires account now - hate it"
- "Rotting - no updates"
- "Can't access on Android Auto for months"

### Replaio
- "Zapping between stations is slow"
- "Need to buy premium"
- "Some stations don't play"

### Radio Mobi
- "Im Auto mit Android-Auto leider kein Support!"

### radio.net
- "Problem is commercials continually cut in"
- "Playback notification is permanent, doesn't vanish"

### Generell (Reddit)
- "No good open source app with Android Auto"
- "Can't add custom stations easily"
- "UI is dated"
- "Wants to sync but can't"
- "Need account for everything"

---

## 💡 Was User WOLLEN (aus Reddit)

1. **Android Auto Support** - funktioniert zuverlässig
2. **Custom Stations** - eigene Stream URLs eintragen
3. **No Ads** - nervige Werbung
4. **No Account Required** - soll einfach funktionieren
5. **Open Source** - Datenschutz, Vertrauen
6. **Dark Mode** - fürs Auto
7. **Modern UI** - nicht wie 2010
8. **Favorites Sync** - zwischen Auto und Phone
9. **Schnelles Zappen** - keine 5 Sekunden Ladezeit

---

## 🎯 Marktlücke / Unsere Chance

| Feature | TuneIn | Replaio | Non Stop | Transistor | RadioDroid | VRadio | Unser Plan |
|---------|--------|---------|----------|------------|------------|--------|------------|
| Android Auto | ❌ buggy | ✅ | ✅ | ✅ | ❌ | ✅ | ✅ |
| Custom URLs | ❌ | ❌ | ❌ | ✅ manuell | ✅ | ✅ | ✅ |
| Open Source | ❌ | ❌ | ❌ | ✅ | ✅ | ❌ | ✅ |
| No Ads | ❌ | ❌ | ✅ | ✅ | ✅ | ❌ | ✅ |
| Moderne UI | ⚠️ | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ |
|Sender-DB|✅|✅|✅|❌|✅|✅|✅|

**GAP:** Niemand hat alles! 
- Transistor = Open Source + AA aber keine DB
- RadioDroid = Open Source + DB aber kein AA
- Non Stop = AA + free aber Hobby-Projekt

---

## 📋 Unsere Anforderungen

### Must-Have 🔴
- [ ] Android Auto Support (stabil!)
- [ ] Sender-DB (Radio Browser API)
- [ ] Eigene Sender eintragen (Stream URL)
- [ ] Dark Mode
- [ ] Modernes UI (Material Design 3)
- [ ] No Ads / Open Source

### Nice-to-Have 🟡
- [ ] Favoriten-Sync
- [ ] Widget
- [ ] Equalizer
- [ ] Sleep Timer
- [ ] Recording
- [ ] Podcasts (später)

---

## 🔧 Technologie

- **Flutter** - wie Familien-App
- **Radio Browser API** - Sender-DB (kostenlos, open source)
- **Android Auto** - Flutter for Android Auto
- **sqflite** - lokale DB für Favoriten/Custom
- **just_audio** - Audio-Playback
- **Audio Service** - Background playback

---

## 📦 Projekt Setup

Projekt-Verzeichnis: `~/Projekte/radio-app/`

Git Branch: `feature/market-analysis`
