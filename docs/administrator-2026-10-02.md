# Administrator workspace — 02.10.2026.

## Checkpoint

Backend je već imao AccountController/AccountService, RoleController/RoleService i EmployeeController/EmployeeService. Postojali su liste/detalji, kreiranje naloga, izmena emaila/aktivnog statusa, dodela i uklanjanje uloga, kao i kreiranje/izmena zaposlenih sa svih pet podtipova. AccountResponse izlaže samo id, email, active, registeredAt, lastLogin i roles. Lozinka/hash nisu deo odgovora.

Role assignment već čuva datum i administratora koji je izvršio eksplicitnu dodelu. Uklanjanje poslednje uloge je odbijeno. Account i subtype postojećeg Employee zapisa su nepromenljivi, hijerarhija nadređenih ne dozvoljava ciklus, a šema štiti totalnu/disjunktnu specijalizaciju. Brisanje naloga sa povezanim podacima se odbija i transakcija se vraća.

Frontend je imao opšti Auth/RBAC, zaštitu i postojeći `/collection` modul dostupan administratoru. Nije postojao zaseban `/admin` workspace niti namenski demo admin seed. Nisu bili potrebni novi backend API-ji ili promena security politike.

## Implementirano

- `/admin`: stvarni brojevi svih/aktivnih/neaktivnih naloga, zaposlenih i naloga po ulozi; postojeći health status aplikacije. Broj naloga po ulozi nije disjunktan — nalog može imati više uloga.
- Navigacija: Pregled, Korisnički nalozi, Uloge, Zaposleni, Prostori i program, Profil, Odjava.
- Lista naloga sa pretragom emaila, detalj, izmena emaila i aktivacija/deaktivacija uz potvrdu. Deaktivacija ne menja neposredno nesnimljen email iz forme. Destruktivno brisanje nije ponuđeno.
- Kreiranje naloga kroz postojeći API: maskirana početna lozinka, email, ime/prezime i opcioni telefon. Posle uspeha lozinka se briše iz modela forme i nikada se ne prikazuje u detalju/listi. UI i backend proveravaju BCrypt ograničenje 72 UTF-8 bajta.
- Pregled postojećih uloga i linkovi ka dodelama na nalogu. Dodela samo uloga vraćenih sa servera, uklanjanje uz potvrdu, zabrana uklanjanja poslednje uloge u UI i backendu.
- Lista zaposlenih i forma sa nalogom, muzejem, imenom/prezimenom, telefonom, datumom zaposlenja, nadređenim i jednim podtipom. Forma šalje samo atribut izabranog podtipa. Nalog i podtip su zaključani pri izmeni.
- Link ka postojećem `/collection`, bez dupliranja njegovih formi.
- Sopstveni Account/Employee profil. Samodeaktivacija briše lokalnu sesiju; uklanjanje sopstvene ADMINISTRATOR uloge uklanja je i iz frontend keša i napušta zaštićeni ekran.
- Loading, empty, success, bezbedne poruke za greške, ponovno učitavanje i responsive raspored. GET zahtevi imaju timeout. Backend exception tekst se ne prikazuje.
- `adminGuard` na parent i child rutama. Početna putanja/prijava administratora vodi na `/admin`; postojeći RBAC za ostale module ostaje na snazi.

## Postojeći API koji UI koristi

| Metod i ruta | Namena |
|---|---|
| `GET /api/admin/accounts` | Lista, pretraga u UI, dashboard i izbor naloga zaposlenog |
| `GET /api/admin/accounts/{id}` | Detalj i osvežavanje dodeljenih uloga |
| `POST /api/admin/accounts` | Kreiranje kroz postojeći RegisterRequest |
| `PUT /api/admin/accounts/{id}` | Email i active |
| `GET /api/admin/roles` | Postojeće uloge |
| `POST /api/admin/accounts/{id}/roles/{roleId}` | Dodela uloge; backend evidentira datum i administratora |
| `DELETE /api/admin/accounts/{id}/roles/{roleId}` | Uklanjanje dodele, uz zadržavanje najmanje jedne uloge |
| `GET /api/employees` | Lista zaposlenih i nadređenih, dashboard/profil |
| `GET /api/employees/{id}` | Detalj zaposlenog |
| `POST /api/employees` | Kreiranje jednog podtipa |
| `PUT /api/employees/{id}` | Izmena profila bez promene naloga/podtipa |
| `GET /api/museums` | Izbor muzeja |
| `GET /api/accounts/me` | Sopstveni nalog i postojeća autorizacija |
| `GET /api/health` | Postojeći `status` i `application`; nije novi monitoring |
| `POST /api/auth/login` | Postojeća prijava |

Novi API nije dodat. Postojeći DELETE naloga i zaposlenih nije korišćen u UI. `/collection` zadržava svoje postojeće API-je opisane u `docs/program-management-2026-10-01.md`.

Frontend rute: `/admin`, `/admin/accounts`, `/admin/accounts/new`, `/admin/accounts/:id`, `/admin/accounts/:id/edit`, `/admin/roles`, `/admin/employees`, `/admin/employees/new`, `/admin/employees/:id/edit`, `/admin/profile`. Detalji/izmene koriste postojeću strategiju ponovnog kreiranja komponente pri promeni ID-ja.

## Security matrix

| Uloga | Account/role/Employee administracija i `/admin` |
|---|---|
| ADMINISTRATOR | Dozvoljeno |
| MANAGER bez ADMINISTRATOR | 403 / UI zabranjeno |
| CURATOR bez ADMINISTRATOR | 403 / UI zabranjeno |
| CASHIER, GUIDE, VISITOR bez ADMINISTRATOR | 403 / UI zabranjeno |
| Anonimni | API 401 / UI prijava |

Dozvole se proveravaju iz stvarnih uloga naloga; Employee podtip sam po sebi ne daje prava. Postojeći JWT converter na svakom zahtevu proverava aktivnost naloga i trenutne uloge iz baze. ADMINISTRATOR bez MANAGER i dalje nema pristup Manager analitici, niti bez GUIDE dobija Guide operacije. Collection prava ostaju ista.

Ne vraćaju se hash/lozinka, JWT secrets ili interni kredencijali. Account lista nema visitor ime/telefon; Employee detalj sadrži samo podatke potrebne za administraciju zaposlenog. Eksplicitni grant čuva `datum_dodele` i `dodelio_id`; postojeći početni VISITOR assignment iz registracije ostaje postojeće ponašanje bez grant autora.

## Demo Administrator

Samo unutar `local` ogranka postojećeg seeda:

- Email: `demo.admin@museum.local`
- Lozinka: `MuseumDemo!2026`
- Uloga: samo ADMINISTRATOR.
- Employee: Demo Administrator, muzej Demo Museum Novi Sad.
- Podtip: Administrator; NivoAdministracije: `Nalozi, zaposleni i muzejski program`.
- BCrypt hash; ponovljeni seed ne menja identitet niti resetuje lozinku.

Nalog se kreira pri sledećem restartu local backenda. Testiran je u izolovanoj bazi, uključujući stvarni login. U ovom koraku nije izvršen seed nad korisnikovom razvojnom bazom. Sam `development` profil ne kreira demo administratora.

## Fajlovi ovog koraka

Novi frontend:

- `frontend/src/app/core/admin.ts`
- `frontend/src/app/pages/admin-overview.ts`
- `frontend/src/app/pages/admin-lists.ts`
- `frontend/src/app/pages/admin-account.ts`
- `frontend/src/app/pages/admin-employee.ts`
- `frontend/src/app/pages/admin.spec.ts`

Dopunjen frontend (integracija):

- `frontend/src/app/core/auth.ts`
- `frontend/src/app/pages/auth-page.ts`
- `frontend/src/app/routes.ts`
- `frontend/src/app/app.ts`

Backend:

- `backend/src/main/java/rs/ac/uns/ftn/museum/config/DemoDataSeeder.java` — local Administrator nalog/podtip.
- `backend/src/test/java/rs/ac/uns/ftn/museum/people/AdminWorkspaceTests.java` — nov.
- `backend/src/test/java/rs/ac/uns/ftn/museum/people/LocalAdminSeedTests.java` — nov.

Dokumentacija: `docs/administrator-2026-10-02.md`.

Nisu menjani EER, entiteti, Flyway V1–V3, PostgreSQL šema, backend poslovni servisi niti pravila autorizacije. Ranije lokalne izmene su sačuvane. Nema commit-a ili push-a.

## Provere

```text
backend: .\mvnw.cmd -Dtest=AdminWorkspaceTests,LocalAdminSeedTests test
9/9 — 0 failures, 0 errors, 0 skipped; BUILD SUCCESS

frontend: npm.cmd test -- --include=src/app/pages/admin.spec.ts
18/18 — prošlo

node node_modules/typescript/bin/tsc --noEmit -p tsconfig.app.json
prošlo

git diff --check
prošlo
```

Backend testovi koriste Java 21, stvarne JWT-ove i PostgreSQL 17.9 u izolovanom kontejneru sa neizmenjenim migracijama. Proveravaju dozvole i upise za druge uloge, deaktivaciju/aktivaciju i sesije, zabranu brisanja povezanog naloga, strogo ograničen Account DTO, autora/datum granta, poslednju ulogu, svih pet podtipova i pokušaj direktnog drugog podtipa, odvojenost uloge i podtipa, kao i idempotentan seed i login.

Frontend testovi proveravaju stvarne dashboard brojeve, pretragu/praznu listu, maskiranu lozinku i payload kreiranja, email, potvrdu deaktivacije, grant/revoke, poslednju ulogu, sopstvenu sesiju, Employee forme, bezbedne greške, loading/retry i guard po ulozi. Nije pokretan kompletan clean suite niti ručni vizuelni browser test.

## Granice postojećeg modela/API-ja

- Kreiranje administrativnim endpointom trenutno koristi istu registraciju: inicijalno pravi VISITOR ulogu i visitor profil. UI to navodi; za zaposlenog se zatim dodeljuje uloga i pravi Employee. VISITOR se može ukloniti tek nakon dodele druge uloge; profil se ne briše.
- Nema resetovanja lozinke, prinudne promene pri prvom login-u, email pozivnica ili serverske paginacije u ovom obimu. Liste/pretraga/dashboard koriste postojeće liste API-ja.
- Nema promene podtipa postojećeg zaposlenog jer je backend izričito zabranjuje. Nema novog toka za istorijsko premeštanje između podtipova.
- UI prikazuje dodeljene uloge, a metapodaci grant-a ostaju evidentirani na backendu; poseban ekran istorije dodela nije uveden.
- Postojeće pravilo štiti poslednju ulogu naloga, ali ne poslednjeg administratora u celom sistemu. Promena sopstvenih prava je dozvoljena uz jasno upozorenje; nije dodat novi globalni policy.
- Traženi funkcionalni obim je implementiran; ostaje ručna vizuelna/prodajno-istorijska provera u konkretnoj lokalnoj bazi.

## Ručni test

1. Pokrenuti postojeće baze i restartovati backend sa local profilom prema README-u (`.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'` iz `backend`, uz postojeću konfiguraciju/JWT_SECRET). Iz `frontend` pokrenuti `npm.cmd start`.
2. Na `http://localhost:4200/login` prijaviti `demo.admin@museum.local` / `MuseumDemo!2026`. Očekuje se `/admin` i svih sedam navigacionih stavki. Osvežiti direktnu `/admin` adresu.
3. Uporediti brojeve na Pregledu sa listama naloga/zaposlenih. Otvoriti Korisničke naloge, pretražiti email velikim/malim slovima i probati tekst bez rezultata.
4. Kreirati sopstveni test nalog sa jedinstvenim emailom i početnom lozinkom od najmanje 12 znakova. Proveriti VISITOR ulogu na detalju i odsustvo lozinke/hash-a iz odgovora u Network panelu. Izmeniti email.
5. Dodeliti GUIDE ulogu. Ukloniti VISITOR uz potvrdu. Pokušati uklanjanje poslednje GUIDE uloge: dugme je onemogućeno, a direktni API DELETE daje 409. Po potrebi vratiti VISITOR. Promene uloga ne kreiraju Employee automatski.
6. Sa detalja test naloga otvoriti „Kreiraj profil zaposlenog”. Izabrati muzej, GUIDE podtip, ime/prezime, datum zaposlenja i primarni jezik. Sačuvati i izmeniti telefon/nadređenog. Nalog i podtip su zaključani; drugi profil za isti nalog i ciklus nadređenih backend odbija.
7. Deaktivirati test nalog tek posle potvrde. Njegova postojeća sesija i novi login moraju biti odbijeni; nalog i profil ostaju vidljivi. Aktivirati ga ponovo. Ne testirati deaktivaciju na jedinom administratorskom nalogu.
8. Otvoriti Uloge: postojeće uloge i izbor naloga vode na njegovu dodelu. Otvoriti Profil: sopstveni Account/Employee podaci i NivoAdministracije. „Prostori i program” otvara postojeći `/collection` sa Museum/Hall/Exhibition/Term/Cenovnik pravima.
9. Prijaviti Manager, Curator, Cashier/Guide ili Visitor bez ADMINISTRATOR: `/admin` je zabranjen, admin API daje 403. Bez prijave UI vodi na login, API daje 401. Admin bez MANAGER nema pristup `/manager`.
10. Za grešku isključiti backend, osvežiti listu i zatim vratiti vezu/ponovo učitati. Na uskom prozoru proveriti prelom navigacije i formi. Preći između detalja dva naloga: podaci i potvrde moraju pripadati trenutno otvorenom ID-ju.
