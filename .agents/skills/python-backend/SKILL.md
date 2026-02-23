---
name: python-backend
description: Python Backend Patterns mit FastAPI, SQLAlchemy, Pydantic
version: 1.0.0
triggers: ["*.py", "pyproject.toml", "requirements.txt"]
---

# Python Backend Skill

## Stack
- Python 3.12+ mit Type Hints überall
- FastAPI als Web-Framework
- SQLAlchemy 2.0+ (async) für ORM
- Pydantic v2 für Validierung
- Alembic für Migrationen
- uvicorn als ASGI Server
- pytest + httpx für Tests

## Code-Regeln

### Schichtenarchitektur (strikt)
```
Router → Service → Repository → Model
```
- Router: Nur HTTP-Logik (Request rein, Response raus)
- Service: Business-Logik, Validierung, Orchestrierung
- Repository: Datenbank-Queries (und NUR das)
- Model: SQLAlchemy Tabellen-Definition
- Schema: Pydantic Request/Response Models

### Async überall
- `async def` für alle Endpunkte und DB-Zugriffe
- `AsyncSession` statt `Session`
- `httpx.AsyncClient` für ausgehende Requests

### Fehlerbehandlung
- Custom `AppException` Klasse mit HTTP-Status + Error-Code
- Globaler Exception Handler
- NIEMALS Stack-Traces an Clients leaken
- Strukturiertes Logging mit `structlog`

### Validierung
- Pydantic Model für JEDEN Request Body
- `Field()` mit Constraints: min_length, max_length, pattern, ge, le
- Eigene Validatoren für komplexe Regeln

### Sicherheit
- Secrets nur aus Environment-Variablen
- `python-dotenv` für lokale Entwicklung
- bcrypt/argon2 für Passwort-Hashing
- JWT mit kurzer Lifetime + Refresh
- CORS strikt konfigurieren
- Rate Limiting auf sensitive Endpunkte
- Parameterized Queries ausschließlich

## Verbotene Patterns
- ❌ `os.system()` oder `subprocess.run(shell=True)`
- ❌ `eval()` oder `exec()`
- ❌ f-Strings in SQL Queries
- ❌ `from module import *`
- ❌ Bare `except:` ohne spezifische Exception
- ❌ Mutable Default Arguments: `def f(items=[])`
- ❌ Synchrone DB-Zugriffe in async Funktionen
