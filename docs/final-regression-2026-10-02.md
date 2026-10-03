# Finalna regresija i stabilizacija — 2026-10-02

## Obim i zaključak

Završni puni backend je **153/153 PASS** (0 failures, 0 errors, 0 skipped; 21 test klasa). Frontend 140/140, TypeScript app/spec i production build prolaze. Postojeća security regresija prolazi. Nema preostalih potvrđenih regresionih bugova u izvršenim proverama; aplikacija je spremna za ručno acceptance testiranje. U ovoj fazi nisu dodavane funkcionalnosti niti menjani produkcioni moduli, EER, entiteti, PostgreSQL šema ili Flyway V1–V3. Sačuvane su prethodne necommitovane izmene. Nema commit-a ili push-a.

## Uzroci i konkretne ispravke

1. **Prethodni 39/40: `curator.spec.ts`.** Test `allows ADMINISTRATOR and shows only management navigation` očekivao je kustoske linkove i za administratora. Postojeći `App` legitimno prvo bira Admin meni. Zadržana je provera postojeće curatorGuard politike; za CURATOR se sada proverava tačan niz kustoskih linkova, a za ADMINISTRATOR tačan niz administratorskih linkova, uključujući collection. Obe grane eksplicitno isključuju Manager link i visitor/cashier navigaciju. Test nije obrisan niti je assertion oslabljen. Ciljano: 17/17.
2. **Pun frontend: `catalog.spec.ts`.** Posle redirect-a na `/cashier`, ugrađeni `CashierSale` učitava `/api/exhibitions`. Test nije odgovarao na taj legitimni zahtev. Otvoren HTTP zahtev je obarao afterEach i izazivao kaskadne greške inicijalizacije TestBed-a u narednim testovima (uključujući prvi collection test). Sada test eksplicitno očekuje i odgovara na taj jedan zahtev, uz zadržavanje svih redirect assertion-a i zabrane `/api/exhibits` zahteva.
3. **Pun frontend: `cashier.spec.ts`.** Test je direktno menjao obična polja forme posle provere prikaza, što u Angular zoneless test okruženju daje NG0100. Sada bira izložbu, termin i kategoriju kroz stvarne DOM `change` događaje i sačeka asinhrono renderovanje. I dalje proverava pun POST payload, cenu, potvrdu prodaje i transakciju. Produkciona forma nije menjana. Ciljani katalog/cashier/collection: 32/32.
4. **Pun backend: `GraphProjectionTests.realNeo4jProjectionIsIdempotentAndRanksAnonymousCatalog`.** Prvi upit u hladnom Neo4j kontejneru prekoračio je postojeći produkcioni rok od 2 s, pa je servis ispravno vratio fallback mapu. Test je pogrešno zahtevao odmah prisutan graf rezultat. Sada Awaitility ograničeno ponavlja upite do 30 s i i dalje zahteva potpuno iste rezultate 12, 5 i 3, idempotentnost i odsustvo privatnih podataka. Executor se zatvara u finally. Produkcioni timeout, fallback i scoring nisu menjani. Poseban `stalledDriverIsBounded` i dalje proverava brzo vraćanje pri zastoju, bez gomilanja upita.
5. **TTS failure-safe dokaz.** `JourneyIntegrationTests` pokreće pravi Spring kontekst sa namerno nepostojećim executable-om. Dodat test proverava 503/LOCAL_AUDIO_ERROR, odsustvo DigitalContent zapisa i zatim uspešan javni katalog, kupovinu, CASHIER validaciju, VISITOR scan i čitanje konteksta. Registracija/login se izvršavaju kroz postojeće API pomoćne metode. Ne koristi se pravi eSpeak niti se menja TTS arhitektura.

## Rezultati

| Provera | Rezultat | Failures | Errors | Skipped | Trajanje |
|---|---|---:|---:|---:|---|
| Prvi puni backend `mvnw.cmd clean test` | 151/152 | 1 | 0 | 0 | Maven 4 min 53 s; wall 297.15 s |
| Ciljani backend nakon ispravke | 26/26 | 0 | 0 | 0 | Maven 2 min 7 s |
| Završni puni backend `mvnw.cmd clean test` | **153/153**, 21 test klasa | 0 | 0 | 0 | Maven 4 min 9 s |
| Prvi puni frontend | 133/140 | 7 | 0 build errors | 0 | Vitest 10.51 s; wall 22.08 s |
| Završni puni frontend `npm test` | **140/140**, 14/14 fajlova | 0 | 0 | 0 | Vitest 7.66 s; wall 25.51 s |
| `tsc --noEmit -p tsconfig.app.json` | **PASS** | — | 0 | — | exit 0 |
| `tsc --noEmit -p tsconfig.spec.json` | **PASS** | — | 0 | — | exit 0 |
| `npm run build -- --configuration production` | **PASS** | — | 0 | — | Angular 14.003 s; wall 19.44 s |
| `git diff --check` | **PASS** | — | 0 | — | exit 0 |

Završetak je potvrđen zbirom atributa tests/failures/errors/skipped iz svih 21 `backend/target/surefire-reports/TEST-*.xml` fajlova i završnim `BUILD SUCCESS` u `backend/final-backend-regression-pass.log`, završenim 2026-10-02 u 13:06:36 +02:00. Oba izvora daju 153/0/0/0. Završni zbir uključuje dodatni TTS failure-safe test; zato je 153, dok je prvi puni prolaz imao 152 testa. U nastavku rada nije ponavljan već uspešan backend ni frontend suite.

Prvi puni backend je nastavio sve module do kraja; nije prekinut na prvom padu. Jedini backend pad naveden je iznad.

Svih sedam frontend padova prvog punog prolaza:
- `Cashier sale > loads data, submits the existing cashier route and displays the transaction`
- `Public digital catalog > redirects cashier away from all catalog routes`
- `Public digital catalog > shows the correct catalog navigation for anonymous`
- `Public digital catalog > shows the correct catalog navigation for VISITOR`
- `Public digital catalog > shows the correct catalog navigation for CASHIER`
- `Public digital catalog > ignores a stale response after switching resource ids`
- `Spaces, exhibitions and prices management > edits a museum and deactivates through PUT while preserving its history`

Poslednjih pet padova iz ove liste bili su posledica otvorenog HTTP zahteva/neuspelog teardown-a, a ne pet nezavisnih produkcionih problema. Završni puni prolaz potvrđuje njihovo otklanjanje bez menjanja collection produkcionog koda ili njegovih testova.

## Security / RBAC regresija

**PASS u postojećim automatizovanim proverama.** U nastavku su potvrđeni završni Surefire rezultati, uključujući svih šest role-matrix slučajeva, zabranu admin operacija drugim ulogama i anonymous korisniku, MANAGER-only dashboard, Neo4j i TTS failure-safe test. Ponovo su pregledani relevantni assertion-i i guard/API pravila. Frontend rezultat 140/140 potvrđen je sačuvanim logom; nije ponavljan. Ovo nije zaseban penetration test niti ručna browser provera cele matrice.

Pokrenuti su postojeći security testovi u okviru punih suite-ova, uključujući `ExhibitCatalogIntegrationTests`, `LocalExhibitCatalogIntegrationTests`, `ProgramManagementIntegrationTests`, `PeopleIntegrationTests.protectedOperationsFollowRoleMatrix`, `AdminWorkspaceTests`, `ManagerAnalyticsTests`, `JourneyIntegrationTests`, `AudioIntegrationTests` i frontend curator/admin/manager/collection/events/catalog/visitor testove. Pregledani su `ApiSecurity`, frontend guard-ovi i navigacija. Politika nije menjana radi prolaska testova.

| Uloga | Postojeća politika proverena testovima i pregledom ruta |
|---|---|
| ANONYMOUS | Javni katalog, health i auth login/register; nema privatnog sadržaja ni management upisa. Lokalni Swagger je postojeći izuzetak samo local profila. |
| VISITOR | Sopstveni visitor tokovi; nema cashier, curator, guide management, manager, admin ni collection management ovlašćenja. Audio dodatno traži aktivnu sopstvenu posetu i scan konkretne postavke. |
| CASHIER | Prodaja/validacija i pripadajuća ticket prava; druga management prava zahtevaju stvarno dodeljene dodatne uloge. |
| GUIDE | Guide profil i upravljanje događajima/prijavama prema postojećem vlasništvu; nema implicitnog admin pristupa. |
| CURATOR | Katalog/postavke/sadržaj i dozvoljeni collection delovi (izložbe, termini, cene); nema administriranja muzeja/sala ni admin/manager pristupa bez dodatne uloge. |
| MANAGER | Manager dashboard samo uz MANAGER; nema admin privilegija. |
| ADMINISTRATOR | Admin i collection; legacy curator CRUD pristup ostaje po postojećoj politici. Ne dobija MANAGER implicitno. Novi TTS generate endpoint traži baš CURATOR. |

Važna postojeća razlika: ADMINISTRATOR vidi svoj glavni meni, iako stari curator guard/CRUD API dopuštaju i tu ulogu. To nije implicitni MANAGER. TTS generisanje zahteva CURATOR čak i ako administrator može otvoriti legacy digital-content stranicu; backend odbija samo ADMINISTRATOR nalog. Politika nije proširivana. Frontend za naloge sa više uloga ima postojeći prioritet prikaza radnog prostora; backend ovlašćenja proverava iz stvarnih uloga naloga.

## TTS failure-safe

- Pravi Spring kontekst uspešno se inicijalizuje uz `museum.tts.espeak-path=./target/nonexistent-espeak-regression`.
- Test `missingLocalTtsDoesNotBlockCatalogLoginSaleValidationOrVisit` proverava izolovanu grešku generisanja i nastavak poslovnog toka.
- Postojeći `AudioServiceTests` pokriva prazan opis, server-side verziju, tačnu postavku, generated flag, rollback/cleanup, missing WAV, non-zero exit, timeout, nedostajući executable i traversal.
- Ne postoji startup provera dostupnosti TTS procesa niti zavisnost kupovine/validacije od TTS-a. Pravi eSpeak nije potreban za suite.

## Git i lokalni artefakti

Pregledani su `git status --short`, `git ls-files` i neignorirani untracked fajlovi. `.env`, runtime/audio WAV, node_modules, target i logovi nisu praćeni. `.env.example` je namerno praćen i sadrži placeholder vrednosti. Postojeća `.gitignore` pravila potvrđena su kroz `git check-ignore -v` (`.env`, `runtime/`, `node_modules/`, `target/`, `*.log`); nije bilo potrebe za novim pravilom. U praćenim fajlovima nisu nađeni standardni potpisi privatnih ključeva/AWS/GitHub tokena; to nije sveobuhvatni secret audit.

Flyway V1–V3 nemaju diff. Nisu brisani korisnički fajlovi. `clean` je izvršen isključivo kao traženi Maven lifecycle cilj. Prethodne lokalne izmene i novi fajlovi ostaju necommitovani.

Fajlovi menjani samo u ovoj fazi:
- `frontend/src/app/pages/curator.spec.ts`
- `frontend/src/app/pages/catalog.spec.ts`
- `frontend/src/app/pages/cashier.spec.ts`
- `backend/src/test/java/rs/ac/uns/ftn/museum/journey/GraphProjectionTests.java`
- `backend/src/test/java/rs/ac/uns/ftn/museum/journey/JourneyIntegrationTests.java`
- `docs/final-regression-2026-10-02.md`

Dijagnostički logovi su lokalni i gitignored: `backend/final-backend-regression.log`, `backend/final-backend-targeted.log`, `backend/final-backend-regression-pass.log`, `frontend/final-frontend-regression.log`, `frontend/final-frontend-regression-pass.log`, `frontend/final-production-build.log`, `final-diff-check.log`, `final-git-status.log`.

## Warnings (odvojeno od grešaka)

- Production Angular build: `qrcode` nije ESM (CommonJS optimization bailout), postojeća zavisnost demo QR prikaza. Build prolazi; initial bundle je 319.41 kB, ispod postojećeg warning/error budžeta. Upozorenje nije utišavano niti je menjan budžet.
- Java testovi: Mockito/Byte Buddy dinamičko učitavanje Java agenta i OpenJDK CDS upozorenje; suite se izvršava. Nema izmene JVM/test konfiguracije radi skrivanja upozorenja.
- Testovi namerno simuliraju offline/spor Neo4j i emituju fallback WARN i stack trace; assertion-i potvrđuju očekivano ponašanje.
- Git daje postojeća LF/CRLF upozorenja i sandbox upozorenje o čitanju korisničkog globalnog ignore fajla. Repo ignore pravila su direktno proverena; diff check završava exit 0.

## Komande za reprodukciju

PowerShell, iz `backend` uz aktivan Docker Desktop i JDK 21:

```powershell
# Privremeni ključ samo za proces testiranja; ne zapisuje se u fajl.
$env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
.\mvnw.cmd clean test
```

Iz `frontend`:

```powershell
npm test
npx tsc --noEmit -p tsconfig.app.json
npx tsc --noEmit -p tsconfig.spec.json
npm run build -- --configuration production
```

Iz korena projekta:

```powershell
git diff --check
git status --short
git check-ignore -v .env backend/runtime/audio/check.wav frontend/node_modules/check backend/target/check backend/final-backend-regression.log
```

## Ograničenja i ručni acceptance

Automatizovana regresija ne zamenjuje proveru stvarnog browsera, produkcionog okruženja, fizičkog QR-a i sluha. Nije ručno provereno:
- stvarna eSpeak NG instalacija na Windows-u, srpski izgovor i browser audio playback;
- kamera telefona i fizičko skeniranje QR-a;
- raspored na mobilnom uređaju i pristupačnost u stvarnom browseru;
- kompletan tok sa postojećom lokalnom demo bazom i stvarnim demo nalozima;
- produkcioni hosting/reverse proxy, TLS, backup i više backend instanci;
- opterećenje, dugotrajni rad i veliki audio fajlovi.

Za ručni acceptance koristiti postojeći demo i redosled: javni katalog → Visitor login/kupovina → Cashier prodaja/validacija → aktivna poseta/scan → Curator kontekst/Generate → Visitor Play → završetak posete i zabrana novog downloada. Dodatno proveriti po jednu osnovnu radnju za Guide, Collection, Manager i Admin, direktne zabranjene URL-ove i promenu uloga/deaktivaciju sesije. Posebno probati nalog sa više stvarnih uloga i proveriti očekivani prioritet menija.

Preostala TTS ograničenja ostaju dokumentovana u `tts-audio-2026-10-02.md`: samo sr, sinhrono generisanje, lokalni storage, ceo WAV Blob, nemogućnost opoziva već preuzetih bajtova i ručno održavanje orphan fajlova. Ne tvrdi se da su svi mogući bugovi isključeni; izveštaj opisuje konkretno izvršene provere.
