# Backend

Java 21 and Spring Boot 3.5.16 API for the museum catalog, accounts and employees, events, ticketing, visits, digital content, and recommendations.

PostgreSQL is the source of business data. Flyway V1–V3 manages the schema; Hibernate uses `ddl-auto=validate`. Neo4j provides a rebuildable recommendation projection.

## Run and verify

Follow the [root setup guide](../README.md) to start Docker dependencies and configure the process-level `JWT_SECRET`, database credentials, and optional TTS and SMTP providers.

From this directory:

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
```

The default API port is 8081. The local profile initializes demonstration data and enables Swagger UI, while retaining authentication, ownership checks, and role restrictions. Local/test payment simulation does not charge real cards.

With Docker running, execute the isolated test suite and package the application:

```powershell
.\mvnw.cmd clean test package
```

Tests use Testcontainers rather than resetting the local database. The output is `target/museum-backend-*.jar`.

## References

- [Catalog API](CATALOG_API.md)
- [Relational models](../database/README.md)
- [Recorded submission audit](../docs/submission-readiness.md)
- [Deployment notes](../docs/production-deployment.md)
