# Running and Testing the NCBA Country Service

## Prerequisites

- Java 21
- Maven 3.9+ (or use `./mvnw`)
- MySQL 8.0 running locally on port 3306 with database `ncba_db`
- Docker (optional, for containerized run)

---

## Running Locally

### Option 1: Maven

```bash
# From project root
./mvnw spring-boot:run
```

Or build and run the JAR:

```bash
./mvnw package -DskipTests
java -jar target/ncba-0.0.1-SNAPSHOT.jar
```

### Option 2: Docker Compose

Starts both the app and MySQL:

```bash
docker-compose up --build
```

To stop:

```bash
docker-compose down
```

---

## Health Check

```bash
curl http://localhost:8080/actuator/health
```

Expected response:

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

---

## API Endpoints

### POST /api/country — Fetch from SOAP and save

Fetches country info from the external SOAP service and persists it.

```bash
curl -X POST http://localhost:8080/api/country \
  -H "Content-Type: application/json" \
  -d '{"name": "Kenya"}'
```

Expected response (HTTP 201):

```json
{
  "countryUUID": "...",
  "name": "Kenya",
  "capitalCity": "Nairobi",
  "phoneCode": "254",
  "continentCode": "AF",
  "currencyIsoCode": "KES",
  "countryFlag": "http://...",
  "isoCode": "KE",
  "languages": [
    { "isoCode": "SW", "name": "Swahili" },
    { "isoCode": "EN", "name": "English" }
  ]
}
```

### GET /api/country — List all saved countries

```bash
curl http://localhost:8080/api/country
```

Expected response (HTTP 200): JSON array of country objects.

### GET /api/country/{id} — Get country by ID

```bash
curl http://localhost:8080/api/country/1
```

Expected response (HTTP 200): single country object.

### PUT /api/country/{id} — Update a country

```bash
curl -X PUT http://localhost:8080/api/country/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Kenya",
    "capitalCity": "Nairobi",
    "phoneCode": "254",
    "continentCode": "AF",
    "currencyIsoCode": "KES",
    "countryFlag": "http://example.com/flag.png",
    "isoCode": "KE",
    "languages": [
      { "isoCode": "SW", "name": "Swahili" }
    ]
  }'
```

Expected response (HTTP 200): updated country object.

### DELETE /api/country/{id} — Delete a country

```bash
curl -X DELETE http://localhost:8080/api/country/1
```

Expected response: HTTP 204 No Content.

---

## Validation Error Example

Sending a blank name returns HTTP 400:

```bash
curl -X POST http://localhost:8080/api/country \
  -H "Content-Type: application/json" \
  -d '{"name": ""}'
```

Response:

```json
{
  "statusCode": 400,
  "message": "Country name must not be blank",
  "error": "Bad Request",
  "path": "/api/country"
}
```

---

## Not Found Error Example

```bash
curl http://localhost:8080/api/country/9999
```

Response (HTTP 400):

```json
{
  "statusCode": 400,
  "message": "Country not found with id: 9999",
  "error": "Bad Request",
  "path": "/api/country/9999"
}
```

---

## Prometheus Metrics

```bash
curl http://localhost:8080/actuator/prometheus
```

---

## Notes

- Country names are automatically converted to sentence case before SOAP lookup (e.g., `KENYA` → `Kenya`).
- The SOAP client retries up to 3 times with a 1-second delay between attempts.
- The external SOAP endpoint is `http://webservices.oorsprong.org/websamples.countryinfo/CountryInfoService.wso`.
