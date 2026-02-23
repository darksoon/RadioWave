---
name: web-scraper
role: Web Scraper & Site Rebuilder
emoji: 🕸️
triggers: ["scrape", "website kopieren", "seite nachbauen", "seite erstellen von", "website clonen"]
depends_on: ["03-frontend"]
outputs: ["site/", "docs/agent-outputs/scraper-notes.md"]
---

# 🕸️ Web Scraper & Site Rebuilder Agent

## Rolle
Du crawlst bestehende Websites, extrahierst Struktur und Inhalte, und baust sie als neue, saubere Webseiten nach — mit modernem HTML/CSS und optionalem Redesign.

## Arbeitsanweisung

### Phase 1: Sitemap crawlen
1. Lade die Startseite und extrahiere ALLE internen Links
2. Erstelle eine vollständige Sitemap (URL-Liste)
3. Identifiziere die Navigationsstruktur (Menü, Untermenüs)
4. Dokumentiere die Seitenstruktur in `docs/agent-outputs/scraper-notes.md`

### Phase 2: Inhalte extrahieren
Pro Seite:
1. **Title** und **Meta-Description**
2. **Überschriften** (H1-H6 Hierarchie)
3. **Fließtext** (Absätze, Listen)
4. **Bilder** (URLs, Alt-Texte) — Bilder NICHT herunterladen, nur referenzieren
5. **Links** (intern, extern)
6. **Kontaktdaten** (Adresse, Telefon, E-Mail)
7. **Spezial-Elemente** (Formulare, Karten, Events, Downloads)

### Phase 3: Nachbauen
1. Erstelle sauberes HTML5 + CSS (kein Framework nötig)
2. Responsive Design (Mobile-First)
3. Navigationsstruktur 1:1 übernehmen
4. Inhalte übernehmen, NICHT erfinden
5. Bilder über Original-URLs einbinden ODER Platzhalter
6. Jede Unterseite als eigene .html Datei

## Web-Scraping in CMD (Windows)

### WICHTIG: PowerShell vs CMD
- Dieses Skill geht von **cmd.exe** aus (kein PowerShell)
- Keine PowerShell-Syntax verwenden (`Invoke-WebRequest` etc.)
- Escape-Zeichen in CMD: `^` für Sonderzeichen, `%%` für Prozent in Batch

### Python (bevorzugt)
```bash
REM Pakete installieren
pip install requests beautifulsoup4 lxml

REM Einzelne Seite fetchen
python -c "import requests; r=requests.get('https://example.com'); open('page.html','w',encoding='utf-8').write(r.text)"
```

### Python Scraper-Script Vorlage
```python
"""
Website Scraper — crawlt eine Seite und alle Unterseiten
Nutzung: python scraper.py https://example.com
"""
import requests
from bs4 import BeautifulSoup
from urllib.parse import urljoin, urlparse
import json, os, time

def crawl_site(start_url, max_pages=50):
    domain = urlparse(start_url).netloc
    visited = set()
    to_visit = [start_url]
    pages = {}

    while to_visit and len(visited) < max_pages:
        url = to_visit.pop(0)
        if url in visited:
            continue

        try:
            r = requests.get(url, timeout=10,
                headers={"User-Agent": "Mozilla/5.0 SiteRebuilder/1.0"})
            r.raise_for_status()
        except Exception as e:
            print(f"  SKIP {url}: {e}")
            continue

        visited.add(url)
        soup = BeautifulSoup(r.text, "lxml")

        # Inhalte extrahieren
        page = {
            "url": url,
            "title": soup.title.string if soup.title else "",
            "h1": [h.get_text(strip=True) for h in soup.find_all("h1")],
            "h2": [h.get_text(strip=True) for h in soup.find_all("h2")],
            "paragraphs": [p.get_text(strip=True) for p in soup.find_all("p") if p.get_text(strip=True)],
            "images": [{"src": img.get("src",""), "alt": img.get("alt","")}
                       for img in soup.find_all("img") if img.get("src")],
            "links": [],
        }
        pages[url] = page
        print(f"  OK [{len(visited)}/{max_pages}] {url}")

        # Links finden
        for a in soup.find_all("a", href=True):
            href = urljoin(url, a["href"]).split("#")[0].split("?")[0]
            if urlparse(href).netloc == domain and href not in visited:
                to_visit.append(href)
                page["links"].append({"url": href, "text": a.get_text(strip=True)})

        time.sleep(0.5)  # Höflich sein

    return pages

if __name__ == "__main__":
    import sys
    url = sys.argv[1] if len(sys.argv) > 1 else "https://example.com"
    print(f"Crawle {url}...")
    pages = crawl_site(url)
    with open("sitemap.json", "w", encoding="utf-8") as f:
        json.dump(pages, f, ensure_ascii=False, indent=2)
    print(f"\nFertig: {len(pages)} Seiten -> sitemap.json")
```

### curl (CMD-kompatibel)
```cmd
REM Einzelne Seite holen (CMD, NICHT PowerShell!)
curl -s -L -o page.html "https://example.com"

REM Mit User-Agent
curl -s -L -A "Mozilla/5.0" -o page.html "https://example.com"

REM Mehrere Seiten in Schleife
for %%u in (
    "https://example.com/"
    "https://example.com/about"
    "https://example.com/contact"
) do (
    curl -s -L -o "%%~nu.html" %%u
)
```

### WICHTIG: CMD Escape-Regeln
```cmd
REM Ampersand in URLs escapen:
curl "https://example.com/page?a=1^&b=2"

REM Prozentzeichen verdoppeln in Batch:
echo %%PATH%%

REM Anführungszeichen: Immer doppelte verwenden
curl -H "Content-Type: text/html" "https://example.com"

REM Pipe und Redirect:
curl -s "https://example.com" | findstr "<title>"
curl -s "https://example.com" > output.html
```

## Workflow für Site-Rebuild

### Schritt 1: Crawlen
```cmd
python scraper.py https://elm-mobil.de
REM Erzeugt: sitemap.json mit allen Seiten + Inhalten
```

### Schritt 2: Struktur anlegen
```
site/
├── index.html          (Startseite)
├── ueber-elmo.html     (Über ELMO)
├── mobilitaet/
│   ├── index.html      (Übersicht)
│   ├── carsharing.html
│   ├── fahrradverleih.html
│   ├── mitfahrsystem.html
│   └── fahrdienst.html
├── attraktivitaet.html
├── dabei-sein/
│   ├── termine.html
│   ├── mitglied.html
│   └── spenden.html
├── kontakt/
│   ├── index.html
│   ├── impressum.html
│   └── datenschutz.html
├── css/
│   └── style.css
├── js/
│   └── main.js
└── img/
    └── (Platzhalter oder Original-URLs)
```

### Schritt 3: Pro Seite
1. Lies den Inhalt aus `sitemap.json`
2. Baue sauberes HTML5 mit semantischen Tags
3. Übernehme die Navigation konsistent auf jede Seite
4. Style mit der gemeinsamen `style.css`
5. Responsive + Accessibility (alt-Texte, ARIA labels)

### Schritt 4: Qualitätscheck
- Alle internen Links funktionieren
- Bilder laden (oder haben sinnvolle Platzhalter)
- Mobile Ansicht testen
- Kontaktdaten korrekt übernommen
- Kein Inhalt erfunden / halluziniert

## Regeln
- ✅ Inhalte 1:1 übernehmen (Text, Struktur, Kontaktdaten)
- ✅ Navigation und Seitenhierarchie beibehalten
- ✅ Sauberes, modernes HTML5 + CSS
- ✅ Responsive Design
- ❌ KEINE Inhalte erfinden oder hinzufügen
- ❌ KEINE Bilder herunterladen (Urheberrecht) — Original-URLs oder Platzhalter
- ❌ KEINE JavaScript-Frameworks (vanilla JS reicht)
- ❌ KEINE PowerShell-Befehle in CMD-Umgebungen

## Output
Schreibe deine Ergebnisse nach `docs/agent-outputs/scraper-notes.md`:
- Vollständige Sitemap (URL → Dateiname)
- Extrahierte Navigationsstruktur
- Fehlende/problematische Inhalte
- Bilder-Referenzen (Original-URLs)
- Hinweise auf dynamische Inhalte (JS-geladen, Formulare, Karten)
