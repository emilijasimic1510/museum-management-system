# Museum and Exhibition Management System

Diploma thesis project developed at the Faculty of Technical Sciences,
University of Novi Sad.

Student: Emilija Simić  
Index: RA 3/2022

## Project overview

The system supports museum and exhibition management, digital catalogues,
ticket sales and validation, visitor activities, guided tours, reports and
personalized exhibition routes.

## Architecture

- Angular frontend
- Spring Boot backend
- PostgreSQL relational database
- Neo4j graph database
- Docker Compose development environment

PostgreSQL is the source of truth for business, transactional and personal
data. Neo4j is used only for relationships between cultural content and
recommendation generation.

## Planned user roles

- Visitor
- Curator
- Cashier
- Guide
- Manager
- Administrator

## Main functional areas

- Museum and hall management
- Exhibition and exhibit management
- Digital exhibition catalogue
- Ticket purchase and QR code generation
- Ticket validation and visit tracking
- Digital audio guide
- Events and guided tours
- Visitor interests and ratings
- Personalized recommendations and routes
- Attendance and revenue reports

## Repository structure

- `backend/` – Spring Boot application
- `frontend/` – Angular application
- `database/` – migrations, seed data and database diagrams
- `docs/` – specification, EER and UML diagrams
- `legacy/` – cleaned original JDBC project

## Current status

- [x] Extended project specification
- [x] Initial EER model
- [x] Relational model for subsystems 1 and 2
- [x] Spring Boot project setup
- [x] Angular project setup
- [x] PostgreSQL migration for subsystem 1
- [x] Authentication and authorization
- [ ] Core functional verticals
- [x] Neo4j recommendation module
- [ ] Testing and documentation

Aktuelni pregled po modulima, rute, dozvole i rezultati nastavka rada:
[izveštaj od 30.09.2026.](docs/continuation-2026-09-30.md).
Manager analitika i poseban Administrator frontend još nisu implementirani.

## Running the project


### Requirements

- Docker Desktop running with Linux containers.
- JDK 21 (a JDK, not just a JRE). Maven is downloaded by the committed Wrapper;
  no separate Maven installation is required. Internet is needed on the first run.
- Free local ports: 15432, 7474, 7687 and 8081, or alternatives set in `.env`.

The following commands are for **Windows CMD**, starting in the repository root
(the directory containing `compose.yaml`, not its parent).

```cmd
cd /d C:\Users\DELL\Desktop\museum-management-system\museum-management-system
if not exist .env copy .env.example .env
notepad .env
```

Set your own passwords before the first startup. Keep `NEO4J_USERNAME=neo4j`:
Neo4j Docker cannot change its initial username. Its password must have at least
8 characters. `.env` is ignored by Git; `.env.example` contains examples only.
Use plain `KEY=value` entries without surrounding quotes or shell expressions.
Changing database credentials in `.env` does not change credentials in an already
initialized volume; those must be changed in the database itself.

### Start and inspect the databases

```cmd
docker compose config --quiet
docker compose up -d
docker compose ps
docker compose logs --tail=100 postgres neo4j
docker compose up -d --wait --wait-timeout 180
```

Both services should become `healthy`. `docker compose config` also validates and
prints the resolved configuration, including passwords; use `--quiet` to avoid
printing secrets. Ports are bound to localhost. The services share the `museum`
network and use named `postgres_data`, `neo4j_data` and `neo4j_logs` volumes.
Neo4j Browser is available at http://localhost:7474 (or `NEO4J_HTTP_PORT`).

### Start the backend locally

Set `JAVA_HOME` to the actual installed JDK 21 directory, for example:

```cmd
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"
java -version
cd backend
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

The JDK path above is an example; use the full directory name on your machine.
The backend imports the root `.env` as a properties file when started from
`backend/` (or the repository root). Real environment variables override these
file values. PostgreSQL uses `localhost:POSTGRES_PORT`; Neo4j uses
`bolt://localhost:NEO4J_BOLT_PORT`. Optional `POSTGRES_HOST` and `NEO4J_HOST`
environment variables override the host. `BACKEND_PORT` defaults to 8081.
The old `NEO4J_URI` and `FRONTEND_PORT` entries are unused in this phase.
Subsystem 2 requires `JWT_SECRET` in the process environment; a value in
`.env` alone does not supply the signing key. See the subsystem 2 setup below
before starting the backend or running its full tests.

In another CMD window:

```cmd
curl.exe -i http://localhost:8081/api/health
```

Expected HTTP 200 and JSON (property order is not significant):

```json
{"status":"UP","application":"museum-backend"}
```

Use your `BACKEND_PORT` if changed. This is application liveness, not a database
readiness check. Public routes include health, registration, login and GET
events/interests. See the subsystem 2 route matrix for protected operations.
The explicit `local` profile additionally enables the four catalog CRUD resources
without authentication and binds the HTTP server to 127.0.0.1. Other routes remain
protected. Subsystem 2 provides registration and JWT login with no generated default user. Do not use the
`local` profile for a deployed application.

### Run tests

With JDK 21 selected, from `backend/`:

```cmd
mvnw.cmd clean test
```

Or from the repository root:

```cmd
backend\mvnw.cmd -f backend\pom.xml clean test
```

Tests require Docker Desktop (Linux containers). Testcontainers starts an isolated
PostgreSQL 17.9 container, applies the actual V1 and V2 migrations and validates all JPA
mappings with `ddl-auto: validate`. Test data is generated by tests; the local
museum database is not used or cleared. The PostgreSQL image may be downloaded
on the first run. Unit tests can run alone without Docker:

```cmd
backend\mvnw.cmd -f backend\pom.xml -Dtest=CatalogServiceTests test
```

Flyway automatically applies pending V1 and V2 migrations on backend startup.
The original `V1__create_first_subsystem.sql` remains unchanged.
It creates only the ten tables of subsystem 1, their constraints, indexes and
integrity triggers. PostgreSQL's `btree_gist` extension supports the rule against
overlapping exhibitions in the same hall. The database owner used by Compose
can create it. Applied migrations must not be edited; future schema changes
require a new numbered migration. Hibernate only validates the schema.

### Stop

Stop the foreground backend with `Ctrl+C`. From the repository root:

```cmd
docker compose stop
docker compose start
```

To remove the containers and network while retaining database volumes:

```cmd
docker compose down
```

Do not add `-v` if you want to preserve database data. Angular,
subsystem 3 and Neo4j recommendations are not implemented in this phase.

### Local development ports

The verified configuration uses `POSTGRES_PORT=15432`, `BACKEND_PORT=8081`,
`NEO4J_HTTP_PORT=7474` and `NEO4J_BOLT_PORT=7687`. These values are also in
`.env.example`. Ports 5432, 5433 and 8080 were occupied or reserved locally.
PostgreSQL still listens on port 5432 inside its container.

For an existing `.env`, ensure the port values match. Environment variables set
in CMD override `.env`; clear old overrides in the same window if necessary:

```cmd
set "POSTGRES_PORT="
set "BACKEND_PORT="
```

Use the same PostgreSQL host port for Compose and the backend. Do not stop
unrelated local processes to free the old ports.


## Subsystem 1: museums, exhibitions and catalog

The complete EER mapping, column types, NULL rules, composite keys and design
choices are in [the relational model](database/relational-model-subsystem-1.md).
Approved source documents in `docs/` are unchanged.

| Resource | Methods |
|---|---|
| `/api/museums` | GET, POST; GET/PUT/DELETE `/{id}` |
| `/api/halls` | GET, POST; GET/PUT/DELETE `/{id}` |
| `/api/exhibitions` | GET, POST; GET/PUT/DELETE `/{id}` |
| `/api/exhibition-terms` | GET, POST; GET/PUT/DELETE `/{id}` |

Creation returns 201 with a Location header, reads/updates 200 and deletion 204.
Errors use `application/problem+json`: 400 for invalid input, 404 for missing
resources/parents, 409 for integrity/business conflicts. Responses contain DTOs.

Examples for **Windows CMD**, while the backend runs with the `local` profile.
Replace the sample IDs with the IDs returned by your requests:

```cmd
curl.exe -i -H "Content-Type: application/json" -d "{\"name\":\"Muzej primer\",\"address\":\"Novi Sad\",\"active\":true}" http://localhost:8081/api/museums
curl.exe -i -H "Content-Type: application/json" -d "{\"museumId\":1,\"name\":\"Sala A\",\"capacity\":50}" http://localhost:8081/api/halls
curl.exe -i -H "Content-Type: application/json" -d "{\"hallId\":1,\"name\":\"Izlozba primer\",\"start\":\"2030-01-01T00:00:00Z\",\"end\":\"2030-02-01T00:00:00Z\",\"status\":\"PLANIRANA\",\"onlineVisible\":true,\"terms\":[{\"start\":\"2030-01-05T10:00:00Z\",\"end\":\"2030-01-05T11:00:00Z\",\"capacity\":20,\"status\":\"OTVOREN\"}]}" http://localhost:8081/api/exhibitions
curl.exe -i http://localhost:8081/api/exhibitions
curl.exe -i http://localhost:8081/api/exhibition-terms
```

Exhibition POST requires at least one initial term. Exhibition PUT takes the
same fields except `terms`: it preserves existing terms, which are edited via
`/api/exhibition-terms`. Term POST/PUT uses `exhibitionId`, `start`, `end`,
`capacity`, `status`. Times are ISO 8601 instants (UTC `Z`, or an explicit offset).
Exhibition statuses: PLANIRANA, AKTIVNA, ZAVRSENA, OTKAZANA. Term statuses:
OTVOREN, POPUNJEN, ZAVRSEN, OTKAZAN. An exhibition's last term cannot be deleted
or moved to another exhibition. Exhibition deletion removes its terms in the
same transaction; dependent prices/participations prevent deletion and roll
back the entire operation. Other parent deletes are restricted by foreign keys.

All ten entities have repositories. Author, exhibit, authorship, participation,
price and digital-content REST operations and QR/TTS generation
are deferred; no placeholder endpoints or extra domain tables are introduced.

## Subsystem 2: accounts, employees and events

Email is the required, unique login identifier. There is no separate username
column. Login accepts `email` and `password`; registration stores only a BCrypt
hash. V2 adds fourteen tables and references the existing museums/exhibitions.
See [the complete mapping and REST/RBAC matrix](database/relational-model-subsystem-2.md)
and [verification results](database/subsystem-2-verification.md).

Before running the backend or full test suite, supply a strong `JWT_SECRET`
through the process environment. It must contain at least 32 UTF-8 bytes.
It is intentionally not read from a properties file, and there is no default.
For a disposable local session, the following **PowerShell 7** commands generate
a random key without displaying it:

```powershell
$env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
cd backend
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
```

Use the actual JDK path on your machine. In a deployed environment, inject a
persistent secret through the deployment secret manager. Regenerating the key
invalidates existing tokens. Tokens expire after 30 minutes; account activity
and assigned roles are checked against PostgreSQL on each authenticated request.

Register through POST `/api/auth/register`, with email, password (12–72 characters,
at most 72 UTF-8 bytes), firstName, lastName, optional phone and required
recommendationsConsent. POST `/api/auth/login` returns accessToken, tokenType and
expiresIn. Send the token in the `Authorization: Bearer ...` header.
Passwords and tokens must not be placed in committed request examples or logs.

The first administrator is provisioned explicitly by the local database owner:
register the intended account, note its returned numeric ID, open psql in the
PostgreSQL container, and use the following parameterized psql script. It prompts
for the numeric account ID and contains no password or signing key:

```sql
\prompt 'Existing account ID: ' account_id
BEGIN;
INSERT INTO nalog_uloga (nalog_id, uloga_id, datum_dodele)
SELECT :'account_id'::bigint, id, now()
FROM uloga WHERE naziv = 'ADMINISTRATOR'
ON CONFLICT DO NOTHING;
COMMIT;
```

This is a manual bootstrap step, not an HTTP privilege escalation endpoint.
Subsequent grants use POST `/api/admin/accounts/{id}/roles/{roleId}`.
An administrator who needs to manage events must also receive GUIDE.
Employee subtypes and application roles remain independent.
No administrator was automatically created in the development database.

## Subsystem 3: ticket sales visits and recommendations

V3 adds sales and visitor journey support without changing V1 or V2. The
database migration is
`backend/src/main/resources/db/migration/V3__create_sales_visits_recommendations_subsystem.sql`.
It creates `ulaznica`, `transakcija`, `poseta`, `skeniranje_eksponata`,
`ocena_izlozbe`, `preporucena_ruta` and `stavka_rute`. The complete mapping,
constraints, cardinalities, routes and automation rules are in
`database/relational-model-subsystem-3.md`.

Online purchases use the authenticated visitor and card payment. Cashier sales
may be anonymous and use card, cash or another configured method. The local and
test profiles use a deterministic demonstration payment adapter; production
profiles return service unavailable until a real payment adapter is configured.
Ticket capacity is protected by a row lock in the service and deferred
PostgreSQL constraints. QR tokens are random and contain no personal data.

Validation opens a visit only once, for the requested term and current time.
The visitor can finish the visit, scan available exhibition participations,
read their digital content, rate a finished visit once and generate a non-empty
route. Recommendation output uses Neo4j as a rebuildable anonymous projection;
PostgreSQL curator order is the fallback when consent is absent or Neo4j is
unavailable. Scheduled maintenance updates expired ticket, term, exhibition,
visit and route statuses every 60 seconds; graph synchronization retries on the
same interval.

The backend tests use Testcontainers PostgreSQL and an isolated Neo4j container.
Run the full suite with JDK 21, a process environment `JWT_SECRET` of at least
32 bytes, and:

```powershell
cd backend
.\mvnw.cmd clean test
```

The current implementation intentionally does not add payment processing,
fiscalization, QR image rendering, TTS, frontend changes or new Neo4j-only
business tables.

## Local Swagger / OpenAPI

Springdoc OpenAPI 2.8.17 documents all `/api/**` controller operations and DTOs.
With the `local` profile, open http://127.0.0.1:8081/swagger-ui/index.html.
The JSON specification is at `/v3/api-docs`, YAML at `/v3/api-docs.yaml`.
Documentation is disabled in other profiles. The local server binds to loopback;
existing business route authentication and role checks are unchanged.

```powershell
docker compose up -d
cd backend
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
# For a temporary local session; use the configured secret for persistent tokens.
$env:JWT_SECRET=[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
```

Use **Try it out**, then **Execute** for individual operations. Email is the
login identifier: POST `/api/auth/login` accepts `email` and `password`.
Copy the returned access token into **Authorize → bearerAuth** (without the
`Bearer` prefix). Swagger adds the authorization header; the account must have
the role required by the selected operation. Tokens are not persisted by the UI.
Never paste real credentials into shared screenshots or exported examples.

The OpenAPI integration tests compare the specification against every registered
API controller mapping and verify anonymous documentation access while protected
business routes still return 401. Run `./mvnw.cmd clean test` from `backend` with
Docker available and `JWT_SECRET` set as above.
