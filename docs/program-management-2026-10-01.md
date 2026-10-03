# Prostori i program — 2026-10-01

## Obim i postojeće ponašanje

Postojali su CRUD muzeja, sala, izložbi i termina, provera preklapanja izložbi u sali, javna vidljivost izložbi, zaštita istorijskih veza pri brisanju i javni pregled trenutno važećih cena. Muzej već ima `active`; sala nema polje za deaktiviranje. Cena ima složeni ključ (izložba, početak važenja, kategorija), a ulaznica čuva sopstvenu plaćenu cenu.

Dodat je Angular modul `/collection` (navigacija „Prostori i program”), sa listama, detaljima, kreiranjem, izmenom, povezanim listama, potvrdama, povratnim porukama i prilagodljivim rasporedom. Muzej se deaktivira postojećim PUT zahtevom; ostali zapisi imaju potvrdu brisanja, koje backend odbija kada postoje zaštićene veze.

Dodata je zaštićena istorija i unos/izmena cenovnika, sa proverom preklapanja perioda iste kategorije. Susedni periodi su dozvoljeni: kraj prethodnog jednak početku sledećeg. Kategorija i početak postojećeg perioda su nepromenljivi. Promena cene ne menja `CenaPlaćena`. Kraj perioda ne može isključiti kupovine koje ga već koriste. Upisi za jednu izložbu su serijalizovani zaključavanjem.

Kapacitet termina mora biti pozitivan. Površina i kapacitet sale, kada su uneti, moraju biti pozitivni; ostaju opcioni prema postojećem modelu. Termin sa kartama ne može promeniti izložbu ili vreme, niti smanjiti kapacitet ispod broja neotkazanih karata. Izmena termina zaključava isti zapis kao prodaja. OTVOREN/POPUNJEN se usklađuju sa zauzećem.

Nisu menjani EER, entiteti, Flyway migracije ni šema. Nije dodat opšti Manager/Admin modul. Postojeće lokalne izmene drugih tokova sačuvane su. Nema commit-a ni push-a.

## API i dozvole

| Rute | Čitanje | Upis |
| --- | --- | --- |
| `GET /api/museums`, `GET /api/museums/{id}`; `POST /api/museums`, `PUT/DELETE /api/museums/{id}` | Postojeća javna politika | ADMINISTRATOR |
| `GET /api/halls`, `GET /api/halls/{id}`; `POST /api/halls`, `PUT/DELETE /api/halls/{id}` | Postojeća javna politika | ADMINISTRATOR |
| `GET /api/exhibitions`, `GET /api/exhibitions/{id}`; `POST /api/exhibitions`, `PUT/DELETE /api/exhibitions/{id}` | Javna vidljivost za javnost; interni pregled za CURATOR/ADMINISTRATOR | CURATOR ili ADMINISTRATOR |
| `GET /api/exhibition-terms`, `GET /api/exhibition-terms/{id}`; `POST /api/exhibition-terms`, `PUT/DELETE /api/exhibition-terms/{id}` | Prema vidljivosti izložbe | CURATOR ili ADMINISTRATOR |
| `GET /api/prices?exhibitionId={id}` | Postojeći javni cenovnik, bez interne napomene | Nije ruta za upis |
| `GET/POST /api/exhibitions/{id}/prices/management` | CURATOR ili ADMINISTRATOR | CURATOR ili ADMINISTRATOR |
| `PUT /api/exhibitions/{id}/prices/management?category={category}&validFrom={ISOInstant}` | — | CURATOR ili ADMINISTRATOR |

Poslednje dve rute su nove; ostale već postoje. Novi frontend koristi CRUD rute i management cenovnik. Javni `/api/prices` ostaje za postojeći prodajni tok. Anonymous, VISITOR, CASHIER i GUIDE nemaju prava upisa. Kombinovani CASHIER+GUIDE ne dobija management prava. Angular guard prati ova prava, backend je glavni izvor zaštite. CURATOR može čitati javne muzeje/sale radi izbora, ali nema njihove management stranice niti prava upisa.

## Fajlovi ovog koraka

Backend, prefiks `backend/src/main/java/rs/ac/uns/ftn/museum/catalog/`:

- `dto/HallRequest.java`, `dto/TermRequest.java`, `dto/InitialTermRequest.java`
- `dto/PriceManagementDtos.java` (nov)
- `service/ScheduleRules.java`, `service/TermService.java`
- `service/PriceManagementService.java` (nov)
- `controller/PriceManagementController.java` (nov)

Testovi, prefiks `backend/src/test/java/rs/ac/uns/ftn/museum/catalog/`:

- `CatalogServiceTests.java`, `CatalogIntegrationTests.java`
- `ProgramManagementIntegrationTests.java` (nov)

Frontend:

- `frontend/src/app/core/collection-management.ts` (nov)
- `frontend/src/app/pages/collection-list.ts` (nov)
- `frontend/src/app/pages/collection-editor.ts` (nov)
- `frontend/src/app/pages/collection-prices.ts` (nov)
- `frontend/src/app/pages/collection.spec.ts` (nov)
- `frontend/src/app/routes.ts`, `frontend/src/app/app.ts`, `frontend/src/styles.css`
- Ovaj dokument: `docs/program-management-2026-10-01.md`

## Provere

- Backend: `./mvnw.cmd -Dtest=CatalogIntegrationTests,CatalogServiceTests,ProgramManagementIntegrationTests test` — 25 prošlo, 0 grešaka, 0 preskočenih (Java 21; testovi sa PostgreSQL kontejnerom).
- Frontend: `npm.cmd test -- --include=src/app/pages/collection.spec.ts` — 13 prošlo.
- `node node_modules/typescript/bin/tsc --noEmit -p tsconfig.app.json` — prošlo.
- `git diff --check` — prošlo; Git prijavljuje samo postojeće LF/CRLF napomene.
- Nije pokretan kompletan clean test suite. Ručni pregled u browseru nije izvršen.

## Demo i ručni test

Lokalni seed: `demo.curator@museum.local`, lozinka `MuseumDemo!2026`. Muzej `Demo Museum Novi Sad`; sale `Galerija svetlosti`, `Galerija susreta`, `Galerija gradskih priča`. Izložbe `Tragovi svetlosti` (ODRASLI 8 / STUDENT 5), `Veče skulpture` (6 / 4), `Grad kroz objektiv` (7 / 4). Datume proveriti u lokalnoj bazi/UI jer zavise od pokretanja seed-a. Ovaj korak ne menja seed i ne kreira administratora.

1. Pokrenuti postojeći lokalni backend/frontend prema README-u. Prijaviti curator nalog; otvoriti „Prostori i program” (`/collection`).
2. Otvoriti `Grad kroz objektiv`, pregledati detalj, termine i cenovnik. Izmena treba da učita postojeće vrednosti i prikaže uspeh nakon čuvanja.
3. Za novi test izabrati salu i proveriti datume postojećih izložbi u njoj. Kreirati test izložbu u slobodnom budućem periodu, sa prvim terminom unutar tog perioda i pozitivnim kapacitetom. Pokušaj preklapanja treba da prikaže bezbednu poruku o konfliktu.
4. Otvoriti njene termine; dodati termin, izmeniti kapacitet. Kraj pre početka, kapacitet nula i termin van perioda izložbe moraju biti odbijeni.
5. U njenom cenovniku dodati kategoriju TEST, cenu 10 i ograničeni period. Dodati sledeći period sa početkom jednakim kraju prethodnog. Proveriti da preklapajući period iste kategorije bude odbijen i da forma sačuva unos. Druga kategorija sme imati isti period.
6. Na terminu koji već ima prodate karte pokušati promenu vremena ili premali kapacitet: očekuje se konflikt. Povećanje kapaciteta je dozvoljeno. Za proveru snapshot-a uporediti cenu postojeće karte pre i posle izmene cenovnika; ostaje ista (pokriveno i backend testom).
7. Za muzeje i sale koristiti postojeći ADMINISTRATOR nalog, ako je dostupan. Otvoriti `/collection/museums`, kreirati test muzej, njegovu salu i proveriti povezanu listu. Negativna/nulta površina ili kapacitet su nevažeći. Muzej deaktivirati potvrdom; ostaje u listi kao neaktivan. Brisanje sale sa vezanim podacima mora biti odbijeno.
8. Prijaviti `demo.staff@museum.local` (CASHIER+GUIDE): direktno otvaranje `/collection` mora biti zabranjeno. Ponoviti sa visitor nalogom. Bez prijave interni UI vodi na prijavu; backend odbija neovlašćene upise.
9. Proveriti forme i liste na uskom prozoru: jedna kolona, dugmad se prelamaju. Za prazan prikaz koristiti muzej bez sala ili novu izložbu bez cenovnika; prekid backend veze treba da završi loading i prikaže bezbednu grešku.

## Granice

Nema novih polja za deaktiviranje sale/izložbe/termina: koriste se postojeći statusi i bezbedno brisanje. Za museum/hall upis potreban je stvarni ADMINISTRATOR nalog; novi demo administrator nije dodat. Nije ostao planirani funkcionalni deo ovog opsega za implementaciju; vizuelni ručni test ostaje za lokalnu proveru.
