# Integration Microservices Engineer — Solution Overview
**Candidate:** Chris Muiru | **Date:** June 10, 2026

---

## How the Flow Works

### End-to-End Request Flow

```
Client
  │
  │  POST /api/country  {"name": "kenya"}
  ▼
CountryController
  │  1. Receives JSON { name }
  │  2. Validates: @NotBlank
  │
  ▼
CountryServiceImp.fetchAndSave()
  │  3. Converts "kenya" → "Kenya"  (sentence case)
  │
  │  4. Calls SOAP: CountryISOCode(sCountryName="Kenya")
  ▼
CountryInfoSoapClient.getCountryISOCode()
  │  POST raw SOAP XML to:
  │  http://webservices.oorsprong.org/websamples.countryinfo/CountryInfoService.wso
  │  Retry: up to 3 attempts with 1s delay
  │  Parses XML response → extracts <CountryISOCodeResult>  e.g. "KE"
  │
  ▼ returns "KE"
CountryServiceImp (continued)
  │  5. Calls SOAP: FullCountryInfo(sCountryISOCode="KE")
  ▼
CountryInfoSoapClient.getFullCountryInfo()
  │  POST raw SOAP XML
  │  Parses response → CountryInfoDTO
  │    name, capitalCity, phoneCode, continentCode,
  │    currencyIsoCode, countryFlag, languages[]
  │
  ▼ returns CountryInfoDTO
CountryServiceImp (continued)
  │  6. Maps DTO → CountryInfoDef entity + List<LanguageDef>
  │  7. Saves to MySQL via countryInfoRepo.save()
  │  8. Maps saved entity → CountryInfoDTO (with DB id + UUID)
  │
  ▼
CountryController
  │  9. Maps DTO → CountryInfoRes
  │  10. Returns HTTP 201 Created
  ▼
Client receives full country data + languages
```

### SOAP Request/Response Examples

**Step 4 — CountryISOCode request:**
```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <web:CountryISOCode xmlns:web="http://www.oorsprong.org/websamples.countryinfo">
      <web:sCountryName>Kenya</web:sCountryName>
    </web:CountryISOCode>
  </soap:Body>
</soap:Envelope>
```

**Step 4 — Response:**
```xml
<m:CountryISOCodeResponse>
  <m:CountryISOCodeResult>KE</m:CountryISOCodeResult>
</m:CountryISOCodeResponse>
```

**Step 5 — FullCountryInfo request:**
```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <web:FullCountryInfo xmlns:web="http://www.oorsprong.org/websamples.countryinfo">
      <web:sCountryISOCode>KE</web:sCountryISOCode>
    </web:FullCountryInfo>
  </soap:Body>
</soap:Envelope>
```

**Step 5 — Response (parsed fields):**
```
sName         → Kenya
sCapitalCity  → Nairobi
sPhoneCode    → 254
sContinentCode→ AF
sCurrencyISOCode → KES
sCountryFlag  → http://www.oorsprong.org/.../Flags/Kenya.jpg
Languages     → [ {sISOCode: "SW", sName: "Swahili"}, ... ]
```

---

## Requirements Checklist

### Task 1 — Spring Boot Application
| Requirement | Implementation | Status |
|---|---|---|
| Spring Boot project | Spring Boot 3.4.2, Maven, Java 24 | ✅ |
| Spring Web dependency | `spring-boot-starter-web` in pom.xml | ✅ |
| Spring Data JPA | `spring-boot-starter-data-jpa` in pom.xml | ✅ |
| MySQL Driver | `mysql-connector-j` in pom.xml | ✅ |

---

### Task 2 — SOAP Integration (CountryInfoService WSDL)
| Requirement | Implementation | Status |
|---|---|---|
| SOAP project consumed | `CountryInfoSoapClient` uses WSDL endpoint | ✅ |
| WSDL URL used | `http://webservices.oorsprong.org/websamples.countryinfo/CountryInfoService.wso` | ✅ |
| Raw SOAP XML over HTTP | `RestTemplate` POST with `Content-Type: text/xml` | ✅ |
| Namespace-agnostic parsing | `getElementsByTagNameNS("*", localName)` | ✅ |

---

### Task 3 — REST Controller
| Requirement | Implementation | Status |
|---|---|---|
| POST endpoint | `POST /api/country` in `CountryController` | ✅ |
| Accepts `{"name": "Tanzania"}` | `CountryReq` record with `@NotBlank String name` | ✅ |
| Convert to sentence case | `toSentenceCase()` in `CountryServiceImp` — uppercases first char, lowercases rest | ✅ |

---

### Task 4 — Fetch ISO Code via SOAP
| Requirement | Implementation | Status |
|---|---|---|
| Call `CountryISOCode` SOAP op | `soapClient.getCountryISOCode(sentenceCase)` | ✅ |
| `sCountryName` as request param | Built into the SOAP XML body | ✅ |
| Returns `countryISOCodeResult` | Parsed from XML response | ✅ |

---

### Task 5 — Fetch Full Country Info via ISO Code
| Requirement | Implementation | Status |
|---|---|---|
| Extract ISO code from step 4 | Returned as `String isoCode` | ✅ |
| Call `FullCountryInfo` SOAP op | `soapClient.getFullCountryInfo(isoCode)` | ✅ |
| `sCountryISOCode` as param | Built into SOAP XML body | ✅ |
| Returns full country info | `CountryInfoDTO` with all fields + languages | ✅ |

---

### Task 6 — Data Models
| Requirement | Implementation | Status |
|---|---|---|
| `CountryInfo` model | `CountryInfoDef` entity — id, countryUUID, name, capitalCity, phoneCode, continentCode, currencyIsoCode, countryFlag, isoCode | ✅ |
| `Language` model | `LanguageDef` entity — id, languageUUID, isoCode, name, FK to CountryInfo | ✅ |
| Relationship | `CountryInfoDef @OneToMany LanguageDef` (cascade ALL, orphanRemoval) | ✅ |
| Persisted to MySQL | JPA with `ddl-auto: update` auto-creates tables | ✅ |

---

### Task 7 — CRUD REST APIs
| Requirement | Endpoint | Status |
|---|---|---|
| Fetch all country info | `GET /api/country` → 200 + List | ✅ |
| Fetch by ID | `GET /api/country/{id}` → 200 + single record | ✅ |
| Update country info | `PUT /api/country/{id}` → 200 + updated record | ✅ |
| Delete country info | `DELETE /api/country/{id}` → 204 No Content | ✅ |

---

### Task 8 — Kubernetes Deployment Scripts
| File | Purpose | Status |
|---|---|---|
| `k8s/deployment.yaml` | App Deployment — 2 replicas, resource limits, liveness + readiness probes | ✅ |
| `k8s/service.yaml` | ClusterIP Service port 80 → 8080 | ✅ |
| `k8s/configmap.yaml` | Non-sensitive env vars (JPA config, app name) | ✅ |
| `k8s/secret.yaml` | DB credentials (base64 encoded) | ✅ |
| `k8s/hpa.yaml` | HorizontalPodAutoscaler — min 2, max 5 pods, CPU 70% threshold | ✅ |
| `k8s/mysql-deployment.yaml` | MySQL StatefulSet with PersistentVolumeClaim | ✅ |

---

### Task 9 — Kubernetes Deployment Guide
| Requirement | File | Status |
|---|---|---|
| Step-by-step deploy guide | `docs/deployment-guide.md` | ✅ |

---

### Task 10 — Kubernetes Troubleshooting Guide
| Requirement | File | Status |
|---|---|---|
| Troubleshoot guide | `docs/troubleshooting-guide.md` | ✅ |

---

## Important Notes — Checklist

| Requirement | How It's Met |
|---|---|
| **System design & integration patterns** | Controller → Service → SOAP Client separation. SOAP abstracted behind a client component. REST for inbound, SOAP for outbound. |
| **High load — stateless services** | No server-side session. All state in MySQL. Kubernetes HPA scales pods horizontally under load. |
| **Horizontal scaling** | `k8s/hpa.yaml` — auto-scales 2→5 replicas at 70% CPU |
| **Retries for external failures** | `executeWithRetry()` in `CountryInfoSoapClient` — 3 attempts, 1s delay between each |
| **Timeouts** | `SoapConfig` — 5s connect timeout, 10s read timeout on `RestTemplate` |
| **Graceful failure / fallback** | SOAP failures throw `CustomException` → `GlobalExceptionHandler` returns clean 400 JSON |
| **Structured logging** | `@Slf4j` on every component, `log.info("operation - field: {}", value)` pattern |
| **Metrics & monitoring** | `spring-boot-starter-actuator` + `micrometer-registry-prometheus` → `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus` |
| **MVC architecture** | Controller (web layer) → Service (business logic) → Repository (data layer) — strict separation |
| **Proper HTTP status codes** | 201 Created, 200 OK, 204 No Content, 400 Bad Request, 500 Internal Server Error |
| **User-friendly error responses** | `CustomExceptionDto { statusCode, message, error, path }` |
| **Production-ready containerization** | Multi-stage `Dockerfile` (build + runtime), `docker-compose.yml` with MySQL + health checks |
| **Config management** | `application.yml` + Kubernetes ConfigMap + Secret |
| **Health checks** | `/actuator/health` exposed — used by K8s liveness + readiness probes |
| **Scalability** | Stateless app + HPA + MySQL as shared state |
| **Running & testing steps** | `docs/running-and-testing.md` + `test_api.py` test script |

---

## Project Structure

```
ncba-jd/
├── src/main/java/tech/muiru/ncba/
│   ├── NcbaApplication.java               # @SpringBootApplication @EnableJpaAuditing
│   ├── country/
│   │   ├── controller/CountryController.java   # REST endpoints
│   │   ├── service/CountryService.java          # Interface
│   │   └── service/imp/CountryServiceImp.java   # Business logic
│   ├── soap/
│   │   ├── client/CountryInfoSoapClient.java    # SOAP calls + XML parsing + retry
│   │   └── config/SoapConfig.java               # RestTemplate with timeouts
│   ├── io/
│   │   ├── entity/CountryInfoDef.java           # JPA entity
│   │   ├── entity/LanguageDef.java              # JPA entity
│   │   ├── repo/CountryInfoRepo.java            # Spring Data repo
│   │   └── repo/LanguageRepo.java
│   ├── model/
│   │   ├── request/CountryReq.java              # POST request body
│   │   └── response/CountryInfoRes.java         # Response shape
│   ├── dto/
│   │   ├── CountryInfoDTO.java                  # Internal data transfer
│   │   └── LanguageDTO.java
│   ├── exception/
│   │   ├── CustomException.java
│   │   ├── CustomExceptionDto.java
│   │   └── GlobalExceptionHandler.java
│   └── util/entity/BaseEntity.java             # Audit fields
├── src/main/resources/application.yml
├── Dockerfile
├── docker-compose.yml
├── k8s/
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── configmap.yaml
│   ├── secret.yaml
│   ├── hpa.yaml
│   └── mysql-deployment.yaml
└── docs/
    ├── deployment-guide.md
    ├── troubleshooting-guide.md
    └── running-and-testing.md
```
