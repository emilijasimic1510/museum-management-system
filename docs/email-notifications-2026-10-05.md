# Email obaveštenja — 2026-10-05

## Audit i obim

Pre izmena pročitana je dokumentacija o visitor lifecycle-u, slikama i SR/EN sadržaju; izvršeni su `git status --short` i `git diff --stat`. Postojeće nezabeležene izmene su sačuvane. Nema reset/checkout/commit/push komandi.

Spring Mail nije postojao. Registracija je u `AccountService`, kupovina i ponovna naplata u `SalesService`, prijave/odjave u `EventService`, završetak posete u `VisitService`, a automatsko zatvaranje isteklih poseta u `JourneyMaintenance`. Adresa primaoca već postoji u `Account.email`; visitor je povezan sa nalogom. Anonimna blagajnička prodaja nema primaoca. Korisnički profil nema polje za jezik; Angular `I18n` čuva UI izbor u `museum.language` u localStorage-u.

Tokom integracionih provera prethodno izmenjeni `AudioService` blokirao je start Spring konteksta: imao je dva konstruktora bez oznake za dependency injection. Dodata je samo `@Autowired` oznaka na postojeći puni konstruktor. Ranije audio izmene nisu uklonjene; audio logika i security nisu menjani ovim korakom.

Dodati su standardni `spring-boot-starter-mail`, centralni `EmailService`, samostalni backend SR/EN `EmailTemplates`, SMTP podešavanja i testovi. Kontroleri ne sastavljaju poruke. Nema izmene baze, Flyway migracija, entiteta ili JSON DTO-a. Pravila naplate, uloga, poseta i preporuka ostala su ista. Postojeće zaštite slika, teksta i audija ostale su sačuvane.

## Događaji i sadržaj

| Događaj | Kada i šta se šalje |
| --- | --- |
| Registracija naloga | Posle kreiranja naloga/profila: dobrodošlica i potvrda uspešne registracije. Bez lozinke. Ovo ne uvodi verifikaciju vlasništva email adrese, aktivacioni link ili promenu login pravila. |
| Kupovina karte | Samo uspešna naplata, uključujući uspešan retry i blagajničku prodaju poznatom visitor-u: izložba, početak/kraj termina, kategorija, plaćena cena, status i ID ulaznice. Odbijena naplata i anonimna prodaja ne šalju poruku. |
| Prijava na događaj | Naziv, početak/kraj, izložba i potvrđen status. Ponovna prijava nakon odjave ponovo šalje potvrdu. |
| Otkazivanje prijave | Naziv, vreme, izložba i potvrda otkazivanja. Ponavljanje već izvršene odjave ne šalje novu poruku. Guide promena statusa na otkazano i vraćanje na prijavljen takođe šalju odgovarajuću potvrdu. |
| Završetak posete | Zahvalnica, izložba i poziv da se ocena unese na detalju posete u odeljku Posete. Ručni završetak, završetak pri ocenjivanju istekle posete i automatsko zatvaranje pokriveni su istim servisom. Ponovljeni završetak/maintenance ne šalje ponovo. |

Poruke imaju UTF-8 subject i tekstualni + HTML sadržaj preko `MimeMessageHelper`. Svi dinamički podaci u HTML-u su escape-ovani. Datumi su eksplicitno označeni kao UTC. Postojeći model nema valutu pa je cena preneta bez izmišljene oznake valute. QR token, JWT, hash lozinke i referenca platnog gateway-a se ne šalju. Poruka upućuje korisnika da QR prikaže kroz postojeći zaštićeni odeljak Ulaznice. Nisu uvedeni novi javni linkovi.

## SR/EN

Postojeći Angular interceptor dodaje `X-Museum-Language: sr|en` isključivo na `/api/` zahteve, uključujući registraciju. Izbor se čita iz postojećeg `I18n` servisa; promena jezika važi za sledeću akciju. Autorizacija i guard pravila nisu menjani. Backend koristi ovaj eksplicitni izbor, a ne podrazumevani jezik pregledača ili jezik vođenja događaja. `en` i regionalni `en-*` koriste engleski; sve ostalo i odsutno zaglavlje koriste srpski.

Jezik se ne čuva u bazi. Automatske posete bez HTTP zahteva zato koriste `sr`. Za akciju zaposlenog važi izbor jezika njegovog trenutnog zahteva; nema pouzdanog trajnog izbora primaoca u postojećem modelu. Trajna preference može biti zaseban budući zahtev. Backend ne učitava frontend prevode.

## Transakcije i greške

`EmailService` napravi nepromenljiv snapshot poruke unutar postojeće transakcije i registruje `TransactionSynchronization.afterCommit`. SMTP se poziva tek nakon uspešnog commit-a; rollback i neuspešan commit ne šalju poruku. Bez aktivne transakcije poruka se preskače uz upozorenje. Automatski lifecycle SQL zadržava iste update uslove i koristi `RETURNING p.id` kako bi se obavestile samo upravo završene posete.

SMTP/formatiranje greške se hvataju; poslovna operacija ostaje uspešna, a HTTP odgovor ne postaje greška zbog transporta. Log sadrži vrstu događaja i klasu izuzetka, bez adrese primaoca, sadržaja, kredencijala i sirovog SMTP exception teksta. Disabled režim ne kontaktira transport. Standardni connect/read/write timeout-i sprečavaju neograničeno čekanje na SMTP I/O.

Slanje je sinhrono nakon commit-a i može produžiti trajanje zahteva. Ovo je best-effort isporuka: nema trajnog reda, automatskog retry-a ili garancije isporuke nakon pada procesa između commit-a i slanja. Uspešan SMTP poziv ne garantuje prijem u inbox. Ne ponavljati kupovinu/registraciju zbog izostanka poruke. Za trajne retry-e eventualno dodati transactional outbox kao zaseban korak sa eksplicitnom izmenom šeme.

## Konfiguracija

Vrednosti dolaze iz env/property konfiguracije; nema stvarnih kredencijala u repozitorijumu.

| Env promenljiva | Podrazumevana vrednost / namena |
| --- | --- |
| `MUSEUM_MAIL_ENABLED` | `false`; potpuno isključivanje slanja |
| `MUSEUM_MAIL_HOST` | `localhost`; SMTP host |
| `MUSEUM_MAIL_PORT` | `1025` |
| `MUSEUM_MAIL_USERNAME` | prazno |
| `MUSEUM_MAIL_PASSWORD` | prazno; čuvati van Git-a |
| `MUSEUM_MAIL_FROM` | `museum@localhost`; u SMTP okruženju postaviti dozvoljenu adresu pošiljaoca |
| `MUSEUM_MAIL_AUTH` | `false`; uključiti za autentifikovani SMTP |
| `MUSEUM_MAIL_STARTTLS` | `false`; kada je true, STARTTLS je obavezan |
| `MUSEUM_MAIL_SSL` | `false`; implicitni TLS, npr. port 465 |
| `MUSEUM_MAIL_CONNECTION_TIMEOUT_MS` | `3000` |
| `MUSEUM_MAIL_READ_TIMEOUT_MS` | `5000` |
| `MUSEUM_MAIL_WRITE_TIMEOUT_MS` | `5000` |

Spring property ekvivalenti su `museum.mail.enabled/from`, `spring.mail.host/port/username/password` i `spring.mail.properties.mail.smtp.*`. `spring.mail.test-connection=false` sprečava proveru dostupnosti SMTP-a pri startu aplikacije. `application-test.yml` eksplicitno isključuje mail; email integracioni test ga uključuje samo uz mock `JavaMailSender`.

### Local disabled

Postaviti `MUSEUM_MAIL_ENABLED=false` ili ostaviti sve mail promenljive nepodešene. Registracija, kupovina, događaji i posete normalno rade bez SMTP servera.

### Lokalni Mailpit/MailHog

Pokrenuti sopstveni lokalni SMTP catcher. Za uobičajeni SMTP port 1025:

```properties
MUSEUM_MAIL_ENABLED=true
MUSEUM_MAIL_HOST=localhost
MUSEUM_MAIL_PORT=1025
MUSEUM_MAIL_FROM=museum@example.test
MUSEUM_MAIL_AUTH=false
MUSEUM_MAIL_STARTTLS=false
MUSEUM_MAIL_SSL=false
```

Poruke pregledati u lokalnom catcher interfejsu. U kontejneru koristiti mrežno ime SMTP servisa umesto localhost-a. Mailpit/MailHog nije instaliran niti pokrenut ovim zadatkom.

### Gmail

1. Uključiti **2-Step Verification** na Google nalogu.
2. Kreirati **App Password** za ovu aplikaciju. U `MUSEUM_MAIL_PASSWORD` uneti tu lozinku, nikada običnu Gmail lozinku. Ako App Password opcija nije dostupna zbog politike naloga, koristiti SMTP provajdera kojeg organizacija dozvoljava.
3. Postaviti `MUSEUM_MAIL_HOST=smtp.gmail.com`, `MUSEUM_MAIL_PORT=587`, `MUSEUM_MAIL_AUTH=true`, `MUSEUM_MAIL_STARTTLS=true`, `MUSEUM_MAIL_SSL=false`, `MUSEUM_MAIL_ENABLED=true`. Username je puna Gmail adresa; FROM mora biti ta adresa ili dozvoljeni alias.
4. Čuvati tajnu u lokalnoj ignorisanoj konfiguraciji ili deployment secret-u. Restartovati backend nakon izmene konfiguracije. Alternativni implicitni TLS koristi port 465, SSL=true i STARTTLS=false.

Provereno prema Google dokumentaciji: [App Passwords](https://support.google.com/accounts/answer/185833) i [SMTP za aplikacije](https://support.google.com/a/answer/176600). Gmail nije obavezni provider.

## Provere

- EmailServiceTests: **6/6 PASS**.
- EmailIntegrationTests: **5/5 PASS**. Pokrivaju svih pet događaja, SR/EN/fallback, disabled mode, SMTP izuzetak, rollback, uspešnu naplatu, odbijenu naplatu, anonimnu prodaju i idempotentno završavanje/otkazivanje.
- Selektivni backend regression (AudioServiceTests, JourneyIntegrationTests, EmailIntegrationTests, PeopleIntegrationTests): **59/59 PASS**.
- Frontend `auth.spec.ts` + `i18n.spec.ts`: **17/17 PASS**; TypeScript `--noEmit` je prethodno prošao; produkcioni build je prethodno prošao. Ciljani frontend test/build pokušaj u ovom audit turnu bio je blokiran dostignutim Codex usage limitom, pa se oslanja na prethodno zabeležene rezultate.
- `git diff --check`: **PASS**.
- Puni `mvn test` nije mogao biti potvrđen u ovom audit turnu: pokušaj bez eskalacije je blokiran sandbox mrežom, a ponovni pokušaj sa eskalacijom automatski je odbijen zbog dostignutog Codex usage limita. Nema dokaza o backend regresiji u selektivnom skupu, ali puna verifikacija ostaje otvorena.

Testovi ne šalju stvarne poruke; SMTP je mockovan, a integracioni testovi koriste izolovani Testcontainers PostgreSQL. Razvojna baza nije menjana. `MUSEUM_MAIL_ENABLED=false` i test profil rade bez SMTP servera. Nisu pronađeni Gmail kredencijali, App Password vrednosti ili tracked `.env` izmene; `MUSEUM_MAIL_PASSWORD` ostaje samo property placeholder.
