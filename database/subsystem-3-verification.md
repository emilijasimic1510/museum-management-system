# Verifikacija podsistema 3

## Swagger provera, 22. septembar 2026.

Završni `mvnw.cmd clean test`: **BUILD SUCCESS**, 69 testova, 0 failures,
0 errors, 0 skipped (4 min 19 s). PostgreSQL/Testcontainers, Neo4j i OpenAPI
testovi su uključeni. Lokalni zapis izvršenja: `swagger-final-tests.log`.

Springdoc 2.8.17 prikazuje 87 REST operacija na 49 putanja. U `local`
profilu `/swagger-ui/index.html`, `/v3/api-docs` i `/v3/api-docs.yaml`
vraćaju 200. Dokumentacija je podrazumevano isključena u ostalim profilima.
Postojeća pravila autentifikacije i uloga poslovnih ruta nisu proširena.

Kroz Swagger **Try it out → Execute** provereno je:

| Operacija | Rezultat |
| --- | --- |
| GET `/api/health` | 200, UP |
| GET `/api/events` | 200, prazna lista |
| GET `/api/tickets/me` bez tokena | 401 |

OpenAPI testovi porede sve registrovane API operacije sa specifikacijom i
proveravaju javnu dokumentaciju uz zadržanu zaštitu poslovnih ruta.
Email je identifikator za prijavu; `accessToken` iz odgovora na login unosi
se u Swagger Authorize bez prefiksa `Bearer`.

Implementirano je sedam tabela u V3 migraciji: `ulaznica`, `transakcija`, `poseta`, `skeniranje_eksponata`, `ocena_izlozbe`, `preporucena_ruta` i `stavka_rute`. V1 i V2 migracije nisu menjane.

Backend paket `rs.ac.uns.ftn.museum.journey` sadrži sedam JPA entiteta, jedan složeni ključ i pet enum tipova, sedam repository interfejsa, DTO/mapper, kontrolere za prodaju, posete i preporuke, servise za prodaju, validaciju, skeniranje, ocene, rute, preporuke, održavanje statusa i Neo4j projekciju.

Prvi cilj V3 provere je uspešno ostvaren: izolovani Testcontainers PostgreSQL je primenio V1, V2 i V3; Hibernate `ddl-auto=validate` je inicijalizovao 31 entitet; `JourneyIntegrationTests` ima 14 testova i svi su prošli. Testovi pokrivaju snapshot cene, email/visitor vlasništvo, neuspešno plaćanje i retry, refundaciju, konkurentnu kupovinu poslednjeg mesta, QR validaciju samo jednom, anonimnu blagajničku prodaju, posetu, skeniranje, rutu, ocenu, istek statusa i DB kardinalitete.

Neo4j provera je otkrila i implicitno grupisanje u Cypher izrazu za score. Broj zajedničkih autora izdvojen je u WITH agregaciju pre računanja score-a. Timeout sinhronizacije je 30 sekundi, čitanja 10 sekundi, uz testni heap 256/512 MB.

Razvojni PostgreSQL i Neo4j su healthy. Flyway istorija potvrđuje V1, V2 i V3 sa success=true. Lokalna aplikacija na portu 8081 uspešno je prošla Hibernate validate; GET /api/health vraća 200 i UP.

Provere bezbednosti i granica: V3 ne dodaje username, ne menja odobrene dokumente/EER, nema stvarne naplate, JWT/QR tokeni se ne ispisuju, Neo4j projekcija ne prenosi naloge/email/telefone/transakcije, a istorijski zapisi se ne brišu.

Komande za konačnu lokalnu proveru:

```powershell
$env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
docker compose config --quiet
docker compose up -d
cd backend
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
```

Zatim proveriti `GET http://127.0.0.1:8081/api/health`, Flyway verziju 3 u PostgreSQL-u i da su PostgreSQL/Neo4j kontejneri healthy. Nijedan fajl nije commitovan niti pushovan.

