---
name: api-security
description: Security-Checklist für API-Entwicklung und Drittanbieter-Integrationen
version: 1.0.0
triggers: ["*.py", "*.js", "*.env*", "docker-compose*"]
---

# API Security Skill

## Authentifizierung & Autorisierung

### JWT-Implementierung
- Access Token: max. 15 Minuten Lifetime
- Refresh Token: max. 7 Tage, rotierend, in HttpOnly Cookie
- Algorithm: RS256 bevorzugt, HS256 mit starkem Secret (min. 256 Bit)
- Payload: Minimale Claims (sub, exp, iat, iss) — keine sensitiven Daten
- Token Blacklist für Logout/Revocation

### API-Keys (Service-zu-Service)
- Hashen bevor sie gespeichert werden (wie Passwörter)
- Prefix für Identifikation: `sk_live_...`, `sk_test_...`
- Rotation ohne Downtime ermöglichen (zwei aktive Keys gleichzeitig)
- Rate Limiting pro API-Key

### OAuth2 (Drittanbieter-Login)
- State-Parameter gegen CSRF
- PKCE für Public Clients (Mobile, SPA)
- Nur notwendige Scopes anfordern
- Token sicher speichern (NICHT in localStorage)

## Input-Validierung

### Alle Eingänge validieren
- Request Body: Pydantic/JSON Schema
- Path Parameter: Typ + Format
- Query Parameter: Typ + Bereich + Whitelist
- Headers: Erwartete Werte prüfen
- File Uploads: Typ (Magic Bytes, nicht nur Extension), Größe, Virus-Scan

### Validation Rules
- Strings: min/max Länge, erlaubte Zeichen (Whitelist > Blacklist)
- Zahlen: min/max Wert, Integer vs Float
- Arrays: max Länge, Element-Validierung
- Nested Objects: Maximale Tiefe
- URLs: Schema-Whitelist (https only), SSRF-Prävention

## Rate Limiting

### Strategie
```
Endpunkt-Typ          | Limit
-----------------------|------------------------
Login/Auth             | 5 Versuche / Minute / IP
Passwort-Reset         | 3 / Stunde / Account
API (authentifiziert)  | 100 / Minute / User
API (unauthentifiziert)| 20 / Minute / IP
WebSocket Messages     | 60 / Sekunde / Connection
File Upload            | 10 / Stunde / User
```

### Implementierung
- HTTP 429 Too Many Requests mit `Retry-After` Header
- Redis-basiert für verteilte Systeme
- Sliding Window Algorithm bevorzugt

## Externe API-Integration

### Credentials
- API-Keys in Environment-Variablen (NIEMALS im Code)
- Separate Keys für Development/Staging/Production
- Rotation alle 90 Tage (oder nach Breach sofort)

### Request-Sicherheit
- TLS 1.2+ erzwingen
- Timeouts setzen (Connect: 5s, Read: 30s)
- Circuit Breaker für fehlerhafte Drittanbieter
- Retry mit Exponential Backoff (max 3 Versuche)
- Response validieren (Schema-Check, erwartete Typen)

### Error Handling
- Drittanbieter-Fehler nicht an eigene Clients durchreichen
- Eigene Error-Messages, eigene Status-Codes
- Fallback-Verhalten definieren (Cache, Degraded Mode)

## CORS (Cross-Origin Resource Sharing)

```python
# STRIKT konfigurieren — NICHT:
# allow_origins=["*"]  ← NIEMALS in Production

# SONDERN:
app.add_middleware(
    CORSMiddleware,
    allow_origins=["https://yourdomain.com"],
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE"],
    allow_headers=["Authorization", "Content-Type"],
)
```

## Security Headers

```
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 0
Content-Security-Policy: default-src 'self'
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: camera=(), microphone=(), geolocation=()
```

## Secrets Management Checkliste

- [ ] `.env` in `.gitignore`
- [ ] `.env.example` mit Platzhaltern committet
- [ ] Keine Secrets in Docker Images (Build Args → Runtime Env)
- [ ] Keine Secrets in Logs
- [ ] Keine Secrets in Error Responses
- [ ] Keine Secrets in Frontend-Code
- [ ] git history geprüft auf versehentlich committete Secrets
- [ ] Separate Secrets pro Environment (dev/staging/prod)
