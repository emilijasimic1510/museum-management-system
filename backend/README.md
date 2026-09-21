# Backend

Java 21 / Spring Boot 3.5.16 / Maven Wrapper 3.9.11.

From this directory, in Windows CMD:

```cmd
mvnw.cmd clean test
mvnw.cmd spring-boot:run
```

See the root README for Java setup, Docker services and environment configuration.
There are no business entities or migrations yet. Tests use H2 only in test scope;
the application uses PostgreSQL. GET /api/health is public; other routes require
authentication. No authentication mechanism or business users are implemented yet.


Local ports: PostgreSQL 15432, backend 8081, Neo4j HTTP 7474 / Bolt 7687.
