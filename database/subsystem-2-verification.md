# Subsystem 2 verification

Date: 2026-09-22. This report records the subsystem 2 implementation stage and is not a current application readiness claim.

The working tree was clean before this stage. The V2 migration, entities, repositories, DTOs, mapper, and services already existed. Work completed controllers, authentication error handling, tests, and documentation, and corrected event RBAC, JWT key sourcing, and event capacity/status refresh.

## Verified mapping

The [relational model](relational-model-subsystem-2.md) documents tables, columns, SQL types, keys, uniqueness, nullability, checks, cardinalities, deletion rules, routes, and permissions.

V2 adds 14 tables: `korisnicki_nalog`, `uloga`, `nalog_uloga`, `posetilac`, `zaposleni`, `kustos`, `vodic`, `blagajnik`, `menadzer`, `administrator`, `interesovanje`, `posetilac_interesovanje`, `dogadjaj`, and `prijava_na_dogadjaj`. It references existing V1 museums/exhibitions rather than recreating them.

The employee hierarchy uses JOINED inheritance, shared supertype/subtype IDs, a discriminator, and deferred PostgreSQL triggers enforcing totality and disjointness. Employee subtype does not automatically grant an RBAC role.

Email is the required unique login identifier. The schema and login DTO do not add `username`, `korisnicko_ime`, or `login_name`. Datasource/Neo4j username settings are database credentials, not user-model attributes.

## Recorded tests

Executed from `backend` with Java 21 and a random process-level JWT_SECRET:

```text
mvnw.cmd clean test
Tests run: 50, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

| Suite | Tests | Failures/errors |
|---|---:|---:|
| CatalogIntegrationTests | 10 | 0 |
| CatalogServiceTests | 10 | 0 |
| HealthControllerTests | 2 | 0 |
| MuseumBackendApplicationTests | 1 | 0 |
| PeopleIntegrationTests | 20 | 0 |
| PeopleServiceTests | 7 | 0 |

The previous 23 tests were preserved. Catalog table/entity counts were scoped to subsystem 1 rather than the entire expanded schema. Health tests received mocked JWT dependencies without changing their assertions.

Coverage included:

- V1/V2 migration on isolated PostgreSQL 17.9 and Hibernate validation of 24 entities.
- Exact account columns, required/case-insensitive unique email, registration, VISITOR profile creation, BCrypt validation, rejection of plaintext hashes, and DTO redaction.
- Wrong passwords, missing/inactive accounts, invalid/expired JWTs, incorrect issuer, and immediate effect of role removal or deactivation.
- All five employee subtypes, subtype fields, CRUD, required parent FKs, profile uniqueness, direct-SQL subtype constraints, hierarchy cycles, and restricted deletion.
- Role assignment time/assigner, duplicate assignments, and minimum-one-role constraints at service and database levels.
- Profile privacy, interest selection/update/removal, and composite keys.
- Registration, duplicates, cancellation/reactivation, attendance, full/cancelled events, capacity reductions, and guide FKs.
- Concurrent registration for the last place: exactly one 201 and one 409.
- Database capacity/duplicate protection, all six roles, and 400/401/403/404/409 responses.

Tests used Testcontainers without clearing development data. Expected FK violations were negative-test outcomes, not suite failures. Random test credentials were not published. MockMvc request printing was disabled for the new integration tests.

The first restricted wrapper invocation could not access the Maven cache. The same wrapper succeeded with approved cache and Docker access; the wrapper was not changed in this stage.

## Recorded development environment checks

- `docker compose config --quiet` and `docker compose up -d` succeeded without printing resolved secrets.
- PostgreSQL 17.9 and Neo4j 5.26.0 were healthy.
- The local backend started on 127.0.0.1:8081.
- Flyway validated two successful migrations; Hibernate initialized with `ddl-auto=validate`.
- GET `/api/health`, `/api/events`, and `/api/interests` returned 200.
- GET `/api/admin/accounts` without a token returned 401.

The development application used a temporary process JWT key. No volumes were deleted, and `docker compose down -v` was not used. No development administrator was created during this historical stage.

## Security and scope at that date

Registration atomically saved a BCrypt hash, registration time, VISITOR assignment, and profile. Login updated last-login time; role assignment recorded its administrator. Event registration/capacity changes maintained PLANIRAN/POPUNJEN with database capacity/minimum-cardinality protection.

Private operations required authentication even locally. At that earlier stage, local subsystem 1 catalog writes remained anonymous and the first administrator required explicit database provisioning. The current implementation supersedes both statements: management writes are protected and local seed accounts include an administrator. Journey, TTS, and graph jobs were implemented in later stages.

V2 was already tracked and was not modified in this continuation. V1 and approved specification/EER documents were unchanged. The recorded whitespace check passed. No commit or push was made and no existing source file was deleted.

## Historical file inventory

This is the original review's status snapshot, not the current working tree:

```text
 M README.md
 M backend/src/main/java/rs/ac/uns/ftn/museum/people/dto/TokenResponse.java
 M backend/src/main/java/rs/ac/uns/ftn/museum/people/security/ApiSecurity.java
 M backend/src/main/java/rs/ac/uns/ftn/museum/people/security/JwtConfiguration.java
 M backend/src/main/java/rs/ac/uns/ftn/museum/people/service/EventService.java
 M backend/src/test/java/rs/ac/uns/ftn/museum/catalog/CatalogIntegrationTests.java
 M backend/src/test/java/rs/ac/uns/ftn/museum/health/HealthControllerTests.java
 M database/relational-model-subsystem-2.md
?? backend/src/main/java/rs/ac/uns/ftn/museum/people/controller/AccountController.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/people/controller/AuthController.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/people/controller/AuthenticationExceptionHandler.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/people/controller/EmployeeController.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/people/controller/EventController.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/people/controller/InterestController.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/people/controller/RoleController.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/people/controller/VisitorController.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/people/controller/VisitorProfileController.java
?? backend/src/test/java/rs/ac/uns/ftn/museum/people/PeopleIntegrationTests.java
?? backend/src/test/java/rs/ac/uns/ftn/museum/people/PeopleServiceTests.java
?? database/subsystem-2-verification.md
```
