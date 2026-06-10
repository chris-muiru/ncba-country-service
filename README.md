# NCBA Country Info Service
Integration Microservices Engineer — Case Study

Spring Boot 3.4.2 · MySQL · SOAP · Docker · Kubernetes

---

## What It Does

Exposes a REST API that:
1. Accepts a country name via `POST /api/country`
2. Converts it to sentence case
3. Calls a live SOAP service to fetch the ISO code, then full country info
4. Persists `CountryInfo` + `Language` records to MySQL
5. Provides full CRUD on the saved data

---

## Run

**Prerequisites:** Docker + Docker Compose

```bash
docker-compose up --build
```

App starts on `http://localhost:8080`. MySQL is included — no separate setup needed.

---

## Test

**Option 1 — automated script (no dependencies):**
```bash
python3 test_api.py
```

**Option 2 — Postman:**
Import `openapi/requests.json` into Postman. Set `{{baseUrl}}` to `http://localhost:8080`.

---

## Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| POST | `/api/country` | Fetch from SOAP + save — `{"name": "Kenya"}` |
| GET | `/api/country` | Get all countries |
| GET | `/api/country/{id}` | Get by ID |
| PUT | `/api/country/{id}` | Update |
| DELETE | `/api/country/{id}` | Delete |
| GET | `/actuator/health` | Health check |

---

## Docs

| File | Purpose |
|------|---------|
| `docs/deployment-guide.md` | Deploy to Kubernetes |
| `docs/troubleshooting-guide.md` | K8s debugging guide |
| `docs/running-and-testing.md` | Detailed run + test steps |
| `openapi/requests.json` | Postman collection |
