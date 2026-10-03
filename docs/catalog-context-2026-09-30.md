# Katalog i kontekst postavki — 30.09.2026.

## Obim

Javni ExhibitResponse sada sadrži `exhibitions` sa isključivo `id` i `name`, izvedeno iz postojećih javno vidljivih učešća. Više učešća na istoj izložbi ne duplira naziv na kartici. Kartica prikazuje najviše tri naziva, link ka svim izložbama na detalju i poruku kada nema javnih izložbi. Dugački nazivi se prelamaju.

Postojeći detalj već prikazuje javne kontekste u odvojenim blokovima i VisitorContent sa statusom svake postavke. Njegova implementacija, journey servisi i security nisu menjani ovim zadatkom. Postojeće javne kontekstualne opise i dalje vraća javni participation endpoint; pristup dodatnom digitalnom sadržaju i dalje ide kroz scan/unlock.

Seed obogaćuje kontekste tri gradske postavke, uz poređenje sa originalnim seed tekstom kako se ne bi pregazile izmene kustosa. Dodaje drugo učešće postojećeg eksponata Jutro na trgu na Tragovima svetlosti. Eksponat ostaje jedan zapis. TEKST reference i datoteke pripadaju pojedinačnim postavkama; nema AUDIO/TTS izmena.

## Fajlovi izmenjeni ovim zadatkom

- backend/src/main/java/rs/ac/uns/ftn/museum/catalog/dto/ExhibitDtos.java
- backend/src/main/java/rs/ac/uns/ftn/museum/catalog/service/ExhibitCatalogService.java
- backend/src/main/java/rs/ac/uns/ftn/museum/config/DemoDataSeeder.java
- backend/src/main/java/rs/ac/uns/ftn/museum/journey/controller/LocalDemoQrController.java — samo novi token na postojećoj listi lokalnih demo oznaka.
- backend/src/test/java/rs/ac/uns/ftn/museum/catalog/ExhibitCatalogIntegrationTests.java
- backend/src/test/java/rs/ac/uns/ftn/museum/catalog/LocalExhibitCatalogIntegrationTests.java
- backend/src/test/java/rs/ac/uns/ftn/museum/journey/LocalDemoJourneyTests.java — samo tri očekivana broja: 10 ukupno, 4 na Tragovima svetlosti i broj preostalih stavki rute; postojeće scan/unlock provere ostaju iste.
- frontend/src/app/core/catalog.ts
- frontend/src/app/pages/exhibits.ts
- frontend/src/app/pages/catalog.spec.ts
- frontend/src/styles.css
- frontend/public/demo/grad-001.txt
- frontend/public/demo/grad-002.txt
- frontend/public/demo/grad-003.txt
- frontend/public/demo/jutro-tragovi-svetlosti.txt (nov)
- docs/catalog-context-2026-09-30.md (ovaj izveštaj)

## Demo postavke i lokalni QR tokeni

| Eksponat | Izložba | Local demo token | TEKST referenca |
|---|---|---|---|
| Jutro na trgu | Grad kroz objektiv | DEMO-CITY-QR-001 | demo/grad-001.txt |
| Jutro na trgu | Tragovi svetlosti | DEMO-LIGHT-CITY-QR-001 | demo/jutro-tragovi-svetlosti.txt |
| Odraz na Dunavu | Grad kroz objektiv | DEMO-CITY-QR-002 | demo/grad-002.txt |
| Noćna svetla | Grad kroz objektiv | DEMO-CITY-QR-003 | demo/grad-003.txt |

Tokeni su namerno predvidljivi isključivo za postojeći local/development demo seed. Nisu deo javnog katalog DTO-a. Demo tekstualni fajlovi koriste postojeći frontend/public/demo mehanizam; nisu produkciono zaštićeno skladište.

## Ručna provera

1. Pokrenuti postojeće baze i backend sa Java 21, postojećom lokalnom konfiguracijom i JWT_SECRET: iz backend direktorijuma `./mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local`. Za postojeću razvojnu bazu kojoj je ranije bio potreban Flyway override zadržati postojeći argument `-Dspring-boot.run.arguments=--spring.flyway.enabled=false`; ne menjati migracije niti šemu. Iz frontend direktorijuma pokrenuti `npm start`.
2. Anonimno otvoriti http://localhost:4200/exhibits i pretražiti Jutro na trgu. Kartica mora prikazati Grad kroz objektiv i Tragovi svetlosti. Otvoriti detalj i proveriti dva različita konteksta pod odgovarajućim nazivima izložbi.
3. U Network panelu proveriti GET /api/exhibits i GET /api/exhibits/{id}: exhibitions sadrži samo id/name, bez qrToken, qrPath, inventarskog broja ili management podataka.
4. Kao demo.visitor@museum.local kupiti kartu za trenutno otvoreni termin izložbe Grad kroz objektiv. Kao demo.staff@museum.local na /cashier validirati QR token te KARTE i termin. Oba naloga koriste MuseumDemo!2026, osim ako je lozinka ranije ručno izmenjena.
5. Ponovo kao visitor otvoriti detalj Jutra na trgu. U Dodatnom sadržaju gradska postavka traži skeniranje. Otvoriti odgovarajuću aktivnu posetu na /visits, uneti DEMO-CITY-QR-001 i skenirati. Vratiti se na detalj, po potrebi kliknuti Osveži status skeniranja. Gradski TEKST se otključava; druga postavka se time ne otključava.
6. Ponoviti kupovinu i validaciju za Tragove svetlosti. Pre skeniranja druga postavka traži QR. U njenoj aktivnoj poseti skenirati DEMO-LIGHT-CITY-QR-001. Na istom detalju proveriti odvojene kontekste i različite TEKST linkove. Ako su obe posete aktivne, oba otključana bloka su vidljiva; sadržaj završene posete prati postojeću politiku pristupa.
7. Na gradskoj poseti skenirati i DEMO-CITY-QR-002 / DEMO-CITY-QR-003 i otvoriti odgovarajuće tekstove.
8. Restartovati backend sa istim profilom: druga postavka i TEKST sadržaj ne smeju se duplirati. Seed ne prepisuje ručne kontekstualne izmene kustosa.
9. Na širinama 320, 375, 768 i 1440 px proveriti prelom naziva i kartice. Empty state i ograničenje na tri naziva imaju automatizovane frontend provere; demo nema namenski prazan eksponat niti eksponat sa više od tri izložbe.

EER, entiteti, Flyway V1–V3 i PostgreSQL šema nisu menjani. Nema commita ni pusha. Razvojna baza nije zasebno menjana alatima u ovom radu; nove podatke primenjuje sledeće pokretanje local seeda.

## Rezultati provera

- ExhibitCatalogIntegrationTests: 17/17 prošlo.
- LocalExhibitCatalogIntegrationTests: 18/18 prošlo, uključujući idempotentnost i očuvanje ručnih kontekstualnih izmena.
- JourneyIntegrationTests: 17/17 prošlo; fajl i postojeći scan/unlock testovi nisu menjani ovim zadatkom.
- LocalDemoJourneyTests: završni ponovljeni skup 4/4 prošao. Prvi skup je otkrio da fiksna local QR lista još nije sadržala novi token; nakon dopune liste provera prolazi. U ovom testu menjani su samo očekivani brojevi zbog dodatog učešća.
- Frontend catalog.spec.ts + visitor-content.spec.ts: 23/23 prošlo.
- TypeScript `node node_modules/typescript/bin/tsc --noEmit -p tsconfig.app.json`: prošlo.
- `git diff --check`: prošlo.

Ukupno 56 relevantnih backend testova i 23 frontend testa prolazi kroz navedena izvršenja. Početne sandbox prepreke Maven mreži/kešu i Angular čitanju direktorijuma rešene su izvršavanjem istih provera sa proširenim dozvolama. Nije pokretan puni test skup niti nova vizuelna browser provera.
