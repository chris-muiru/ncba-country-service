# Running and Testing

## Run

```bash
docker-compose up --build
```

App starts on `http://localhost:8080`. MySQL is included.

To stop:
```bash
docker-compose down
```

---

## Test

**Automated:**
```bash
python3 test_api.py
```

**Postman:** import `openapi/requests.json`

**Health check:**
```bash
curl http://localhost:8080/actuator/health
```

---

## Key Endpoints

```bash
# Fetch from SOAP and save
curl -X POST http://localhost:8080/api/country \
  -H "Content-Type: application/json" \
  -d '{"name": "Kenya"}'

# Get all
curl http://localhost:8080/api/country

# Get by ID
curl http://localhost:8080/api/country/1

# Update
curl -X PUT http://localhost:8080/api/country/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Kenya","capitalCity":"Nairobi","phoneCode":"254","continentCode":"AF","currencyIsoCode":"KES","countryFlag":"","isoCode":"KE","languages":[]}'

# Delete
curl -X DELETE http://localhost:8080/api/country/1
```

---

## Notes

- Country names are converted to sentence case before the SOAP call (`KENYA` → `Kenya`)
- SOAP client retries up to 3 times on failure
- Internet access required — SOAP calls hit `webservices.oorsprong.org`
