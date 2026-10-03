# Nastavak projekta — 30.09.2026.

## Zatečeno stanje i granice rada

Read-only pregled obuhvatio je `git status`, `git diff --stat`, diff postojećih
izmena, poslednje commitove, kontrolere/servise/DTO-e, security, Angular rute,
guardove, navigaciju, forme, testove, seed i postojeće izveštaje. Nema AGENTS.md
u projektu. Pre izmena prikazan je checkpoint u razgovoru.

HEAD je `a380b73 Add responsive public digital catalog`; prethodni commitovi:
`b7e49a4` katalog API, `68826c6` skeniranje, `48d8fc1` local seed i Angular,
`455cce3` prodaja/posete/preporuke/Swagger, `20cf119` drugi podsistem.
Zatečeno je 16 izmenjenih praćenih i 17 nepraćenih fajlova. Katalog je već bio
implementiran; necommitovani Curator je imao funkcionalni CRUD i dokumentaciju.
Guide je već imao većinu implementacije, local seed i testove, ali nije imao
završni izveštaj, prikaz imena posetilaca ni zaštitu od zadržavanja prethodnog
zapisa pri promeni ID-ja u Angular ruti.

Sačuvane su sve zatečene izmene. Nisu menjani EER, odobrena specifikacija,
Flyway V1–V3, PostgreSQL šema niti entiteti. Nema commita/pusha, reset/checkout
komandi, brisanja razvojnih podataka ili uvođenja novih entiteta za ture.

## Konačni status modula

„Gotovo” označava funkcionalni obim pregledanog modula, ne produkcionu spremnost
celog projekta. Automatizovane provere navedene su zasebno ispod.

| Modul | Backend | Frontend | Security | Demo podaci | Testovi | Status |
|---|---|---|---|---|---|---|
| Visitor | Profil/interesovanja, karte, posete, ocene, scan, događaji | Povezane stranice i sopstvena prijava/otkazivanje | VISITOR, identitet iz JWT, vlasništvo posete/karte | Visitor i povezani katalog | Journey, local journey, auth, scan, events | Gotovo za postojeće tokove |
| Cashier | Prodaja i validacija karata | Validacija postoji; prodajna forma nedostaje | CASHIER; prednost u višestrukim ulogama | Staff CASHIER+GUIDE | Journey i auth regresije | Delimično kao celovit modul; postojeći tok očuvan |
| Digitalni katalog | Javni eksponati/autori/autorstva/učešća | /exhibits, /exhibits/:id, /authors, /authors/:id; filteri, stanja i responsive CSS | Javni GET; DTO bez qrToken/qrPath i internih polja | 9 eksponata, 2 autora, 3 izložbe u local seedu | Catalog i ExhibitCatalog u oba profila; frontend catalog | Gotovo |
| Curator | Eksponati, autori, autorstva, učešća, serverski UUID QR, digitalni sadržaj | Dashboard, CRUD, profil, QR; ispravljena promena ID-ja forme | CURATOR ili ADMINISTRATOR; parent/child guards i API zaštita | Local curator sa podtipom | CRUD, QR stabilnost, zabrane, konflikti, frontend curator | Gotovo za traženi obim |
| Guide i događaji | CRUD, kapacitet/zaključavanje, prijave/statusi, sopstveni profil, imena prijavljenih | Guide meni/dashboard/liste/forme/prijave/profil; visitor događaji i otkazivanje; responsive CSS | GUIDE upravlja; VISITOR menja samo sopstvenu prijavu; privatna lista | Local Guide+Employee+podtip i 3 dodatna događaja | People integracija/konkurentnost, local seed, frontend events | Gotovo za traženi obim |
| Manager analitika | Samo Employee podtip; nema analitičkih endpointa | Nedostaje | Nema posebnih analitičkih pravila | Nema demo naloga | Testiran podtip, ne analitika | Nedostaje |
| Administrator | Nalozi/uloge/zaposleni, muzeji/sale i dozvole kataloga | Nema zasebnog administratorskog modula; može u Curator | ADMINISTRATOR API pravila | Nema demo naloga | Backend administrativni CRUD i role matrix | Delimično |
| Neo4j preporuke | Projekcija sadržaja, skorovi, preporuke i rute; fallback | Visitor preporuke i rute posete | VISITOR/vlasništvo; graf bez ličnih podataka | Katalog i scan podaci | GraphProjection i Journey | Gotovo za postojeći obim |

## Dovršeno u ovom radu

- Popravljen Windows Maven Wrapper: null/prazan `Target` lokalnog Maven keša
  više ne izaziva grešku pre kompilacije. Verzija Maven distribucije nije menjana.
- Zaštićeni GET prijava sada vraća `ManagedRegistrationResponse` sa imenom i
  prezimenom posetioca. Visitor odgovor ostaje isti. Nema emaila, telefona ili
  hash-a lozinke. EntityGraph učitava posetioca sa prijavama.
- Guide ekran prikazuje ime, prezime, ID i status; promena statusa čuva ime
  dok se podaci ponovo učitavaju.
- `RecordRouteReuseStrategy` ponovo kreira samo označene detalje/forme kada se
  promeni ID. Sprečava da URL drugog događaja/autora/eksponata/učešća/sadržaja
  nastavi sa starom formom ili potvrdom. Ostale rute koriste postojeće ponašanje.
- Dodati regresioni testovi promene URL-a tokom zahteva, čuvanja drugog događaja
  i autora, prikaza imena i privatnosti backend odgovora.
- Neispravan Guide edit ID prikazuje grešku i ne može kreirati novi događaj.
- Ažurirani demo nalozi u dokumentaciji i zastareo README status.

## Rute i dozvole

| Frontend | Namena / API |
|---|---|
| /exhibits, /exhibits/:id | GET /api/exhibits, /api/exhibits/{id}, /api/exhibits/{id}/participations |
| /authors, /authors/:id | GET /api/authors, /api/authors/{id} |
| /curator, /curator/profile | Stvarni brojevi kataloga; GET /api/accounts/me |
| /curator/exhibits[/new ili /:id/edit] | GET/POST /api/exhibits; PUT/DELETE /api/exhibits/{id}; GET /api/exhibits/{id}/management |
| /curator/authors[/new ili /:id/edit] | GET/POST /api/authors; GET/PUT/DELETE /api/authors/{id} |
| Autorstva na edit strani eksponata | GET /api/exhibits/{id}/authorships/management; PUT/DELETE /api/exhibits/{id}/authorships/{authorId} |
| /curator/participations[/new ili /:id/edit] | GET/POST /api/exhibit-participations; GET/PUT/DELETE /api/exhibit-participations/{id} |
| /curator/digital-content[/new ili /:id/edit] | GET /api/exhibit-participations/{id}/digital-content; POST /api/digital-content; GET/PUT/DELETE /api/digital-content/{id} |
| /guide, /guide/profile | GET /api/events i /api/guides/me; dashboard koristi stvarne brojeve |
| /guide/events, /guide/events/new, /guide/events/:id/edit | GET/POST /api/events; GET/PUT/DELETE /api/events/{id} |
| /guide/registrations, /guide/events/:id/registrations | GET /api/events/{id}/registrations; PUT /api/events/{id}/registrations/{visitorId} |
| /events, /events/:id | Javni GET /api/events i /api/events/{id}; GET /api/visitors/me/registrations samo za visitor |
| Prijava/otkazivanje na /events/:id | POST /api/events/{id}/registrations; DELETE /api/events/{id}/registrations/me |
| /cashier | POST /api/cashier/validate; GET /api/tickets/{id} |
| /tickets, /visits, /visits/:id, /scan, /recommendations, /profile | Postojeći /api/tickets, /api/visits, /api/recommendations/exhibitions, /api/visitors/me tokovi |

| Uloga | Javni GET katalog/događaji | Curator API/UI | Guide API/UI | Visitor prijave |
|---|---|---|---|---|
| Anonimni | Da | API 401; UI login | API 401; UI login | 401 |
| VISITOR | Da | 403 | 403 | Sopstvene |
| CASHIER | Da | 403 | 403 za nalog bez GUIDE | 403 |
| CURATOR | Da | Da | 403 | 403 |
| GUIDE | Da | 403 | Da | 403 |
| ADMINISTRATOR | Da | Da | 403 bez GUIDE | 403 |
| MANAGER | Da | 403 | 403 | 403 |

Backend proverava sve stvarne uloge naloga; staff sa CASHIER+GUIDE zato ima oba
API prava. Frontend zadržava prioritet CASHIER, zatim CURATOR/ADMINISTRATOR,
zatim GUIDE, zatim VISITOR. Postojeći visitorGuard usmerava zaposlene sa javnih
visitor ekrana u njihov radni prostor; anonimni i visitor imaju javni katalog.
Svi vodiči mogu upravljati događajima prema postojećoj politici; nije uvedeno
novo ograničenje vlasništva ili muzeja.

Kapacitet štite pesimističko zaključavanje događaja, složeni ključ prijave i
postojeći DB trigger. Aktivna duplikat prijava daje 409; otkazana se može ponovo
aktivirati samo dok je događaj otvoren i ima mesta. Početak događaja zatvara
prijave. Statusi su PLANIRAN/POPUNJEN/ZAVRSEN/OTKAZAN i
PRIJAVLJEN/PRISUSTVOVAO/OTKAZAN. Visitor ne može otkazati evidentirano prisustvo.
Brisanje događaja sa istorijom prijava daje 409 zbog RESTRICT veze; UI nudi
promenu statusa u Otkazan. UI prevodi 400/401/403/404/409 u razumljive poruke,
bez ispisivanja neobrađenog serverskog sadržaja.

## Demo nalozi

Local seed i testovi koriste sledeće naloge; lozinka je `MuseumDemo!2026`.
U PostgreSQL-u se čuva BCrypt hash.

| Nalog | Uloge / zapis |
|---|---|
| demo.visitor@museum.local | VISITOR + Visitor |
| demo.staff@museum.local | CASHIER i GUIDE; postojeći Employee/Guide i prioritet blagajne |
| demo.curator@museum.local | CURATOR + Employee/Curator; samo local |
| demo.guide@museum.local | GUIDE + Employee/Guide; samo local |

Sva četiri naloga potvrđuje seed u izolovanoj test bazi. Read-only SQL provera
postojeće razvojne baze potvrdila je visitor, staff i curator naloge, njihove
uloge i BCrypt hash format; demo.guide još nije prisutan u toj bazi. Pokretanje
nove verzije sa local profilom idempotentno ga kreira zajedno sa Guide podtipom
i događajima. Seed ne resetuje lozinku postojećeg naloga. Nema demo
Manager/Administrator naloga.

## Provere

Prvi puni `mvnw.cmd clean test` uz Java 21 i generisan privremeni JWT_SECRET:
110 testova, 0 failures/errors/skipped, BUILD SUCCESS. Pošto su poslednje backend
dopune nastale tokom tog izvršenja, pokrenut je i završni puni skup.
Završni `mvnw.cmd clean test`: **110/110, 0 failures, 0 errors, 0 skipped,
BUILD SUCCESS**, 7 min 4 s; završen 30.09.2026. u 13:25 CEST.
PostgreSQL 17.9 je u izolovanom Testcontainers okruženju primenio neizmenjene
V1–V3 i prošao Hibernate validate. GraphProjection koristi stvarni Neo4j 5.26
kontejner. Local seed je testiran za idempotentnost, uloge i podtipove.

| Backend skup | Testova | Rezultat |
|---|---:|---|
| CatalogIntegration / CatalogService | 10 + 10 | Svi prošli |
| ExhibitCatalog / LocalExhibitCatalog | 16 + 16 | Svi prošli |
| HealthController | 2 | Svi prošli |
| GraphProjection | 3 | Svi prošli |
| JourneyIntegration / LocalDemoJourney | 16 + 4 | Svi prošli |
| MuseumBackendApplication / OpenApi | 1 + 2 | Svi prošli |
| PeopleIntegration / PeopleService | 23 + 7 | Svi prošli |

Lokalni ignorisani logovi: backend-final-verification.log, frontend-tests.log,
frontend-build.log i schema-validation.log. Privremeni JWT secret generisan je
za proces; nije upisan u izvorni kod niti dokumentaciju.
TypeScript `--noEmit -p tsconfig.app.json`: prošao.
Frontend `npm test`: 68/68, pet fajlova, bez neuspeha.
Angular production build: prošao; postojeće CommonJS upozorenje za `qrcode`.
Prvi Angular pokušaj blokirao je sandbox pristup roditeljskim direktorijumima;
isti postupak sa proširenim dozvolama je prošao.

Postojeća razvojna PostgreSQL šema: Hibernate `ddl-auto=validate` prošao uz
`spring.flyway.enabled=false`, bez local/development profila i bez seeda,
sa isključenim zakazanim poslovima. Privremeni backend na 127.0.0.1:8082
uspešno se pokrenuo i `/api/health` vratio UP. Zaustavljen je samo taj Java
proces. SQL provera potvrđuje V1/V2/V3 sa success=true. Prvi startup pokušaj
bio je prerano pokrenut dok PostgreSQL još nije primao konekcije; ponovljeni
pokušaj posle spremnosti baze je uspeo. Projektni PostgreSQL ostavljen je
pokrenut; Docker Desktop je pokrenut za integracione testove.

`git diff --check` je prošao. Git ne prati .env, target, node_modules ni logove;
`.env.example` je očekivani šablon. Provera tipičnih privatnih ključeva/API
tokena u praćenim fajlovima nije našla poklapanja; ovo nije iscrpan secret audit.
`git diff` za migracije, EER i specifikaciju je prazan.

Responsive raspored postoji u CSS-u (jedna kolona na malim ekranima, prelom
dugmadi i mobilni meni). Ovaj nastavak nije izvršio novu vizuelnu browser proveru;
istorijske provere kataloga i Curator ekrana nalaze se u frontend dokumentaciji.
To nije dokaz vizuelne provere novih Guide ekrana.

## Ručno testiranje

1. Pokrenuti Docker i postojeće projektne baze prema README-u. Java 21,
   postojeća lokalna konfiguracija i JWT_SECRET; iz backend direktorijuma
   `mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local`. Za već formiranu
   razvojnu šemu može se dodati `-Dspring-boot.run.arguments=--spring.flyway.enabled=false`;
   Hibernate ostaje `ddl-auto=validate`. Iz frontend pokrenuti `npm start`.
2. Anonimno otvoriti localhost:4200/exhibits, detalj, /authors i detalj autora.
   Proveriti filtere, prazne rezultate, nepostojeći ID i odsustvo QR polja u JSON-u.
3. Prijaviti demo.curator; očekivati /curator. Kreirati test autora i eksponat,
   povezati autorstvo, izmeniti i ukloniti vezu. Kreirati učešće na postojećoj
   izložbi; proveriti serverski QR i stabilnost posle izmene konteksta.
4. Dodati TEKST referencu demo/grad-001.txt u novo učešće, izmeniti verziju.
   Brisanje učešća sa sadržajem treba da daje 409. Ukloniti samo sopstvene test
   zapise obrnutim redom. Ne brisati seed ili istoriju poseta.
5. Prijaviti demo.guide; očekivati /guide i Pregled, Događaji, Prijave, Profil,
   Odjava. Osvežiti stranicu. Kreirati budući događaj kapaciteta 1 na izložbi,
   izabrati sebe kao vodiča. Izmeniti naziv i proveriti listu/dashboard.
6. Kao demo.visitor otvoriti /events, odabrati događaj i prijaviti se. Očekivati
   PRIJAVLJEN i 0 slobodnih mesta. Pokušaj drugog visitora daje 409; ponovni POST
   prvog visitora takođe 409. Konkurentnu trku poslednjeg mesta proverava test.
7. Visitor otkazuje uz potvrdu; mesto se oslobađa. Ponoviti prijavu. Vodič u
   Prijavama vidi ime/prezime i menja status u PRISUSTVOVAO. Visitor više nema
   otkazivanje. Otkazan/završen/već počeo događaj ne dozvoljava novu prijavu.
8. Vodič pokušava brisanje događaja sa prijavama: 409; događaj ostaje. Kreirati
   drugi događaj bez prijava i obrisati ga uz potvrdu. Ne smanjuje se kapacitet
   ispod zauzetih mesta. Neispravni datumi daju poruku validacije.
9. Preći između dve edit adrese događaja/autora i proveriti novi naziv i ID
   pre čuvanja. Back/forward i direktno osvežavanje moraju učitati odgovarajući zapis.
10. Kao visitor/cashier/curator direktno otvoriti /guide/events: zabrana.
    Bez prijave otvaranje /guide vodi na login. Direktni API write bez tokena
    daje 401, a sa nedozvoljenom ulogom 403. Visitor ne dobija listu drugih prijava.
11. Regresija: visitor kupuje kartu, staff je validira, visitor otvara aktivnu
    posetu, skenira pripadajući QR, otvara sadržaj, preporuke i preporučenu rutu.
12. Na 320, 375, 390, 768 i 1440 px proveriti Guide listu/formu/prijave i visitor
    detalj: bez horizontalnog skrola, čitljivi nazivi, mobilni meni, fokus tastature.
    Za loading/error isključiti mrežu i zatim koristiti Osveži/Pokušaj ponovo.

## Preostalo za ceo projekat

- Manager analitika: API, agregacije/izveštaji, prava i frontend prema specifikaciji.
- Administrator frontend i namenski demo nalog ako bude zatražen.
- Cashier frontend prodaje (backend već postoji), ukoliko se zatvara ceo obim uloge.
- Nova vizuelna i ručna end-to-end provera Guide ekrana na stvarnom local serveru.
- Produkciona naplata/storage/deployment nisu obuhvaćeni demo implementacijom.
  Digitalni sadržaj trenutno čuva reference; nema upload infrastrukture.
- Liste nemaju serversku paginaciju. Dalje proširenje obima uskladiti sa odobrenom
  specifikacijom; ovaj rad je nije menjao niti pretpostavio nove entitete.

## Fajlovi ovog nastavka

Pored sačuvanih ranijih izmena, ovaj nastavak menja/dodaje:

- backend/mvnw.cmd
- backend/src/main/java/rs/ac/uns/ftn/museum/people/controller/EventController.java
- backend/src/main/java/rs/ac/uns/ftn/museum/people/dto/ManagedRegistrationResponse.java (nov)
- backend/src/main/java/rs/ac/uns/ftn/museum/people/repository/EventRegistrationRepository.java
- backend/src/main/java/rs/ac/uns/ftn/museum/people/service/EventService.java
- backend/src/test/java/rs/ac/uns/ftn/museum/people/PeopleIntegrationTests.java
- frontend/src/main.ts
- frontend/src/app/core/route-reuse.ts (nov)
- frontend/src/app/core/events.ts
- frontend/src/app/pages/guide-registrations.ts
- frontend/src/app/pages/guide-event-editor.ts
- frontend/src/app/pages/events.spec.ts
- frontend/src/app/pages/curator.spec.ts
- frontend/src/app/routes.ts
- database/demo-data-local.md
- README.md
- docs/continuation-2026-09-30.md (ovaj izveštaj)

Trenutni status: 22 izmenjena praćena i 20 nepraćenih fajlova; ništa nije staged.

```text
 M README.md
 M backend/mvnw.cmd
 M backend/src/main/java/rs/ac/uns/ftn/museum/catalog/controller/ExhibitCatalogController.java
 M backend/src/main/java/rs/ac/uns/ftn/museum/catalog/dto/ExhibitDtos.java
 M backend/src/main/java/rs/ac/uns/ftn/museum/catalog/service/ExhibitCatalogService.java
 M backend/src/main/java/rs/ac/uns/ftn/museum/config/DemoDataSeeder.java
 M backend/src/main/java/rs/ac/uns/ftn/museum/people/controller/EventController.java
 M backend/src/main/java/rs/ac/uns/ftn/museum/people/repository/EventRegistrationRepository.java
 M backend/src/main/java/rs/ac/uns/ftn/museum/people/repository/GuideRepository.java
 M backend/src/main/java/rs/ac/uns/ftn/museum/people/security/ApiSecurity.java
 M backend/src/main/java/rs/ac/uns/ftn/museum/people/service/EventService.java
 M backend/src/test/java/rs/ac/uns/ftn/museum/catalog/ExhibitCatalogIntegrationTests.java
 M backend/src/test/java/rs/ac/uns/ftn/museum/journey/LocalDemoJourneyTests.java
 M backend/src/test/java/rs/ac/uns/ftn/museum/people/PeopleIntegrationTests.java
 M database/demo-data-local.md
 M frontend/src/app/app.ts
 M frontend/src/app/core/api.ts
 M frontend/src/app/core/auth.ts
 M frontend/src/app/pages/auth-page.ts
 M frontend/src/app/routes.ts
 M frontend/src/main.ts
 M frontend/src/styles.css
?? backend/src/main/java/rs/ac/uns/ftn/museum/people/controller/GuideController.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/people/dto/ManagedRegistrationResponse.java
?? docs/continuation-2026-09-30.md
?? frontend/CURATOR.md
?? frontend/src/app/core/curator.ts
?? frontend/src/app/core/events.ts
?? frontend/src/app/core/route-reuse.ts
?? frontend/src/app/pages/curator-authorships.ts
?? frontend/src/app/pages/curator-content.ts
?? frontend/src/app/pages/curator-editor.ts
?? frontend/src/app/pages/curator-list.ts
?? frontend/src/app/pages/curator-overview.ts
?? frontend/src/app/pages/curator.spec.ts
?? frontend/src/app/pages/event-detail.ts
?? frontend/src/app/pages/events.spec.ts
?? frontend/src/app/pages/events.ts
?? frontend/src/app/pages/guide-event-editor.ts
?? frontend/src/app/pages/guide-overview.ts
?? frontend/src/app/pages/guide-registrations.ts
?? frontend/src/app/shared/participation-qr.ts
```
