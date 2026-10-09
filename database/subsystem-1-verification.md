# Subsystem 1 verification

Date: 2026-09-22. This is a historical implementation-stage report. It does not describe the current feature set or establish that a later revision passes. The source specification and EER models were unchanged; no commit or push was made during this review.

## Recorded results

| Check | Result |
|---|---|
| Maven Wrapper clean test, Java 21 | BUILD SUCCESS; 23 tests; 0 failures, 0 errors, 0 skipped |
| Service/value-object tests | 10 passed |
| PostgreSQL/REST integration tests | 10 passed, including parameterized cases |
| Health tests | 2 passed |
| Application context test | Passed using PostgreSQL Testcontainers |
| Flyway on isolated PostgreSQL 17.9 | V1 applied successfully |
| JPA/Hibernate validation | All 10 entities validated |
| docker compose config --quiet | Exit 0; credentials were not printed |
| docker compose up -d / ps | Both services healthy; PostgreSQL 15432, Neo4j 7474/7687 |
| Development PostgreSQL | V1 applied; Hibernate validation passed |
| Local backend | Started on 127.0.0.1:8081 |
| git diff --check | Passed |
| Secret check | Actual .env secret values were not found in Git candidates; .env was ignored |

Testcontainers used a separate temporary PostgreSQL instance, without resetting development data. Docker was required; tests were not skipped when unavailable. H2 was replaced because it cannot faithfully verify PostgreSQL exclusion constraints and deferred triggers.

## Recorded HTTP checks

The base URL was `http://localhost:8081`. Temporary records were removed after the review. Repeated checks must use newly returned IDs.

| Operation | Recorded result |
|---|---|
| POST /api/museums | 201, museum ID 1 |
| POST /api/halls with museumId 1 | 201, hall ID 1 |
| POST /api/exhibitions with a nonempty terms list | 201, exhibition and admission-time ID 1 |
| GET /api/exhibitions/1 | 200, matching saved data |
| GET /api/health | 200, application museum-backend, status UP |
| DELETE /api/exhibition-terms/1 | 409, cannot remove or move the last exhibition term |
| POST /api/halls with museumId 9223372036854775807 | 404, museum not found |
| DELETE /api/exhibitions/1 | 204, admission times removed in the same transaction |
| DELETE /api/halls/1 | 204 |
| DELETE /api/museums/1 | 204 |

The exhibition interval was 2030-01-01 to 2030-02-01, with a 20-place admission time on 2030-01-05 from 10:00 to 11:00 UTC. The hall capacity was 30. PUT, additional admission-time creation, validation, all GET/DELETE endpoints, and rollback of dependent deletions were also covered by integration tests.

## Scope and decisions at that date

V1 introduced ten tables, primary/foreign/unique/check constraints, indexes, the `btree_gist` extension, and two deferred constraint triggers. It did not include subsystem 2 or 3 tables. See the [relational model](relational-model-subsystem-1.md) and `V1__create_first_subsystem.sql`.

Documented decisions included optional descriptive fields, UTC/timestamptz, half-open intervals, cancelled exhibitions not occupying halls, and mandatory initial admission times. Participation and both composite keys retained the approved mapping. No hall activation field was added.

At this early stage, local catalog CRUD was anonymous, and authentication, Angular, QR/TTS, accounts, sales, visits, and recommendations had not yet been implemented. Those statements are historical: the current application implements those modules and protects management writes in the local profile.

## Historical file inventory

The following status snapshot belongs to the original review, not the current working tree. `M` means modified; `??` means untracked. Source documents under `docs/eer` and `docs/specification` were already untracked and were not edited.

```text
 M README.md
 M backend/README.md
 M backend/pom.xml
 M backend/src/main/java/rs/ac/uns/ftn/museum/config/SecurityConfig.java
 M backend/src/main/resources/application.yml
 M backend/src/test/java/rs/ac/uns/ftn/museum/MuseumBackendApplicationTests.java
 M backend/src/test/resources/application-test.yml
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/controller/ExhibitionController.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/controller/HallController.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/controller/MuseumController.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/controller/TermController.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/domain/Author.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/domain/Authorship.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/domain/AuthorshipId.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/domain/ContentType.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/domain/DigitalContent.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/domain/Exhibit.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/domain/ExhibitParticipation.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/domain/Exhibition.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/domain/ExhibitionPrice.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/domain/ExhibitionPriceId.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/domain/ExhibitionStatus.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/domain/ExhibitionTerm.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/domain/Hall.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/domain/Museum.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/domain/TermStatus.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/dto/ExhibitionCreateRequest.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/dto/ExhibitionResponse.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/dto/ExhibitionUpdateRequest.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/dto/HallRequest.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/dto/HallResponse.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/dto/InitialTermRequest.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/dto/MuseumRequest.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/dto/MuseumResponse.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/dto/TermRequest.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/dto/TermResponse.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/exception/ApiExceptionHandler.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/exception/ConflictException.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/exception/InvalidRequestException.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/exception/NotFoundException.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/mapper/CatalogMapper.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/repository/AuthorRepository.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/repository/AuthorshipRepository.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/repository/DigitalContentRepository.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/repository/ExhibitParticipationRepository.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/repository/ExhibitRepository.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/repository/ExhibitionPriceRepository.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/repository/ExhibitionRepository.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/repository/ExhibitionTermRepository.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/repository/HallRepository.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/repository/MuseumRepository.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/service/ExhibitionService.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/service/HallService.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/service/MuseumService.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/service/ScheduleRules.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/catalog/service/TermService.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/config/LocalSecurityConfig.java
?? backend/src/main/resources/db/migration/V1__create_first_subsystem.sql
?? backend/src/main/resources/local-server.yml
?? backend/src/test/java/rs/ac/uns/ftn/museum/catalog/CatalogIntegrationTests.java
?? backend/src/test/java/rs/ac/uns/ftn/museum/catalog/CatalogServiceTests.java
?? backend/src/test/java/rs/ac/uns/ftn/museum/support/PostgresTestBase.java
?? database/relational-model-subsystem-1.md
?? database/subsystem-1-verification.md
?? "docs/eer/EER_1_Muzeji_Izlozbe_Katalog (2).png"
?? "docs/eer/EER_2_Korisnici_Zaposleni_Dogadjaji (1).png"
?? "docs/eer/EER_3_Prodaja_Posete_Preporuke (1).png"
?? "docs/eer/EER_Celokupan_Model (1).png"
?? "docs/specification/Specifikacija_diplomskog_Emilija_Simic_RA3-2022 (1).docx"
```
