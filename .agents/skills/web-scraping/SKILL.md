---
name: web-scraping
description: Website crawlen, Inhalte extrahieren, Seiten nachbauen
version: 1.0.0
triggers: ["scrape", "crawl", "website kopieren", "seite nachbauen"]
---

# Web Scraping & Site Rebuild Skill

## Umgebung
- **Shell**: cmd.exe (Windows) — KEIN PowerShell
- **Python**: 3.10+ mit requests, beautifulsoup4, lxml
- **Fallback**: curl (in CMD)

## Setup (einmalig)
```cmd
pip install requests beautifulsoup4 lxml
```

## Schnell-Crawl einer ganzen Seite

### Python One-Liner (CMD-kompatibel)
```cmd
python -c "import requests; from bs4 import BeautifulSoup; r=requests.get('https://example.com'); soup=BeautifulSoup(r.text,'lxml'); [print(a['href']) for a in soup.find_all('a',href=True) if 'example.com' in a.get('href','')]"
```

### Vollständiger Crawler
Speichere als `scraper.py` und starte mit:
```cmd
python scraper.py https://example.com
```

Der Crawler:
1. Holt die Startseite
2. Findet alle internen Links
3. Besucht jede Unterseite (max 50)
4. Extrahiert Titel, Überschriften, Text, Bilder, Links
5. Speichert alles als `sitemap.json`

## Inhalte aus gecrawlter Seite extrahieren

### Wichtigste BeautifulSoup Selektoren
```python
soup = BeautifulSoup(html, "lxml")

# Navigation
nav = soup.find("nav")
nav_links = nav.find_all("a") if nav else []

# Hauptinhalt (verschiedene Muster probieren)
main = (soup.find("main")
     or soup.find("article")
     or soup.find("div", class_="content")
     or soup.find("div", id="content"))

# Bilder mit vollständigen URLs
from urllib.parse import urljoin
images = [
    {"src": urljoin(base_url, img["src"]), "alt": img.get("alt", "")}
    for img in soup.find_all("img") if img.get("src")
]

# Kontaktdaten suchen
import re
emails = re.findall(r'[\w.+-]+@[\w-]+\.[\w.-]+', html)
phones = re.findall(r'[\+]?[\d\s\-/]{7,}', soup.get_text())

# Footer
footer = soup.find("footer")
```

### WordPress-Seiten (wie elm-mobil.de)
```python
# WordPress hat typische Klassen:
content = soup.find("div", class_="entry-content")  # Seiteninhalt
sidebar = soup.find("aside", class_="sidebar")
menu = soup.find("div", class_="menu-hauptmenue-container")
# oder:
menu = soup.find("ul", id="menu-hauptmenue")
```

## Site nachbauen — HTML Template

### Basis-Template für nachgebaute Seiten
```html
<!DOCTYPE html>
<html lang="de">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{{TITLE}}</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <header>
        <nav>
            <a href="index.html" class="logo">
                <img src="{{LOGO_URL}}" alt="{{SITE_NAME}}">
            </a>
            <button class="menu-toggle" aria-label="Menü">☰</button>
            <ul class="nav-menu">
                {{NAV_ITEMS}}
            </ul>
        </nav>
    </header>

    <main>
        {{CONTENT}}
    </main>

    <footer>
        {{FOOTER}}
    </footer>

    <script src="js/main.js"></script>
</body>
</html>
```

### Navigation generieren
```python
def build_nav(menu_items):
    """Menu-Items: [{"text": "Home", "url": "/", "children": [...]}]"""
    html = ""
    for item in menu_items:
        if item.get("children"):
            html += f'<li class="has-dropdown">\n'
            html += f'  <a href="{item["url"]}">{item["text"]}</a>\n'
            html += f'  <ul class="dropdown">\n'
            for child in item["children"]:
                html += f'    <li><a href="{child["url"]}">{child["text"]}</a></li>\n'
            html += f'  </ul>\n</li>\n'
        else:
            html += f'<li><a href="{item["url"]}">{item["text"]}</a></li>\n'
    return html
```

## URL zu Dateiname konvertieren
```python
def url_to_filename(url, base_domain):
    """https://elm-mobil.de/ueber-elmo/ -> ueber-elmo.html"""
    from urllib.parse import urlparse
    path = urlparse(url).path.strip("/")
    if not path:
        return "index.html"
    # Unterordner erkennen
    parts = path.split("/")
    if len(parts) == 1:
        return f"{parts[0]}.html"
    else:
        return os.path.join(*parts[:-1], f"{parts[-1]}.html")
```

## CMD Fallback: curl
```cmd
REM Seite holen
curl -s -L -o "seite.html" "https://elm-mobil.de/"

REM Alle Links extrahieren (mit findstr)
curl -s -L "https://elm-mobil.de/" | findstr /i "href=" > links.txt

REM Mehrere Seiten holen
curl -s -L -o "index.html" "https://elm-mobil.de/"
curl -s -L -o "ueber-elmo.html" "https://elm-mobil.de/ueber-elmo/"
curl -s -L -o "carsharing.html" "https://elm-mobil.de/mobilitaetsangebote/nachbarschaftliches-carsharing/"
curl -s -L -o "fahrradverleih.html" "https://elm-mobil.de/mobilitaetsangebote/fahrradverleih/"
```

## Häufige Probleme

### JavaScript-geladene Inhalte
- Einfacher Scraper bekommt nur das initiale HTML
- Lösung: `requests_html` mit `render()` oder `playwright`
- Oft reicht das initiale HTML trotzdem (WordPress lädt Inhalte serverseitig)

### Encoding-Probleme
```python
r = requests.get(url)
r.encoding = r.apparent_encoding  # Auto-Detect
```

### Rate Limiting / Blockiert
```python
import time
time.sleep(1)  # 1 Sekunde zwischen Requests
headers = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"}
```

### Bilder
- Original-URLs beibehalten: `<img src="https://elm-mobil.de/wp-content/uploads/bild.jpg">`
- ODER Platzhalter: `<img src="img/placeholder.svg" alt="Beschreibung">`
- Bilder NICHT herunterladen ohne Erlaubnis

## Checkliste nach Rebuild
- [ ] Alle Seiten aus der Sitemap erstellt
- [ ] Navigation funktioniert auf jeder Seite
- [ ] Interne Links korrekt (relative Pfade)
- [ ] Bilder laden oder haben Platzhalter
- [ ] Kontaktdaten korrekt übernommen
- [ ] Responsive auf Mobile
- [ ] Kein Inhalt erfunden
- [ ] Footer mit Impressum/Datenschutz
