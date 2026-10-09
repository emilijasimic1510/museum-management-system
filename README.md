# Museum Management System

Informacioni sistem za muzejske zbirke, izložbe, korisnike, događaje, prodaju ulaznica i digitalno vođenje posetilaca. Projekat za diplomski rad implementira sva tri podsistema: katalog; korisnike/zaposlene/događaje; prodaju/posete/preporuke.

Angular 21.2 i TypeScript 5.9; Java 21, Spring Boot 3.5.16, Maven Wrapper 3.9.11; PostgreSQL 17.9 i Neo4j 5.26. PostgreSQL je izvor poslovnih podataka, Neo4j izvedena projekcija za preporuke. Flyway V1–V3 upravlja šemom, Hibernate je samo validira.

## Implementirano

- Javni katalog muzeja, izložbi, eksponata i autora; filteri i dostupni termini.
- Registracija, JWT prijava, posetilački profil, interesovanja i saglasnost za preporuke.
- Kupovina, transakcije, blagajnička prodaja i jednokratna validacija ulaznice; aktivna poseta, skeniranje QR koda učešća eksponata, digitalni tekst/slika/audio, završetak i ocena.
- Curator: zbirka, postavke, kontekstualni sadržaj, cenovnici/termini i QR tabla. Cashier: prodaja i validacija. Guide: događaji i prijave. Manager: pregled agregata. Administrator: nalozi, uloge i zaposleni.
- SR/EN interfejs, verzionisanje teksta i generisanog zvuka, skladištenje slika, opciona SMTP obaveštenja.
- Neo4j rangiranje i PostgreSQL rezervni redosled kada nema dostupnog grafa/podataka/saglasnosti. BLIZINA koristi razliku u redosledu obilaska, ne metre ili prostorne koordinate.

`local` profil zahteva autentifikaciju i odgovarajuće uloge za zaštićene operacije. Nije zaobilaženje RBAC-a. Lokalni seed i simulacija plaćanja služe demonstraciji; javno produkciono plaćanje nije povezano sa stvarnim procesorom.

## Pokretanje na Windows-u

Preduslovi: Java 21 na PATH, Node.js 22.12+ (podržana Angular 21 verzija), npm, Docker Desktop sa Linux kontejnerima. Za stvarno generisanje zvuka instalirati eSpeak NG ili podesiti drugi implementirani TTS provider.

U PowerShell-u iz korena projekta:

```powershell
# Samo ako .env već ne postoji. Ne prepisuje postojeća podešavanja.
if (!(Test-Path .env)) { Copy-Item .env.example .env }
notepad .env
docker compose up -d
```

U `.env` postaviti vlastite `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT` (podrazumevano 15432), `NEO4J_USERNAME` (neo4j), `NEO4J_PASSWORD`, `NEO4J_HTTP_PORT` (7474), `NEO4J_BOLT_PORT` (7687), `BACKEND_PORT` (8081). Postojeći Docker volume zadržava ranije inicijalizovane kredencijale: samo izmena `.env` ne menja lozinku u postojećoj bazi. Ne koristiti `docker compose down -v` za redovno pokretanje.

U istom terminalu pripremiti JWT ključ i pokrenuti backend:

```powershell
$jwtBytes = New-Object byte[] 48
$jwtRng = [Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRng.GetBytes($jwtBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
$jwtRng.Dispose()
$env:MUSEUM_TTS_PROVIDER = 'espeak'
$env:MUSEUM_TTS_ESPEAK_PATH = 'C:\Program Files\eSpeak NG\espeak-ng.exe'
cd backend
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
```

`JWT_SECRET` se čita direktno iz procesnog okruženja; samo upis u `.env` nije dovoljan. Mora imati najmanje 32 UTF-8 bajta. Primer generiše nov ključ bez prikazivanja; promena ključa poništava stare JWT sesije. Za trajno okruženje ključ sačuvati u privatnoj konfiguraciji van repozitorijuma i ponovo koristiti. Ne dodavati tajne u Git.

U drugom terminalu:

```powershell
cd frontend
npm ci
npm start
```

Otvoriti http://localhost:4200. Angular prosleđuje `/api` na 127.0.0.1:8081 kroz `frontend/proxy.conf.json`. Ako promenite backend port, promeniti i proxy. Lokalni API: http://localhost:8081/api/health; Swagger: http://localhost:8081/swagger-ui.html (samo local). Health endpoint je liveness provera, ne potvrđuje ispravnost svih zavisnosti.

Lokalni seed nalozi su `ana.petrovic@museum.local` (VISITOR), `milica.jovanovic@museum.local` (CASHIER), `nikola.markovic@museum.local` (CURATOR), `jelena.stojanovic@museum.local` (GUIDE), `ivan.nikolic@museum.local` (MANAGER), `marija.ilic@museum.local` (ADMINISTRATOR). Lokalna lozinka definisana je u `DemoDataSeeder.PASSWORD`; ovde se ne objavljuje. Registracija pravi nov posetilački nalog. Seed ne treba koristiti na javnom serveru.

## Kamera, mediji i jezici

Kamera telefona zahteva dozvolu i pouzdan HTTPS origin. Običan `http://IP-racunara:4200` nije dovoljan; sama zamena `http` sa `https` ne postavlja TLS. HTTPS reverse proxy/tunel mora prosleđivati i frontend i `/api`. `frontend/public/runtime-config.js` određuje API i javni URL za QR kodove. Na telefonu QR ne sme sadržati `localhost`. Samopotpisani sertifikat nije pouzdano rešenje bez instaliranja poverenja na telefonu.

## Testiranje QR skeniranja telefonom

QR skeniranje proverava token učešća eksponata u okviru konkretne aktivne posete. Zato ulaznica, poseta i QR tabla moraju pripadati istoj izložbi. Isti muzej sam po sebi nije dovoljan.

Za test na računaru, pokrenuti baze, backend i frontend, prijaviti se kao posetilac, otvoriti aktivnu posetu i kliknuti `Skeniraj eksponat`. Dozvoliti kameru računara i usmeriti je ka QR kodu sa kustoske QR table. Alternativno se može koristiti QR token kao ručna provera, ali to ne proverava fizičku kameru.

Za test telefonom, u jednom CMD prozoru frontend mora da radi na portu 4200, a u drugom pokrenuti:

```cmd
cloudflared tunnel --url http://localhost:4200
```

Cloudflare ispisuje privremenu adresu oblika `https://....trycloudflare.com`. Tu istu HTTPS adresu otvoriti na računaru i telefonu. Kustos mora otvoriti QR tablu preko te HTTPS adrese, izabrati muzej i istu izložbu za koju je posetiocu blagajnik validirao ulaznicu, pa tek onda kliknuti `Pregled`/generisati QR tablu. QR kod treba štampati ili prikazati na drugom ekranu. Posetilac na telefonu otvara istu HTTPS adresu, prijavljuje se, ulazi u aktivnu posetu, klikne `Skeniraj eksponat`, dozvoljava kameru i skenira novi QR kod.

QR kod generisan na `http://localhost:4200` ne treba skenirati na telefonu preko Cloudflare adrese, jer URL u QR kodu pripada drugom originu. Ako se `cloudflared` ponovo pokrene i dobije novu adresu, QR tabla se mora ponovo generisati dok je kustos prijavljen na toj novoj adresi. Poruka da QR URL mora pripadati ovom muzeju najčešće znači da je korišćena stara tabla, druga izložba ili druga HTTPS adresa.

Posle uspešnog skena treba da se prikažu naziv eksponata, kontekstualni tekst, slika i audio ako su pripremljeni. Ponovljeni sken istog eksponata u istoj poseti je odbijen, a sken posle završetka posete više nije dozvoljen. Tunnel je privremen i važi samo dok je njegov CMD prozor otvoren.

Digitalni sadržaj pripada učešću eksponata na konkretnoj izložbi. Za posetilački pristup potreban je odgovarajući tok ulaznice i posete. Kustos čuva tekst, a zatim posebno pokreće generisanje zvuka; čuvanje teksta samo po sebi ne generiše audio.

TTS: `MUSEUM_TTS_PROVIDER=espeak` i `MUSEUM_TTS_ESPEAK_PATH`; alternativno `azure` uz `MUSEUM_TTS_AZURE_KEY`, `MUSEUM_TTS_AZURE_REGION`, `MUSEUM_TTS_AZURE_VOICE`, ili `piper` uz `MUSEUM_TTS_PIPER_PYTHON` i `MUSEUM_TTS_PIPER_MODEL`. Odgovarajući izvršni program/model/glas mora biti stvarno dostupan. Putanje skladišta: `MUSEUM_AUDIO_STORAGE_PATH`, `MUSEUM_TEXT_STORAGE_PATH`, `MUSEUM_IMAGE_STORAGE_PATH` (podrazumevano `backend/runtime/...` pri pokretanju iz backend direktorijuma).

UI prevodi su u `frontend/src/app/core/i18n.ts`. Za sadržaj SR koristi srpski tekst, EN koristi postojeći prevod i vraća SR ako prevod ne postoji. Deo seed prevoda je mapiran u frontend rečniku: ovo nije opšti automatski prevodilac proizvoljnog novog sadržaja.

SMTP je isključen dok se ne postavi `MUSEUM_MAIL_ENABLED=true`. Podesiti host/port, privatne kredencijale, from i TLS preko `MUSEUM_MAIL_*` varijabli iz `application.yml`. Bez toga uspeh poslovne operacije nije dokaz isporučene e-pošte.

## Provere

Docker Desktop mora raditi. Backend testovi koriste sopstveni PostgreSQL Testcontainer, a test grafa zaseban Neo4j; ne resetuju lokalnu demonstracionu bazu. Sa postavljenim procesnim `JWT_SECRET`:

```powershell
cd backend
.\mvnw.cmd clean test package
```

Iz frontend direktorijuma:

```powershell
npm test
npx --no-install tsc --noEmit -p tsconfig.app.json
npm run build
```

Rezultat pakovanja je `backend/target/museum-backend-*.jar`; frontend build je `frontend/dist/museum/browser`. Proizvodni Angular build nije dokaz da je aplikacija javno postavljena. Deployment fajlovi u repozitorijumu nisu potvrda spremnosti kompletnog produkcionog okruženja.

Aktuelni rezultati, odstupanja od specifikacije/EER-a, preostale ručne provere i tok odbrane: [submission-readiness](docs/submission-readiness.md). Raniji datirani izveštaji opisuju ranija stanja i nisu dokaz prolaza trenutne verzije.
