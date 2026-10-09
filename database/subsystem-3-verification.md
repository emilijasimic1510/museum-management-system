# Subsystem 3 verification

## Historical review — 2026-09-22

The final `mvnw.cmd clean test` recorded **BUILD SUCCESS**, with 69 tests, 0 failures, 0 errors, and 0 skipped, in 4 minutes 19 seconds. PostgreSQL Testcontainers, Neo4j, and OpenAPI tests were included. The local execution artifact was `swagger-final-tests.log`. These are historical results, not a rerun for a later revision.

Springdoc 2.8.17 exposed 87 REST operations on 49 paths at that date. In the local profile, `/swagger-ui/index.html`, `/v3/api-docs`, and `/v3/api-docs.yaml` returned 200. Documentation was disabled by default outside local; business-route authentication and role restrictions remained in place.

Swagger **Try it out → Execute** checks recorded:

| Operation | Result |
|---|---|
| GET /api/health | 200, UP |
| GET /api/events | 200, empty list |
| GET /api/tickets/me without a token | 401 |

OpenAPI tests compared registered API operations against the specification and checked public documentation alongside protected business routes. Email was the login identifier; Swagger Authorize accepted the login response's `accessToken` without a manually added Bearer prefix.

## Schema and behavior

V3 introduced seven tables: `ulaznica`, `transakcija`, `poseta`, `skeniranje_eksponata`, `ocena_izlozbe`, `preporucena_ruta`, and `stavka_rute`. V1/V2 were unchanged.

The journey package included seven entities, a composite key, five enum types, seven repositories, DTOs/mapping, sales/visit/recommendation controllers, and services for sales, validation, scanning, ratings, routes, recommendations, lifecycle maintenance, and graph projection.

Isolated PostgreSQL applied V1–V3 and Hibernate validated 31 entities. All 14 JourneyIntegrationTests passed, covering price snapshots, ownership, declined payment/retry, simulated refunds, concurrent last-place purchase, single-use ticket validation, anonymous counter sales, visits, scans, routes, ratings, expiry, and cardinalities.

Neo4j verification found implicit grouping in a Cypher score expression. Shared-author counting was moved to a WITH aggregation before scoring. Synchronization/read timeouts were 30/10 seconds, with a 256/512 MB test heap.

Development PostgreSQL/Neo4j were healthy, Flyway recorded three successful migrations, and the local backend on 8081 passed Hibernate validation and returned health UP. The review added no separate username field, altered no approved EER/specification, performed no real charges, published no JWT/QR tokens, and exported no personal account/payment data to Neo4j. Historical records were preserved.

## Repeating verification

Follow the [root setup guide](../README.md) for Java 21, Docker, and a process-level JWT_SECRET. From `backend`:

```cmd
mvnw.cmd clean test
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

Check the health endpoint, Flyway history, and database container health separately. Do not interpret an old log or liveness response as proof that every current workflow passes.

No commit or push was made during the recorded review.
