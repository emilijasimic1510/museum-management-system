# Verifikacija drugog podsistema

Datum: 22.09.2026. Implementacija je dovršena nad postojećim kodom. Pre rada Git status je bio čist; V2 migracija, entiteti, repozitorijumi, DTO, mapper i servisi već su postojali. Dopunjeni su kontroleri, obrada autentifikacionih grešaka, testovi i dokumentacija; popravljeni su RBAC događaja, izvor JWT ključa i osvežavanje statusa događaja pri promeni kapaciteta.

## Potvrđeno mapiranje

[Relacioni model](relational-model-subsystem-2.md) sadrži sve tabele, kolone, SQL tipove, PK/FK/složene ključeve, UNIQUE, NULL i CHECK pravila, kardinalitete, ON DELETE ponašanje, REST rute i RBAC matricu.

V2__create_users_employees_events_subsystem.sql dodaje 14 tabela: korisnicki_nalog, uloga, nalog_uloga, posetilac, zaposleni, kustos, vodic, blagajnik, menadzer, administrator, interesovanje, posetilac_interesovanje, dogadjaj i prijava_na_dogadjaj. Tabele muzej i izlozba referenciraju se iz V1; ne kreiraju se ponovo.

IS-A je JOINED sa zajedničkim identifikatorom nadtipa i podtipa, diskriminatorom i odloženim PostgreSQL constraint triggerima za totalnost i disjunktnost. Poslovni podtip ne daje RBAC ulogu automatski.

Email je obavezan i jedinstven identifikator za prijavu. Pre testiranja pretraženi su V2, JPA entiteti i DTO: nema username, korisnicko_ime niti login_name atributa. LoginRequest sadrži email i password. Test proverava i tačan skup kolona naloga u PostgreSQL-u. Konfiguracioni spring.datasource.username i Neo4j username odnose se na pristup bazama, ne na model korisnika.

## Testovi

Iz backend direktorijuma, sa JDK 21 i nasumično generisanim JWT_SECRET u environment-u, izvršeno je:

```text
mvnw.cmd clean test
Tests run: 50, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

| Test-suita | Testova | Greške/neuspešni |
|---|---:|---:|
| CatalogIntegrationTests | 10 | 0 |
| CatalogServiceTests | 10 | 0 |
| HealthControllerTests | 2 | 0 |
| MuseumBackendApplicationTests | 1 | 0 |
| PeopleIntegrationTests | 20 | 0 |
| PeopleServiceTests | 7 | 0 |

Postojeća 23 testa sačuvana su. U CatalogIntegrationTests provera deset tabela i entiteta sada obuhvata samo prvi podsistem, umesto svih tabela cele aplikacije. HealthControllerTests dobio je mock JWT zavisnosti; oba postojeća testa ostala su ista. Produkcioni kod prvog podsistema nije menjan.

Novi testovi pokrivaju:
- stvarne V1 + V2 migracije na praznoj izolovanoj PostgreSQL 17.9 bazi i Hibernate validaciju svih 24 JPA entiteta;
- tačan skup kolona naloga, NOT NULL/UNIQUE email i jedinstvenost bez obzira na velika/mala slova;
- registraciju, automatski VISITOR/profil, BCrypt proveru, odbijanje otvorene lozinke u bazi i redakciju DTO odgovora;
- login sa emailom, pogrešnu lozinku, nepostojeći/neaktivan nalog, nevažeći/istekli JWT i pogrešan issuer;
- odmah važeće dodavanje/oduzimanje uloge i deaktiviranje naloga za već izdat token;
- svih pet podtipova, karakteristična obeležja, kreiranje/čitanje/izmenu/brisanje zaposlenog, FK muzeja/naloga i jedinstveni profil;
- zabranu zaposlenog bez podtipa, dva podtipa i brisanja jedinog podtipa direktnim SQL-om;
- nadređenost, ciklus, nepostojećeg nadređenog i RESTRICT brisanje;
- dodelu uloge sa identitetom dodelioca, duplu dodelu i minimum jednu ulogu na servisnom i DB nivou;
- privatnost profila, izbor/izmenu/uklanjanje interesovanja i složene ključeve;
- prijavu, duplu prijavu, odjavu, ponovno prijavljivanje, prisustvo, otkazan/pun događaj, smanjenje kapaciteta i FK vodiča;
- konkurentnu prijavu dva posetioca na poslednje mesto: tačno jedan HTTP 201 i jedan HTTP 409;
- direktnu DB zabranu prekoračenja kapaciteta i dupliranja prijave;
- matricu svih šest uloga i odgovore 400, 401, 403, 404 i 409.

Testovi koriste Testcontainers, bez čišćenja razvojne baze. Očekivane FK greške u negativnim testovima nisu neuspešni testovi. Lozinke su nasumične testne vrednosti, a izveštaj ne sadrži lozinke, hash vrednosti, tokene ili tajni ključ. MockMvc ispis zahteva isključen je za nove integracione testove.

Maven Wrapper u ograničenom procesu prvo nije mogao da pristupi korisničkom Maven direktorijumu; isti zahtevani wrapper uspešno je izvršen sa odobrenim pristupom Maven kešu i Docker-u. Wrapper nije menjan.

## Razvojni PostgreSQL i Docker

Izvršeno:
- docker compose config --quiet — uspešno; quiet sprečava ispis razrešenih lozinki.
- docker compose up -d — uspešno.
- docker compose ps — PostgreSQL 17.9 i Neo4j 5.26.0 su healthy.
- mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local — aplikacija uspešno pokrenuta na 127.0.0.1:8081.
- Flyway validacija: dve migracije, šema na verziji 2.
- Hibernate EntityManagerFactory uspešno inicijalizovan uz neizmenjeno ddl-auto=validate.

Direktan upit razvojnoj bazi potvrdio je:

| Verzija | Opis | success |
|---|---|---|
| 1 | create first subsystem | true |
| 2 | create users employees events subsystem | true |

HTTP provera pokrenute aplikacije:
- GET /api/health → 200, status UP, application museum-backend.
- GET /api/events → 200.
- GET /api/interests → 200.
- GET /api/admin/accounts bez tokena → 401.

Razvojna aplikacija ostavljena je pokrenuta uz privremeni procesni JWT ključ; pri novom pokretanju potrebno je obezbediti JWT_SECRET kako opisuje README. Docker volumeni nisu brisani. Nije korišćen docker compose down -v. Nisu kreirani razvojni administratorski nalozi.

## Automatizacija i bezbednost

Registracija atomski upisuje BCrypt hash, vreme registracije, VISITOR ulogu i profil. Login ažurira poslednju uspešnu prijavu. Dodela čuva vreme i administratora. Prijave menjaju PLANIRAN/POPUNJEN prema zauzetim mestima; kapacitet i minimalne kardinalnosti štite DB triggeri. JWT_SECRET čita se isključivo iz environment promenljive. Nema novih poslova za prodaju, posete, skeniranje, preporuke, TTS ili Neo4j.

Sve nove privatne operacije zahtevaju autentifikaciju i u local profilu. Anonimni katalog CRUD prvog podsistema ostaje dostupan samo u local profilu. Prvi administrator dodeljuje se ručno postojećem nalogu preko DB vlasnika; README sadrži postupak bez fiksnih kredencijala.

## Git status i kompletan spisak izmena

M označava izmenjen praćen fajl, ?? nov fajl:

```text
 M README.md
 M backend/src/main/java/rs/ac/uns/ftn/museum/people/dto/TokenResponse.java
 M backend/src/main/java/rs/ac/uns/ftn/museum/people/security/ApiSecurity.java
 M backend/src/main/java/rs/ac/uns/ftn/museum/people/security/JwtConfiguration.java
 M backend/src/main/java/rs/ac/uns/ftn/museum/people/service/EventService.java
 M backend/src/test/java/rs/ac/uns/ftn/museum/catalog/CatalogIntegrationTests.java
 M backend/src/test/java/rs/ac/uns/ftn/museum/health/HealthControllerTests.java
 M database/relational-model-subsystem-2.md
?? backend/src/main/java/rs/ac/uns/ftn/museum/people/controller/AccountController.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/people/controller/AuthController.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/people/controller/AuthenticationExceptionHandler.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/people/controller/EmployeeController.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/people/controller/EventController.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/people/controller/InterestController.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/people/controller/RoleController.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/people/controller/VisitorController.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/people/controller/VisitorProfileController.java
?? backend/src/test/java/rs/ac/uns/ftn/museum/people/PeopleIntegrationTests.java
?? backend/src/test/java/rs/ac/uns/ftn/museum/people/PeopleServiceTests.java
?? database/subsystem-2-verification.md
```

V2 je već bila praćena u početnom stanju i nije menjana tokom ovog nastavka; proverena je i uspešno radi. V1, odobreni docs dokumenti i produkcioni katalog prvog podsistema nemaju Git razliku. git diff --check prolazi.

Nema preostalih kontradikcija koje blokiraju ovaj podsistem. Tipovi opcionih opisnih polja, email normalizacija i REST/RBAC razrada dokumentovani su u relacionom modelu. Provisioning prvog administratora ostaje eksplicitna operativna radnja vlasnika baze, ne nerešena promena šeme.

Ništa nije commitovano niti pushovano. Nijedan postojeći izvorni fajl nije obrisan.
