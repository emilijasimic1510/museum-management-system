# Backend

Java 21 / Spring Boot 3.5.16 / Maven Wrapper 3.9.11. Implementirani su katalog, korisnici i zaposleni, događaji, prodaja, posete, preporuke, Manager i Administrator API, mediji, TTS i opciona e-pošta.

Za procesni `JWT_SECRET`, PostgreSQL/Neo4j i TTS podešavanja pratiti [glavni README](../README.md). Iz ovog direktorijuma:

```powershell
.\mvnw.cmd clean test package
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
```

Testovi zahtevaju Docker i koriste izolovane Testcontainers baze. `local` dodaje seed/Swagger i demonstracioni payment gateway, ali ne uklanja autorizaciju. Flyway V1–V3 kreira šemu, Hibernate je validira. Ne menjati primenjene migracije niti brisati postojeće volume radi pokretanja.

Katalog: [API opis](CATALOG_API.md). Rezultati sadašnje provere: [spremnost za predaju](../docs/submission-readiness.md).
