# Slike eksponata i izložbi

Implementacija proveravana 2026-10-05. Nema Flyway migracije niti izmene šeme.

## Model i audit

- Postojeći `DigitalContent` pripada konkretnom učešću eksponata na izložbi i već ima tip `SLIKA` (nije dodat novi enum `IMAGE`).
- Audio ima lokalni file storage. Pre ovog zadatka slike nisu imale upload/storage endpoint; postojeći sadržaj je mogao da čuva samo referencu.
- `Exhibition` nema cover polje. `DigitalContent` nije semantički ispravan za javni cover jer obavezno pripada postavci i podrazumeva pristup tokom obilaska.
- Minimalno rešenje: posebni javni image slotovi, vezani za ID postojećeg eksponata/izložbe u file storage-u. Jedan PNG po javnom slotu. Nema URL kolona, nove tabele, binarnih podataka u PostgreSQL ili promena DTO-a kataloga.
- Contextual upload kreira novu verziju postojećeg `DigitalContent` tipa `SLIKA`, sa neprozirnom internom referencom. Reference privatnih fajlova se ne vraćaju u DTO-u. Stare verzije ostaju dostupne kao zapisi sadržaja; curator ih može ukloniti postojećom radnjom brisanja sadržaja.

## Storage i konfiguracija

- `MUSEUM_IMAGE_STORAGE_PATH` / `museum.image.storage-path`: podrazumevano `./runtime/images`, relativno radnom direktorijumu backend-a.
- `MUSEUM_IMAGE_MAX_BYTES` / `museum.image.max-bytes`: podrazumevano 5242880 (5 MiB). HTTP multipart ograničenje fajla koristi istu vrednost; maksimalan ceo zahtev je 6 MB. Frontend upozorava na 5 MiB; ako menjate serverski limit naviše, uskladite UI limit.
- Javna imena generiše server: `exhibit-{id}.png`, `exhibition-{id}.png`. Privatna imena koriste UUID. Originalno ime upload-a se ignoriše.
- Upis ide preko privremenog fajla i atomskog premeštanja. Ne hotlinkuju se spoljne slike.
- `runtime/` je već ignorisan u Git-u. Za produkciju montirati trajan volume; kopiju baze i image direktorijuma praviti zajedno. Više instanci mora deliti isto skladište. Bez skladišta UI prikazuje placeholder.
- Brisanje zapisa sadržaja/entiteta ukida pristup preko API-ja; automatsko fizičko čišćenje napuštenih fajlova nije uvedeno. Ne brisati slike prema korisnički prosleđenoj putanji.

## Security i API

- Dozvoljeni su samo PNG i JPEG. MIME se poredi sa stvarno dekodiranim formatom; SVG, pogrešan MIME, pokvareni fajlovi i prazni upload-i se odbijaju.
- Provera dužine pre dekodiranja; najviše 6000 px po dimenziji i 12 miliona piksela. Fajl se ponovo enkoduje u PNG, bez prenosa originalnih metapodataka ili dodatnog aktivnog sadržaja.
- Sve interne putanje prolaze strogu whitelist proveru, proveru roditeljskog direktorijuma i simboličkog linka. Nema arbitrary-file-read rute.
- Odgovori slika imaju `image/png`, `X-Content-Type-Options: nosniff` i `Cache-Control: no-store`.
- `GET /api/media/exhibits/{id}`: javni katalog preview, samo za postojeći eksponat.
- `GET /api/media/exhibitions/{id}`: javni cover, samo za postojeću `onlineVisible` izložbu.
- `POST/GET /api/curator/media/{exhibits|exhibitions}/{id}`: upload/preview javnog slota, CURATOR ili ADMINISTRATOR, u skladu sa uređivanjem kataloga. Preview omogućava proveru cover-a pre javnog objavljivanja izložbe.
- `POST /api/curator/participations/{id}/images`: multipart `file`, kreira novu contextual verziju. `GET .../images/{contentId}`: curator preview. Isti katalog role pristup, bez izmene drugih pravila.
- `GET /api/visits/{visitId}/content/{participationId}/images/{contentId}`: poziva postojeći `VisitService.content` pre čitanja fajla. Zahteva vlasnika aktivne posete, odgovarajuću dostupnu postavku i zabeležen scan. Proverava tip i pripadnost sadržaja. Nema javnog endpointa za contextual sadržaj.
- TTS, postojeće audio rute i ticket/visit/Neo4j poslovna logika nisu menjani. U visitor DTO mapiranju dodato je samo skrivanje image reference.

## Curator workflow i frontend

1. Sačuvati eksponat, pa na stranici izmene koristiti „Javna slika za katalog“. Izbor PNG/JPEG i „Sačuvaj sliku“ zamenjuju javni preview.
2. Cover izložbe dodaje se na `Prostori i program → Izložbe → Izmena`, nakon što izložba postoji.
3. Contextual slika: `Digitalni sadržaj → izbor učešća → Slika postavke`. Upload pravi novu `SLIKA` verziju; kartice prikazuju zaštićeni preview i omogućavaju postojeće brisanje reference. Za image upload nije potreban TTS ili tekst za sintezu.
4. Jasno je označeno da je javna slika javna, a contextual slika dostupna tek posle skeniranja. Greške i uspeh imaju nove centralne SR/EN ključeve.

Prikaz: javne kartice izložbi, kartice eksponata, detalj eksponata, detalj izložbe, curator editor/preview, aktivna poseta nakon scan-a i otključani dodatni sadržaj na detalju eksponata. Kartice imaju stabilan 4:3 okvir, detalji čuvaju odnos stranica, alt dolazi iz naziva eksponata/izložbe, a neuspešno učitavanje pokazuje neutralan placeholder. Privatne slike se učitavaju autorizovanim blob zahtevima; object URL se oslobađa pri promeni/sakrivanju komponente ili odjavi.

## Lokalni demo

`LocalImageSeeder`, order 30, radi samo u `local & !production & !prod`, posle postojećeg rich seed-a. Generiše originalne geometrijske ilustracije Java2D kodom; nema preuzetih ili tuđih slika i nema dodatnih binarnih fajlova u Git-u.

Dopunjava 56 rich eksponata, 12 rich izložbi i 96 postavki. Popunjava samo nedostajuće javne slotove i postavke bez slike. Koristi postojeći seed advisory lock, stabilne privatne demo reference i ne zamenjuje curator slike niti ostale podatke. Ponovni start ne pravi duplikate. Izgubljena demo datoteka se može ponovo generisati za postojeću demo referencu.

## Provere

Rezultati završnih komandi:

- Backend: **14/14 PASS** — `ImageStorageTests` (3), `ImageIntegrationTests` (1), `AudioIntegrationTests` (1), `RichDemoDataSeederTests` (9). PostgreSQL je izolovan Testcontainers primerak. Postavljen je privremeni nasumičan `JWT_SECRET` samo u procesu testiranja.
- Frontend: **74 različita testa PASS** u dve grupe: images/catalog/curator-audio/visitor-content/audio-guide **33/33**, zatim images/curator/collection/visit-lifecycle **45/45** (4 image testa ponovljena).
- `npx tsc --noEmit -p tsconfig.app.json`: **PASS**.
- `npm run build`: **PASS**, produkcioni artefakt u `frontend/dist/museum`. Postojeća upozorenja za neiskorišćene translation importe i `qrcode` CommonJS nisu blokirajuća.
- `git diff --check`: **PASS**.

Testovi pokrivaju stvarni PNG/JPEG upload, lažan MIME, SVG/pokvaren fajl, veličinu i dimenzije, traversal, zaštitu internih referenci, privatni pristup pre/posle scan-a i završetka, tuđu posetu, javni preview/skrivenu izložbu, placeholder, multipart curator flow, audio regresiju i seed idempotentnost.

Početni sandbox pokušaji imali su Maven network/permission i Angular `Access denied` greške; provere su zatim uspešno izvršene van sandboxa. Početni backend integracioni pokušaj bez `JWT_SECRET` nije mogao podići kontekst; ponovljeni pokušaji sa test ključem prolaze. Nisu menjane aplikacione zavisnosti ili podešavanja bezbednosti da bi se te prepreke zaobišle.

Za lokalni prikaz novih endpointa i demo slika restartovati backend sa postojećim `local` profilom. Baza razvojnog servera nije ručno resetovana niti seed-ovana tokom provera. Pre produkcionog puštanja obezbediti trajan image volume i backup; naknadno se može dodati održavanje napuštenih datoteka i generisanje zasebnih thumbnail rezolucija.

Tri neispravna Angular izraza iz prethodnog i18n rada minimalno su sintaksno popravljena u cashier/recommendations/audio šablonima jer su blokirala produkcioni build; ostali raniji prevodi i njihove nedovršenosti nisu deo ovog zadatka.
