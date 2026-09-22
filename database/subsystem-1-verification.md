# Provera implementacije podsistema 1

Datum: 2026-09-22. Izvorni EER i specifikacija nisu menjani. Nema commita ni pusha.

## Rezultati

| Provera | Rezultat |
|---|---|
| Maven Wrapper clean test, Java 21 | BUILD SUCCESS; 23 testa; 0 failures, 0 errors, 0 skipped |
| Service/value-object testovi | 10 uspešnih |
| PostgreSQL/REST integracioni testovi | 10 uspešnih (uključuje parametrizovane slučajeve) |
| Postojeći health testovi | 2 uspešna |
| Postojeći contextLoads | uspešan; sada koristi PostgreSQL Testcontainers |
| Flyway na izolovanom PostgreSQL 17.9 | V1 uspešno primenjen |
| JPA/Hibernate validate | svih 10 entiteta uspešno validirano |
| docker compose config --quiet | exit 0; lozinke nisu ispisivane |
| docker compose up -d / ps | oba servisa healthy; PostgreSQL 15432, Neo4j 7474/7687 |
| Razvojni PostgreSQL localhost:15432/museum | Flyway primenio V1; Hibernate validate prošao |
| Lokalni backend | uspešno pokrenut na 127.0.0.1:8081 uz local profil |
| git diff --check | bez grešaka |
| Tajne | vrednosti tajni iz .env nisu nađene u Git kandidatima; .env ignorisan i nije praćen |

Testcontainers koristi zaseban privremeni PostgreSQL kontejner, bez pristupa razvojnim podacima. Testovi se ne preskaču ako Docker nije dostupan: clean test zahteva Docker Desktop. H2 više nije korišćen jer ne može verno proveriti PostgreSQL exclusion constraint i odložene trigere.

## Stvarni HTTP zahtevi

Osnovni URL: http://localhost:8081. Navedeni ID-evi su stvarni rezultati ove provere; probni zapisi su na kraju uklonjeni. Za ponovno izvršavanje koristiti novovraćene ID-eve.

POST `/api/museums` — 201:

```json
{"name":"Provera podsistema 1","address":"Novi Sad","active":true}
```

Odgovor:

```json
{"id":1,"name":"Provera podsistema 1","foundationYear":null,"address":"Novi Sad","description":null,"active":true}
```

POST `/api/halls` — 201:

```json
{"museumId":1,"name":"Sala za proveru","capacity":30}
```

Odgovor:

```json
{"id":1,"museumId":1,"name":"Sala za proveru","area":null,"floor":null,"capacity":30}
```

POST `/api/exhibitions` — 201:

```json
{"hallId":1,"name":"Izlozba za proveru","start":"2030-01-01T00:00:00Z","end":"2030-02-01T00:00:00Z","status":"PLANIRANA","onlineVisible":true,"terms":[{"start":"2030-01-05T10:00:00Z","end":"2030-01-05T11:00:00Z","capacity":20,"status":"OTVOREN"}]}
```

Odgovor (isti podaci potvrđeni GET `/api/exhibitions/1`, HTTP 200):

```json
{"id":1,"hallId":1,"name":"Izlozba za proveru","description":null,"start":"2030-01-01T00:00:00Z","end":"2030-02-01T00:00:00Z","status":"PLANIRANA","onlineVisible":true,"terms":[{"id":1,"exhibitionId":1,"start":"2030-01-05T10:00:00Z","end":"2030-01-05T11:00:00Z","capacity":20,"status":"OTVOREN"}]}
```

- GET `/api/health` → 200, `{"application":"museum-backend","status":"UP"}`.
- DELETE `/api/exhibition-terms/1` → 409, `Cannot remove or move the last exhibition term`.
- POST `/api/halls` sa `museumId=9223372036854775807` → 404, `Museum not found: 9223372036854775807`.
- DELETE `/api/exhibitions/1` → 204 (uklanja njegove termine u istoj transakciji).
- DELETE `/api/halls/1` → 204.
- DELETE `/api/museums/1` → 204.

PUT, dodatni POST termina, validacija, svi GET/DELETE endpointi i rollback zavisnih brisanja provereni su i integracionim testovima.

## Obuhvat i odluke

V1 sadrži deset tabela, PK/FK/UNIQUE/CHECK ograničenja, indekse, btree_gist extension i dva odložena constraint triggera. Nema tabela podsistema 2/3. Sve kolone i mapiranja su u `relational-model-subsystem-1.md`; izvršivi SQL je `V1__create_first_subsystem.sql`.

Nema preostalih kontradikcija koje blokiraju ovaj podsistem. Dokumentovane implementacione odluke: opciona opisna polja, UTC/timestamptz, poluotvoreni intervali, otkazane izložbe ne blokiraju salu, obavezni početni termini pri POST izložbe. Odobreno mapiranje gerunda i oba složena ključa je očuvano. Sale nemaju novo polje aktivan; time nije proširen odobreni model.

CRUD je razvojno otvoren samo uz profil local. Ostali profili zadržavaju zaštitu. Autentifikacija, Angular, QR/TTS automatizacija, korisnici, prodaja, posete i preporuke nisu implementirani.

## Git status i kompletan spisak fajlova

`M` = izmenjen praćen fajl; `??` = nov/nepraćen. Dokumenti u `docs/eer` i `docs/specification` već su bili nepraćeni pre početka implementacije i nisu menjani.

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
