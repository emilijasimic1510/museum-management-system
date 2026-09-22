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
- [ ] Relational model
- [x] Spring Boot project setup
- [ ] Angular project setup
- [x] PostgreSQL migration for subsystem 1
- [ ] Authentication and authorization
- [ ] Core functional verticals
- [ ] Neo4j recommendation module
- [ ] Testing and documentation

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
The old `NEO4J_URI`, `JWT_SECRET` and `FRONTEND_PORT` entries, if present in an
existing `.env`, are unused in this phase.

In another CMD window:

```cmd
curl.exe -i http://localhost:8081/api/health
```

Expected HTTP 200 and JSON (property order is not significant):

```json
{"status":"UP","application":"museum-backend"}
```

Use your `BACKEND_PORT` if changed. This is application liveness, not a database
readiness check. In the default profile only GET `/api/health` is public.
The explicit `local` profile additionally enables the four catalog CRUD resources
without authentication and binds the HTTP server to 127.0.0.1. Other routes remain
protected. There is no login, JWT, or generated default user. Do not use the
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
PostgreSQL 17.9 container, applies the actual V1 migration and validates all JPA
mappings with `ddl-auto: validate`. Test data is generated by tests; the local
museum database is not used or cleared. The PostgreSQL image may be downloaded
on the first run. Unit tests can run alone without Docker:

```cmd
backend\mvnw.cmd -f backend\pom.xml -Dtest=CatalogServiceTests test
```

Flyway automatically applies `V1__create_first_subsystem.sql` on backend startup.
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

Do not add `-v` if you want to preserve database data. Angular, authentication,
subsystems 2/3 and Neo4j recommendations are not implemented in this phase.

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
price and digital-content REST operations, QR/TTS generation and authentication
are deferred; no placeholder endpoints or extra domain tables are introduced.
