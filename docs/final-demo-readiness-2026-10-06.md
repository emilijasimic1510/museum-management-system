# Final demo readiness — 2026-10-06

Ovaj dokument beleži završni tehnički audit trenutnog radnog stabla. U ovoj proveri nisu menjani business flow, API ugovori, autorizacija, baza ili produkcioni backend servisi. Nije urađen commit niti push.

## Rezultati provere

1. **Backend testovi:** puni `backend/mvnw.cmd clean test` nisu mogli da počnu u ovom okruženju. Maven je pre kompajliranja zahtevao Spring Boot parent sa Maven Central-a, a sandbox je odbio mrežni pristup (`Permission denied: getsockopt`). Pokušaj odobrenog mrežnog izvršenja nije pokrenut jer je automatska approval provera dostigla usage limit. Prethodno dokumentovani izolovani prolazi ostaju: osnovni full backend `153/153 PASS`, rich seed/image prolaz `14/14 PASS`, email selekcija `59/59 PASS` i journey/recommendation selekcija `22/22 PASS`.
2. **Backend package/build:** nije potvrđen u ovoj sesiji zbog istog Maven dependency limita. Pokrenuti `backend/mvnw.cmd clean package` na računaru sa Maven pristupom i Docker-om nakon uspešnog clean test prolaza.
3. **Frontend testovi:** poslednji lokalni rezultat dostavljen u toku rada bio je `155/156 PASS`, sa jednim QR-board testom koji je čekao asinhrono generisanje QR slike. Test je minimalno stabilizovan čekanjem `toDataURL` poziva; sandbox nije mogao ponovo da pokrene Angular/Vitest zbog filesystem sandbox ograničenja. Potrebno je lokalno potvrditi završnih `156/156 PASS`.
4. **Frontend production build:** lokalni izlaz je završio sa `Application bundle generation complete`; ostala su samo upozorenja za neiskorišćen `UiLabelPipe` i CommonJS `qrcode` paket. Sandbox build ne može da pristupi Angular privremenim putanjama.
5. **TypeScript:** `npx tsc --noEmit -p tsconfig.app.json` — **PASS**.
6. **Diff check:** `git -c core.safecrlf=false diff --check` — **PASS**.

## Demo i bezbednost

- **Secrets/security:** u diff-u nema Gmail lozinke, App Password-a, JWT tajne, PostgreSQL/Neo4j lozinke niti tracked `.env` fajla. Konfiguracija koristi environment placeholder-e; testne lozinke su ograničene na test fixtures. Pre slanja repozitorijuma ponoviti skeniranje tracked fajlova i proveriti da lokalni `.env` ostaje ignored.
- **Rich demo seed:** `RichDemoDataSeeder` i `LocalImageSeeder` rade samo u `local` profilu, koriste advisory lock i stabilne reference, i projektovani su za idempotentan restart. Seed testovi i dokumentacija potvrđuju dopunu bez duplikata; stvarni lokalni prolaz treba ponoviti sa Java 21, PostgreSQL i Docker-om.
- **Neo4j:** preporuke i rute imaju PostgreSQL fallback kada Neo4j nije dostupan. Za odbranu pokrenuti Neo4j i proveriti projekciju/route smoke test; nedostupan Neo4j nije authorization bypass.
- **Email:** disabled mail mode radi bez SMTP servera; SMTP greška ne rollback-uje business operaciju. Za demo ostaviti `MUSEUM_MAIL_ENABLED=false` ili proveriti SMTP samo sa environment secrets.
- **LAN/mobile/QR:** frontend/API base URL i CORS su configurable; QR board generiše `<publicBaseUrl>/scan/<opaque-token>`. Ručni telefonski test je obavezan na istoj Wi-Fi mreži.
- **QR board:** `/staff/qr-board` je za CURATOR/ADMIN, ima izbor izložbe, selekciju i A4 print layout sa dve kolone. Pre odbrane proveriti browser print preview i crno-belu čitljivost.

## Pre-defense smoke checklist

1. Pokrenuti PostgreSQL i Neo4j, backend sa Java 21 i `local` profilom, pa proveriti da se rich seed pri restartu ne duplira.
2. Pokrenuti frontend sa LAN host binding-om i proveriti `/`, login i public catalog sa telefona.
3. Na telefonu skenirati najmanje dva QR-a: bez login-a (login return na `/scan/:token`) i sa aktivnom posetom (scan, tekst, slika/audio i preporuka). Proveriti jasnu poruku bez aktivne posete.
4. Otvoriti `/staff/qr-board`, izabrati reprezentativnu izložbu, odštampati PDF i proveriti page break, duge nazive i QR fokus.
5. Proći role smoke: Visitor, Cashier, Guide, Curator, Manager i Admin; proveriti da zaštićene rute i scan authorization ostaju aktivni.
6. Za screenshotove proveriti SR i EN navbar, dashboard/tabele, curator/guide forme, visitor content i QR board na 1366×768 i 360–430 px.

## Poznata ograničenja

- Ovaj sandbox ne dozvoljava Maven Central/Docker ni Angular filesystem putanje, pa puni backend/frontend rerun mora biti izvršen lokalno van sandboxa.
- Neo4j timing/fallback logovi mogu biti očekivani u hladnom kontejneru; proveriti da se graf projekcija eventualno stabilizuje pre zaključivanja.
- Frontend build ima samo postojeća upozorenja (CommonJS `qrcode`, unused pipe); nisu funkcionalni failure-i.
- Fizički telefon, stvarni browser print i Gmail SMTP nisu automatizovani testovi.

## Zaključak

Kod i prethodno dokumentovani selektivni prolazi pokrivaju traženi demo. Aplikacija je **spremna za završni diplomski demo uz obavezni lokalni clean backend test/package, završnu lokalnu potvrdu `npm test` (cilj 156/156), pokrenute PostgreSQL/Neo4j servise i kratki telefon/print smoke test**. Bez tih environment provera ne treba označiti readiness kao potpuno potvrđen.
