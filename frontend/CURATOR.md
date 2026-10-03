# Kustoski modul

Implementacija koristi postojeće entitete i tabele. Nema promena EER-a, PostgreSQL šeme ili Flyway V1–V3. Nije rađen commit/push.

## Frontend

Sve /curator rute imaju curatorGuard i canActivateChild, proveravaju nalog preko /api/accounts/me i podržavaju obnovu sesije. Neprijavljeni korisnik ide na prijavu, a zabranjena uloga na /forbidden sa porukom 403. To je frontend stranica, dok stvarni API za nedozvoljene uloge vraća HTTP 403.

| Ruta | Funkcija i API |
| --- | --- |
| /curator | Broj eksponata, autora i svih učešća, uz dodatni broj aktivnih učešća: GET /api/exhibits, /api/authors, /api/exhibit-participations, /api/exhibitions |
| /curator/exhibits | Lista, pretraga, DELETE /api/exhibits/{id} uz potvrdu |
| /curator/exhibits/new | POST /api/exhibits |
| /curator/exhibits/:id/edit | GET /api/exhibits/{id}/management, PUT /api/exhibits/{id}; upravljanje autorstvom |
| /curator/authors | GET /api/authors, DELETE /api/authors/{id} uz potvrdu |
| /curator/authors/new | POST /api/authors |
| /curator/authors/:id/edit | GET i PUT /api/authors/{id} |
| /curator/participations | GET /api/exhibit-participations, eksponati i izložbe za nazive; DELETE /api/exhibit-participations/{id} uz potvrdu |
| /curator/participations/new | POST /api/exhibit-participations; izbor iz postojeće liste eksponata i izložbi |
| /curator/participations/:id/edit | GET i PUT /api/exhibit-participations/{id}; QR slika i kopiranje tokena |
| /curator/digital-content | Lista po učešću: GET /api/exhibit-participations/{id}/digital-content; DELETE /api/digital-content/{id} uz potvrdu |
| /curator/digital-content/new | Izbor učešća i POST /api/digital-content; posle uspeha otvara edit rutu |
| /curator/digital-content/:id/edit | Direktni GET i PUT /api/digital-content/{id}; učešće je zaključano |
| /curator/profile | Podaci sopstvenog naloga iz /api/accounts/me |

Kustos vidi Pregled, Eksponati, Autori, Postavke, Digitalni sadržaj, Profil i Odjava. Nakon prijave ide na /curator. Visitor i cashier tokovi zadržavaju svoje rute i menije. Kod naloga sa više uloga postojeća CASHIER prednost ostaje sačuvana.

## Minimalne backend dopune

| Ruta | Dozvola | Semantika |
| --- | --- | --- |
| GET /api/exhibit-participations | CURATOR, ADMINISTRATOR | Postojeći opcioni exhibitionId i dalje filtrira; bez parametra vraća sva učešća |
| GET /api/exhibits/{id}/authorships/management | CURATOR, ADMINISTRATOR | Veze sa authorId, exhibitId, contributionType, note |
| PUT /api/exhibits/{id}/authorships/{authorId} | CURATOR, ADMINISTRATOR | Idempotentno kreira ili menja vezu, telo sadrži contributionType i note |
| DELETE /api/exhibits/{id}/authorships/{authorId} | CURATOR, ADMINISTRATOR | Briše samo vezu; nedostajuća veza daje 404 |

Autorstvo ostaje određeno postojećim složenim ključem (authorId, exhibitId). Nema veštačkog ID-a. Postojeća zajednička security konfiguracija štiti nove putanje u oba profila; nije dodat izuzetak za local.

## Dozvole

| Uloga | Javni katalog API | Upravljački API | Kustoski frontend |
| --- | --- | --- | --- |
| Neprijavljen | GET dozvoljen | 401 | Prijava |
| VISITOR | GET dozvoljen | 403 | Zabranjen |
| CASHIER | GET ostaje javni | 403 | Zabranjen; sopstveni blagajnički meni |
| GUIDE | GET ostaje javni | 403 | Zabranjen |
| CURATOR | GET dozvoljen | Dozvoljen CRUD | Dozvoljen |
| ADMINISTRATOR | GET dozvoljen | Dozvoljen prema postojećim pravilima | Dozvoljen |

Javni DTO-i ostaju bez QR tokena, QR putanje, inventarskog broja i interne napomene autorstva. Visitor dobija sadržaj kroz postojeću aktivnu posetu, bez administrativnih kontrola.

## QR i digitalni sadržaj

Frontend ne šalje qrToken ni qrPath. Backend generiše UUID pri kreiranju i čuva isti token pri izmeni; baza garantuje jedinstvenost. QR polja su uklonjena iz ulaznog DTO-a i servis ih ne čita iz klijentskog zahteva. Dodatni JSON ključevi se ignorišu: ne mogu prepisati ID, QR identitet ili datum kreiranja sadržaja. Postojeća QR putanja ostaje sačuvana pri izmeni. QR sliku lokalno generiše već instalirani qrcode, bez spoljnog servisa. Kopiranje ima poruku uspeha i rezervno ručno kopiranje.

Izložba i eksponat postojećeg učešća se ne menjaju; novo povezivanje zahteva novo učešće. Digitalni sadržaj se ne premešta na drugo učešće. Brisanje povezanih zapisa može vratiti 409; nema kaskadnog brisanja istorije.

TEKST, SLIKA i AUDIO čuvaju putanju/referencu, jezik, pozitivnu celobrojnu verziju i oznaku automatskog generisanja. TEKST nema posebno polje za tekst: koristi se referenca na postojeći tekstualni resurs. Frontend i backend proveravaju oblik reference; nema upload-a, provere dostupnosti udaljenog servera niti preuzimanja resursa na backendu. Postojeći visitor prikaz referenci nije menjan. Model i šema ostaju isti.

## Demo nalog i ručna provera

Local seed idempotentno dodaje demo.curator@museum.local, sa lozinkom navedenom u zahtevu korisnika i konstantom DemoDataSeeder.PASSWORD. Lozinka se ne ispisuje u dokumentaciju ili logove; u bazi je BCrypt hash. Novi nalog ima samo CURATOR ulogu i Curator podtip. Ova dopuna se izvršava isključivo kada je aktivan profil local, ne samo development i ne u produkciji.

1. Pokrenuti novu backend verziju sa postojećom .env konfiguracijom i JWT_SECRET, profilom local. Pokrenuti npm start iz frontend; postojeći proxy očekuje port 8081.
2. Na /login prijaviti demo.curator@museum.local lozinkom iz dostavljenog zahteva. Očekuje se /curator i samo kustoski meni. Osvežiti stranicu: prijava i meni ostaju.
3. U Autorima kreirati test autora i izmeniti biografiju. U Eksponatima kreirati predmet sa jedinstvenim inventarskim brojem. Posle čuvanja otvara se edit ruta sa porukom uspeha.
4. Na edit strani eksponata povezati autora, uneti doprinos i internu napomenu, izmeniti vezu, pa je ukloniti. Autor i eksponat moraju ostati. Ponovo povezati radi provere 409 pri brisanju autora.
5. Kreirati učešće test eksponata na postojećoj izložbi. Uneti datume, poziciju, redosled i kontekst. Proveriti da ne postoji polje za ručni unos tokena. Nakon čuvanja proveriti QR sliku i Kopiraj; izmeniti kontekst i potvrditi da token ostaje isti.
6. U Digitalnom sadržaju odabrati upravo to učešće, kliknuti „Dodaj digitalni sadržaj“ i na /curator/digital-content/new uneti TEKST referencu demo/grad-001.txt, jezik sr, verziju 1. Sačuvati: otvara se /curator/digital-content/{id}/edit. Izmeniti verziju i sačuvati; učešće ostaje zaključano. Link „Nazad na sadržaj“ vraća listu izabranog učešća. AUDIO i SLIKA testirati sopstvenim dostupnim resursima; ne unositi proizvoljan nepostojeći URL kao dokaz da reprodukcija radi.
7. Probati brisanje učešća sa sadržajem: očekuje se razumljiva poruka konflikta. Ukloniti test sadržaj, zatim test učešće, autorstvo, eksponat i autora. Svako brisanje traži potvrdu. Ne brisati seed zapise ili istoriju poseta.
8. Odjaviti se i otvoriti /curator/exhibits: očekuje se prijava. Sa VISITOR, CASHIER i GUIDE nalozima očekuje se zabrana pristupa. ADMINISTRATOR ima postojeća prava upravljanja.
9. Anonimno i kao visitor proveriti javni katalog: QR/interna napomena nisu prisutni. Kupovina, validacija na blagajni, aktivna poseta, skeniranje i preporuke koriste postojeće tokove.
10. Proveriti dashboard, liste i forme na 320, 375, 390 i 430 px, tabletu 768 px i desktopu. Forme prelaze u jednu kolonu, liste su kartice, navigacija koristi postojeći mobilni meni. Proveriti fokus tastaturom, duge nazive i dugačke reference.

## Provere i preostala ograničenja

- Ceo backend skup: 104 testa, 0 grešaka/neuspeha; izolovani PostgreSQL sa postojećim migracijama i Hibernate validacijom. Uključuje javne DTO-e, zabranu uloga, CRUD, složeni ključ, roditeljske 404, konflikte, QR generisanje i stabilnost, digitalni sadržaj i stare tokove.
- Frontend: 45 testova u 4 fajla, svi prošli. Uključuju postojeće testove i nove provere guardova, navigacije, obnove sesije, duplog slanja, konflikta, brisanja, autorstva, referenci i direktnih ruta sadržaja. Završna TypeScript provera i production build prošli su 23.09.2026. Prošao je i git diff --check.
- Angular build ima postojeće CommonJS upozorenje za qrcode. Prvi sandbox pokušaj nije mogao pročitati roditeljske direktorijume; odobren build van sandboxa je prošao.
- Posle eksplicitnog odobrenja korisnika pokrenuti su local backend na 8082 (Flyway isključen, Hibernate validate) i privremeni frontend na 4201 sa proxyjem iz privremenog direktorijuma. Postojeći procesi na 8081/4200 nisu zaustavljani. Demo kustos je uspešno prijavljen; dashboard prikazuje 9 eksponata, 2 autora i 9 aktivnih učešća.
- U browseru su potvrđeni meni, refresh sesije, liste i edit forme, autorstva, profil, izbor sadržaja, čuvanje postojeće reference sadržaja, QR slika/read-only token, odjava, anonimno preusmeravanje na prijavu i zabrana kustoskih ruta za visitor i cashier. Tokeni nisu ispisivani niti snimani u screenshotovima.
- Vizuelno su pregledane mobilna forma eksponata, mobilni sadržaj i tablet forma učešća. Merene su stranice pregleda/listi/formi na 320, 390, 768 i 1440 px, a potpuno učitana forma učešća dodatno na 375 i 430 px: nema horizontalnog prelivanja, mobilna forma ima jednu kolonu. GUIDE i ADMINISTRATOR dozvole proverene su automatizovanim testovima.
- Automatska approval provera odbila je probno čuvanje postojećeg eksponata i autorstva u razvojnoj bazi. Ti upisi nisu ponavljani; njihov CRUD, kao i kreiranje/brisanje drugih zapisa, provereni su u izolovanoj test bazi i frontend HTTP testovima. Potpun ručni CRUD krug nad razvojnim zapisima ostaje opcioni korak iz prethodnog odeljka.
- Nema upload/storage infrastrukture, zbirnog broja digitalnog sadržaja ni detaljnog profila zaposlenog kroz API sopstvenog naloga. Dashboard prikazuje ukupan broj učešća i dodatno aktivna učešća po datumima postavljanja/uklanjanja i statusu/datumu izložbe, bez izmišljene analitike. API autora ne vraća listu povezanih eksponata; autorstvima se upravlja na edit strani izabranog eksponata, bez N+1 obilaska kataloga.
- Liste su namenjene sadašnjoj veličini kataloga; server nema paginaciju. Osvežavanje Neo4j projekcije koristi postojeći mehanizam; ovaj modul ga ne menja.

## Izmenjeni fajlovi

Backend:
- backend/src/main/java/rs/ac/uns/ftn/museum/catalog/controller/ExhibitCatalogController.java
- backend/src/main/java/rs/ac/uns/ftn/museum/catalog/dto/ExhibitDtos.java
- backend/src/main/java/rs/ac/uns/ftn/museum/catalog/service/ExhibitCatalogService.java
- backend/src/main/java/rs/ac/uns/ftn/museum/config/DemoDataSeeder.java
- backend/src/test/java/rs/ac/uns/ftn/museum/catalog/ExhibitCatalogIntegrationTests.java
- backend/src/test/java/rs/ac/uns/ftn/museum/journey/LocalDemoJourneyTests.java

Frontend:
- frontend/src/app/app.ts
- frontend/src/app/core/api.ts
- frontend/src/app/core/auth.ts
- frontend/src/app/core/curator.ts (nov)
- frontend/src/app/pages/auth-page.ts
- frontend/src/app/pages/curator-overview.ts (nov)
- frontend/src/app/pages/curator-list.ts (nov)
- frontend/src/app/pages/curator-editor.ts (nov)
- frontend/src/app/pages/curator-authorships.ts (nov)
- frontend/src/app/pages/curator-content.ts (nov)
- frontend/src/app/pages/curator.spec.ts (nov)
- frontend/src/app/shared/participation-qr.ts (nov)
- frontend/src/app/routes.ts
- frontend/src/styles.css
- frontend/CURATOR.md (ovaj dokument)
