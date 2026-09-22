# Visitor digitalni katalog

## Stranice i API

Sve četiri rute koriste postojeći `visitorGuard` sa `publicCatalog: true`: dozvoljeni su anonimni korisnik i VISITOR, dok se CASHIER vraća na `/cashier`.

| Frontend | Javni GET API |
| --- | --- |
| `/exhibits` | `/api/exhibits` |
| `/exhibits/:id` | `/api/exhibits/{id}`, `/api/exhibits/{id}/participations` |
| `/authors` | `/api/authors` |
| `/authors/:id` | `/api/authors/{id}` |

Autorstva se prikazuju iz `authors` polja odgovora eksponata (`id`, `name`, `contributionType`), bez dodatnih zahteva. Katalog šalje anonimne zahteve bez JWT-a; njegov 401/403 prikazuje poruku umesto preusmeravanja ili brisanja postojeće prijave. Zaštićeni tokovi zadržavaju postojeću obradu autentifikacije.

Lista eksponata ima lokalnu pretragu naziva i filtere tipa, materijala i perioda. Sve stranice imaju loading, prazno i error stanje; detalji imaju 404. Ne prikazuju QR, inventarske podatke, posete ni kontrole skeniranja.

## Ograničenja stvarnog API-ja

- Digitalni sadržaj nema javnu read rutu; katalog ga zato ne traži. Postojeći sadržaj i skeniranje ostaju u aktivnoj poseti.
- Detalj autora ne vraća eksponate. Stranica ne pravi N+1 zahteve da bi ih rekonstruisala.
- Nema media storage infrastrukture ni lokalnih slika/audio zapisa. Demo koristi devet lokalnih TEKST resursa u `public/demo`, dostupnih postojećem toku posete. To su javni demonstracioni tekstovi, ne skladište za zaštićeni produkcioni sadržaj.
- Local seed je idempotentno dopunjen drugim autorom i koautorstvima. Postojeća nedostupna demo MP3 referenca zamenjena je tekstom samo kada odgovara originalnom placeholderu. ID-jevi učešća i QR vrednosti nisu menjani. Za primenu na već pokrenut lokalni backend potreban je restart sa postojećim local podešavanjima. EER, šema i migracije nisu menjani.

## Ručna provera

1. Pokrenuti backend sa postojećim local podešavanjima i bazama, a iz `frontend` pokrenuti `npm start`. Otvoriti `http://localhost:4200/exhibits` bez prijave.
2. Proveriti kartice, pretragu „tišina“, sva tri filtera i njihovo poništavanje. Otvoriti detalj preko kartice; proveriti autore, tip, materijal, poreklo, period, opis i kontekst izložbe. Otvoriti autora i listu autora. Posle primene seeda postoje najmanje 3 izložbe, 9 eksponata i 2 autora.
3. Otvoriti `/exhibits/999999999` i `/authors/999999999`: očekuje se odgovarajuća poruka da zapis nije pronađen. Isključiti backend ili blokirati zahtev u DevTools: proveriti poruku i ponovno učitavanje. Praznu listu i 401/403 pokrivaju automatizovani testovi.
4. Prijaviti lokalni nalog `demo.visitor@museum.local` postojećom demo lozinkom iz lokalne konfiguracije. Proveriti stavku „Digitalni katalog“, iste stranice i osvežavanje browsera. `/cashier` ne sme otvoriti blagajnu.
5. Odjaviti se; prijaviti `demo.staff@museum.local` postojećom demo lozinkom. Očekuje se `/cashier`, uloga „Blagajnik“ i odsustvo kataloga i visitor menija. Direktno otvaranje svake od četiri nove rute mora vratiti blagajnu. Odjaviti se i proveriti ponovni anonimni pristup katalogu.
6. Za regresiju postojećeg toka: visitor kupuje kartu sa stranice izložbe; u „Ulaznice“ proveriti QR. Blagajnik validira tu kartu i njen termin; visitor potom otvara aktivnu posetu, skenira pripadajući eksponat i proverava preporuke. QR koristiti samo u tim zaštićenim ekranima, ne kopirati u logove.
7. U responsive režimu proveriti sve četiri nove stranice na 375×667, 390×844, 768×1024 i desktopu. Proveriti meni, dodirne kontrole, fokus tastaturom i odsustvo horizontalnog skrolovanja cele strane.

## Izvršene provere

- TypeScript: `node node_modules/typescript/bin/tsc --noEmit -p tsconfig.app.json` — prošao.
- `npm run build -- --configuration production` — prošao; ostalo postojeće CommonJS upozorenje za `qrcode`.
- `npm test` — 29 testova u 3 fajla, svi prošli.
- Backend: `mvn test -q -Dtest=LocalDemoJourneyTests,JourneyIntegrationTests,GraphProjectionTests` uz podešen test JWT secret — 21 test, svi prošli. Obuhvaćeni seed/idempotentnost, ticket/visit/cashier tok i graf projekcija/preporuke.
- Browser sa stvarnim API-jem: anonimna lista/detalj/autori, pretraga, 404, visitor prijava i refresh, cashier prijava/meni i zabrana direktnog `/exhibits` pristupa. Sve četiri kataloške stranice proverene na četiri navedene veličine: bez horizontalnog prelivanja i QR polja.
- `git diff --check` — prošao. Nije rađen commit ni push.

## Izmenjeni fajlovi

- `frontend/src/app/pages/exhibits.ts`, `exhibit-detail.ts`, `authors.ts`, `author-detail.ts`: nove stranice.
- `frontend/src/app/core/catalog.ts`: javni modeli i zajednička obrada stanja/zastarelih odgovora.
- `frontend/src/app/pages/catalog.spec.ts`: testovi kataloga, guardova i navigacije.
- `frontend/src/app/core/api.ts`, `auth.ts`: anonimni zahtevi kataloga bez promena ponašanja zaštićenih zahteva.
- `frontend/src/app/app.ts`, `routes.ts`, `frontend/src/styles.css`: navigacija, rute i responsive stilovi.
- `frontend/angular.json`: kopiranje lokalnih resursa.
- `frontend/public/demo/`: `tišina-u-plavom.txt`, `dodir-prostora.txt`, `pre-nego-sto-svane.txt`, `krug-u-bronzi.txt`, `ritam-kamena.txt`, `linija-u-pokretu.txt`, `grad-001.txt`, `grad-002.txt`, `grad-003.txt`.
- `backend/src/main/java/rs/ac/uns/ftn/museum/config/DemoDataSeeder.java`: drugi autor, koautorstva i tekstualna zamena demo audio placeholdera.
- `backend/src/test/java/rs/ac/uns/ftn/museum/journey/LocalDemoJourneyTests.java`: povezanost i idempotentnost seeda.
- `frontend/README.md`: ovaj pregled i postupak provere.
