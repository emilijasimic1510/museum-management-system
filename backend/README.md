# Backend

Java 21 / Spring Boot 3.5.16 / Maven Wrapper 3.9.11.

Windows CMD, from this directory (Docker Desktop running):

```cmd
mvnw.cmd clean test
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

Catalog GET endpoints are public in both profiles. Catalog writes require the
appropriate staff role; `local` does not bypass write authorization. Authentication
uses JWT and current account roles. See [CATALOG_API.md](CATALOG_API.md) for the
exhibit, author, participation and digital-content API and its access rules.

Flyway V1–V3 create the approved schema. Hibernate validates it, never creates/updates it.
Tests use an isolated PostgreSQL Testcontainers database, not the development DB.
See the root README for API examples, configuration and complete run instructions.

Ports: PostgreSQL 15432, backend 8081, Neo4j HTTP 7474 / Bolt 7687.
