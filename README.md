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
- [ ] PostgreSQL migrations
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
mvnw.cmd spring-boot:run
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
readiness check. Only GET `/api/health` is public. Other routes return 401 to
unauthenticated requests; there is no login, JWT, or generated default user.

### Run tests

With JDK 21 selected, from `backend/`:

```cmd
mvnw.cmd clean test
```

Or from the repository root:

```cmd
backend\mvnw.cmd -f backend\pom.xml clean test
```

The three tests cover Spring context startup, the exact public health response,
and protection of other routes. They do not require Docker: the context test
uses an isolated in-memory H2 database, with Flyway and Hibernate validation
still enabled. Neo4j creates a lazy driver without querying a live server.
Actual PostgreSQL startup and Neo4j readiness are checked separately with the
running application and Docker healthchecks.

Hibernate uses `ddl-auto: validate`. Flyway is enabled without migrations at
this stage. It may create only its own `flyway_schema_history` metadata table;
there are no business tables. A no-migrations warning is expected.

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

Do not add `-v` if you want to preserve database data. No Angular application,
business entities, EER migrations, authentication flow or recommendations are
implemented in this infrastructure phase.

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
