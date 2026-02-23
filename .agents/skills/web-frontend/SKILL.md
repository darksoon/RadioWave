---
name: web-frontend
description: Standards für HTML5, CSS3, JavaScript/TypeScript Web-Frontends
version: 1.0.0
triggers: ["*.html", "*.css", "*.js", "*.ts"]
---

# Web Frontend Skill

## Standards

### HTML5
- Semantische Tags: header, main, nav, section, article, footer
- Accessibility: ARIA-Labels, alt-Texte, lang-Attribut, Keyboard-Navigation
- Meta-Tags: viewport, description, Open Graph
- Keine Inline-Styles, kein Inline-JS

### CSS
- Mobile-First Responsive Design
- CSS Custom Properties für Theming
- BEM-Namenskonvention: `block__element--modifier`
- Kein `!important` (Ausnahme: Utility-Klassen)
- Flexbox/Grid statt Floats
- `prefers-color-scheme` für Dark Mode
- `prefers-reduced-motion` respektieren

### JavaScript
- Strict Mode
- ES2022+ Syntax
- Keine globalen Variablen — ES Modules nutzen
- `fetch()` statt XMLHttpRequest
- Error Handling für jeden Netzwerk-Request
- Event Delegation statt massenhafter Listener
- `requestAnimationFrame` für Animationen

### Performance
- Bilder: WebP, Lazy Loading, srcset
- Code-Splitting für große Apps
- Critical CSS inline
- `<link rel="preconnect">` für externe Domains
- Keine unnötigen Dependencies

## Sicherheit
- NIEMALS `innerHTML` mit User-Input → `textContent` oder DOM-APIs
- Content Security Policy definieren
- Anti-CSRF Tokens
- Keine API-Keys im Frontend
- SRI (Subresource Integrity) für CDN-Ressourcen

## Canvas/WebGL (Game-Frontends)
- `requestAnimationFrame` für Render-Loop
- Delta-Time für framerate-unabhängige Logik
- Input-Manager Klasse zentral
- Asset-Preloader mit Fortschritt
- Spatial Partitioning für Kollisionen

## Verbotene Patterns
- ❌ `document.write()`
- ❌ `innerHTML` mit unvalidiertem Input
- ❌ Synchrone AJAX-Requests
- ❌ `var` → immer `const` oder `let`
- ❌ `==` → immer `===`
- ❌ jQuery (es sei denn explizit gewünscht)
