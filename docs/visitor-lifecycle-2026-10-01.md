# Visitor životni ciklus — 01.10.2026.

## Postojeći tok

Backend je već imao završetak posete, vreme izlaska, zatvaranje ruta i status iskorišćene ulaznice, istoriju sopstvenih poseta i ocenu vezanu za konkretnu posetu. Ručni završetak je idempotentan. JourneyMaintenance automatski zatvara istekle posete. Postojeći rate servis takođe može zatvoriti isteklu aktivnu posetu pre čuvanja ocene; ocena se čuva tek nad završenom posetom. Ova politika nije menjana.

Ocena je 1–5, komentar je opcioni, datum upisuje server, a druga ocena iste posete daje 409. `/api/visits/**` zahteva VISITOR ulogu; servis proverava vlasnika iz JWT identiteta. CASHIER/GUIDE/CURATOR bez VISITOR uloge nemaju pristup privatnoj istoriji i ocenjivanju.

FutureExhibitionService već koristi interesovanja i poslednji skenirani eksponat, uključujući skeniranja završene posete, samo uz saglasnost. Bez saglasnosti ne poziva graf niti čita lična interesovanja/istoriju za rangiranje. GraphProjection ima vremensko ograničenje i vraća prazne skorove pri nedostupnom Neo4j-u, a servis tada koristi opšti PostgreSQL redosled. Frontend ima timeout i link ka javnim izložbama. Nije uveden novi algoritam; ocene nisu dodate kao novi signal rangiranja.

## Dopune

- Aktivna poseta ima dugme Završi posetu i potvrdu. Uspešan završetak uklanja scan kontrole i prethodno prikazani digitalni sadržaj, čuva istorijski prikaz i otvara ocenjivanje.
- Lista i detalj prikazuju naziv izložbe, status i vremena ulaska/izlaska. VisitResponse dobija exhibitionId/exhibitionName izvedene iz postojeće veze Poseta → Ulaznica → Termin → Izložba.
- Završeni detalj koristi već dostupna imena iz scan odgovora. Ne traži pristup digitalnom sadržaju završene posete.
- Ocena ima zasebno učitavanje, prazno stanje, formu 1–5 i komentar, uspeh i bezbedne poruke greške. Sačuvana ocena zamenjuje formu; konflikt ili neizvestan odgovor zahteva GET proveru pre novog slanja.
- Istorijske kartice prikazuju postojeću ocenu i vode na detalj skeniranih eksponata. Promena ID-a detalja koristi postojeću strategiju ponovnog kreiranja komponente da se ne prenese ocena/formular prethodne posete.

## Postojeće API rute

- GET /api/visits/me
- GET /api/visits/{id}
- POST /api/visits/{id}/finish
- GET /api/visits/{id}/scans
- GET /api/visits/{id}/routes
- GET i POST /api/visits/{id}/rating
- GET /api/recommendations/exhibitions

Nijedna nova ruta nije dodata. EER, entiteti, Flyway V1–V3 i šema nisu menjani. Cashier, Curator i Guide frontend nisu menjani. Scan/content backend i preporučivački algoritam nisu menjani. Nema commita/pusha.

## Fajlovi ovog koraka

- backend/src/main/java/rs/ac/uns/ftn/museum/journey/dto/JourneyDtos.java
- backend/src/main/java/rs/ac/uns/ftn/museum/journey/mapper/JourneyMapper.java
- backend/src/test/java/rs/ac/uns/ftn/museum/journey/JourneyIntegrationTests.java
- frontend/src/app/core/models.ts
- frontend/src/app/pages/visits.ts
- frontend/src/app/pages/visit-detail.ts
- frontend/src/app/shared/visit-rating.ts (nov)
- frontend/src/app/pages/visit-lifecycle.spec.ts (nov)
- frontend/src/app/routes.ts
- frontend/src/styles.css
- docs/visitor-lifecycle-2026-10-01.md (ovaj dokument)

## Ručno testiranje

1. Pokrenuti postojeći backend sa local profilom i frontend. Koristiti postojeću konfiguraciju baze, Java 21 i JWT_SECRET. Iz backend direktorijuma: `./mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local`; za postojeću razvojnu šemu zadržati ranije korišćeni Flyway override ako je potreban. Iz frontend direktorijuma: `npm start`.
2. Prijaviti se kao demo.visitor@museum.local, lozinka MuseumDemo!2026 (ako nije ručno promenjena). Otvoriti /visits. Može se koristiti njegova postojeća aktivna poseta; ID zavisi od lokalne baze i nije fiksiran seed-om.
3. Ako nema aktivne posete, kao visitor kupiti kartu za trenutno otvoreni termin izložbe Grad kroz objektiv, kategorija ODRASLI (seed cena 7.00) ili STUDENT (4.00). Koristiti termin koji je sada u toku. Local seed pri pokretanju obezbeđuje takav termin. Karta mora biti kupljena visitor nalogom, jer anonimna blagajnička karta nema visitor vlasnika.
4. Kao demo.staff@museum.local, ista demo lozinka, u postojećoj /cashier validaciji uneti token kupljene KARTE i njen ID termina. Vratiti se na demo visitor nalog i otvoriti nastalu posetu na /visits.
5. Skenirati local postavku DEMO-CITY-QR-001 (Jutro na trgu). Kliknuti Završi posetu, zatim Potvrdi završetak. Očekivati ZAVRSENA, vreme izlaska, istorijski naziv skeniranog eksponata i formu ocene. Nova skeniranja i digitalni sadržaj završene posete nisu dostupni.
6. Izabrati ocenu 1–5, uneti komentar i sačuvati. Prikazuju se ocena, komentar i datum; forma nestaje. Osvežiti stranicu: ocena ostaje. Na /visits kartica ostaje u istoriji i prikazuje ocenu.
7. Za još neocenjenu završenu posetu probati praznu ocenu; slanje je onemogućeno. Aktivna poseta nema formu za ocenu. Ownership, uloge, vrednosti van opsega i dupli POST proveravaju backend testovi.
8. Otvoriti /recommendations. Sa saglasnošću na /profile postojeći mehanizam može koristiti prethodno skeniranje. Bez saglasnosti očekivati opšti pregled. Nedostupan Neo4j daje opšti pregled; nedostupan backend daje završeno loading stanje, poruku i link ka javnim izložbama.
9. Proveriti širine 320, 375, 768 i 1440 px: nazivi i komentari se prelamaju, textarea ostaje unutar panela, dugmad za završetak se prelamaju. Browser vizuelna provera nije automatizovana ovim korakom.

Razvojna baza nije zasebno menjana radi pravljenja gotove demo posete; koristi se postojeći nalog i gore opisani tok.

## Rezultati provera

- Backend: JourneyIntegrationTests (19 testova) i RecommendationAvailabilityTests (3 testa). Prvo izvršenje: 21 prošao, novi lifecycle test pao zbog poređenja nanosekundnog Java vremena sa PostgreSQL mikrosekundnim vremenom. Posle ispravke poređenja ponovljen je samo taj test: 1/1, BUILD SUCCESS. Sva 22 izabrana testa imaju uspešan rezultat; ostatak skupa nije nepotrebno ponavljan.
- Frontend: visit-lifecycle.spec.ts, recommendation-availability.spec.ts i visitor-content.spec.ts: završno 20/20, tri fajla prošla. Prva dva pada novih testova ispravljena su simulacijom stvarnog DOM unosa umesto direktne izmene običnog svojstva komponente.
- `node node_modules/typescript/bin/tsc --noEmit -p tsconfig.app.json`: prošao nakon frontend testova.
- `git diff --check`: prošao.
- Nije pokretan ceo clean test suite. Browser ručna/vizuelna provera nije izvršena.
