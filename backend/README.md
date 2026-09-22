# Backend

Java 21 / Spring Boot 3.5.16 / Maven Wrapper 3.9.11.

Windows CMD, from this directory (Docker Desktop running):

```cmd
mvnw.cmd clean test
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

The `local` profile exposes catalog CRUD on localhost only. The default profile
keeps all routes except GET `/api/health` protected. No JWT or users are implemented.

Flyway V1 creates subsystem 1. Hibernate validates it, never creates/updates it.
Tests use an isolated PostgreSQL Testcontainers database, not the development DB.
See the root README for API examples, configuration and complete run instructions.

Ports: PostgreSQL 15432, backend 8081, Neo4j HTTP 7474 / Bolt 7687.
