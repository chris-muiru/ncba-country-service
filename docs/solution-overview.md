# Solution Overview

## Flow

```
POST /api/country {"name": "kenya"}
        │
        ▼
Convert to sentence case → "Kenya"
        │
        ▼
SOAP: CountryISOCode(Kenya) → "KE"
        │
        ▼
SOAP: FullCountryInfo(KE) → name, capital, phone, currency, languages[]
        │
        ▼
Save CountryInfoDef + LanguageDef to MySQL
        │
        ▼
Return 201 with full country data
```

## Structure

```
country/
  controller/   CountryController       REST endpoints
  service/      CountryService          Interface
  service/imp/  CountryServiceImp       Business logic
soap/
  client/       CountryInfoSoapClient   SOAP calls + XML parsing + retry
  config/       SoapConfig              RestTemplate with timeouts
io/
  entity/       CountryInfoDef          JPA entity (@OneToMany LanguageDef)
  entity/       LanguageDef
  repo/         CountryInfoRepo
exception/      GlobalExceptionHandler  400 / 500 responses
```

## Design Decisions

| Concern | Approach |
|---|---|
| SOAP integration | Raw XML over `RestTemplate` — no Spring-WS overhead |
| Retries | 3 attempts, 1s delay in `executeWithRetry()` |
| Timeouts | 5s connect, 10s read via `SimpleClientHttpRequestFactory` |
| Scaling | Stateless app + Kubernetes HPA (min 2, max 5 pods at 70% CPU) |
| Monitoring | Actuator + Micrometer Prometheus on `/actuator/prometheus` |
| Config | `application.yml` locally, ConfigMap + Secret on Kubernetes |
