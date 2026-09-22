# Relacioni model — podsistem 2

Izvori: specifikacija §3, §8–10 i §12; EER drugog podsistema i konačni EER. V1 i odobreni dokumenti ostaju neizmenjeni.

## Mapiranje pre implementacije

ID: bigint identity. Datumi procesa: timestamptz/Instant; datum zaposlenja: date/LocalDate. Tekst: text; enum: varchar(32); logičke vrednosti: boolean. Sve PK kolone su NOT NULL.

| EER element / tabela | Kolone | PK | FK | UNIQUE | NULL pravila | CHECK | Kardinalitet | ON DELETE |
|---|---|---|---|---|---|---|---|---|
| KorisničkiNalog / korisnicki_nalog | id, email, lozinka_hash, aktivan, datum_registracije, poslednja_prijava | id | — | email, lower(email) | poslednja_prijava opciona; ostalo obavezno | BCrypt format | nalog ima 1..N uloga; 0..1 posetioca i 0..1 zaposlenog | RESTRICT za zavisne profile/dodele |
| Uloga / uloga | id, naziv, opis | id | — | naziv | opis opciono | šest odobrenih naziva | 0..N dodela | RESTRICT |
| NalogUloga / nalog_uloga | nalog_id, uloga_id, datum_dodele, dodelio_id | (nalog_id, uloga_id) | nalog, uloga, opciono nalog dodelioca | PK | dodelio_id opciono | minimum jedna uloga po nalogu: odloženi trigger | M:N + opcioni dodelilac | RESTRICT; dodelilac SET NULL |
| Posetilac / posetilac | id, nalog_id, ime, prezime, telefon, saglasnost_za_preporuke | id | nalog_id | nalog_id | telefon opciono; ostalo obavezno | — | profil tačno 1 nalog; nalog 0..1 profil | RESTRICT |
| Zaposleni / zaposleni | id, nalog_id, muzej_id, ime, prezime, telefon, datum_zaposlenja, tip_zaposlenog, nadredjeni_id | id | nalog, muzej iz V1, zaposleni | nalog_id | telefon i nadredjeni_id opciono | pet podtipova; zabrana sopstvenog nadređenog | tačno 1 nalog/muzej/podtip; 0..1 nadređeni; 0..N podređenih | RESTRICT |
| Kustos / kustos | zaposleni_id, specijalizacija | zaposleni_id | zaposleni | PK | specijalizacija opciona | IS-A trigger | tačno 1 nadtip | RESTRICT |
| Vodič / vodic | zaposleni_id, primarni_jezik | zaposleni_id | zaposleni | PK | primarni_jezik opciono | IS-A trigger | tačno 1 nadtip; 0..N događaja | RESTRICT |
| Blagajnik / blagajnik | zaposleni_id, oznaka_saltera | zaposleni_id | zaposleni | PK | oznaka_saltera opciono | IS-A trigger | tačno 1 nadtip | RESTRICT |
| Menadžer / menadzer | zaposleni_id, nivo_izvestavanja | zaposleni_id | zaposleni | PK | nivo_izvestavanja opciono | IS-A trigger | tačno 1 nadtip | RESTRICT |
| Administrator / administrator | zaposleni_id, nivo_administracije | zaposleni_id | zaposleni | PK | nivo_administracije opciono | IS-A trigger | tačno 1 nadtip | RESTRICT |
| Interesovanje / interesovanje | id, naziv, opis | id | — | naziv | opis opciono | — | 0..N posetilaca | RESTRICT |
| interesuje se za / posetilac_interesovanje | posetilac_id, interesovanje_id, nivo | (posetilac_id, interesovanje_id) | posetilac, interesovanje | PK | nivo opciono | nema izmišljenog opsega nivoa | M:N, 0..N sa obe strane | RESTRICT |
| Događaj / dogadjaj | id, izlozba_id, vodic_id, naziv, tip, pocetak, kraj, kapacitet, jezik, status | id | izlozba iz V1, vodic | — | vodic_id i jezik opciono; ostalo obavezno | tip/status, ispravan interval, nenegativan kapacitet | tačno 1 izložba; 0..1 vodič; 0..N prijava | RESTRICT |
| prijavljuje se na / prijava_na_dogadjaj | posetilac_id, dogadjaj_id, datum_prijave, status | (posetilac_id, dogadjaj_id) | posetilac, dogadjaj | PK | sve obavezno | PRIJAVLJEN/PRISUSTVOVAO/OTKAZAN | M:N | RESTRICT |

Svi FK imaju ON UPDATE RESTRICT. Indeksi pokrivaju FK i početak događaja. Ne uvodi se direktan FK događaj–muzej: veza je događaj → izložba → sala → muzej.

## IS-A i minimalne kardinalnosti

JPA: apstraktni Employee, @Inheritance(JOINED), diskriminator tip_zaposlenog; pet konkretnih podtipova sa @PrimaryKeyJoinColumn. PostgreSQL odloženi constraint triggeri nad nadtipom i svih pet podtipova zaključavaju nadtip, broje podtipove i proveravaju da postoji tačno jedan i da odgovara diskriminatoru. Time ni direktan SQL ne može ostaviti nadtip bez podtipa ili sa dva podtipa nakon commit-a.

Zaposleni se kreira kao konkretan podtip, u jednoj transakciji. PUT menja podatke postojećeg podtipa; promena podtipa nije implicitna (409). Poslovni podtip i RBAC uloga su nezavisni; zaposlenje ne daje automatski privilegije.

NalogUloga koristi @EmbeddedId/@MapsId. Odloženi trigger čuva minimum 1..N i pri direktnom SQL-u; servis ne dozvoljava uklanjanje poslednje uloge. Datum dodele se popunjava automatski, dodelilac je autentifikovani administrator, a za registraciju NULL.

Nalog može imati i profil posetioca i profil zaposlenog: dva nezavisna FK UNIQUE, bez neodobrenog međusobnog isključivanja. Nadređenost je opcioni self FK; servis sprečava cikluse i serijalizuje izmene hijerarhije. Ne zahteva se isti muzej nadređenog jer to nije navedeno.

## Tumačenja i granice

- Korisničko ime nije atribut u specifikaciji ni EER-u. Identitet za login je email (trim + lowercase); ne dodaje se username kolona. Jedinstvenost proveravaju servis i DB, uključujući razliku u velikim/malim slovima.
- Opisna polja i podtipska obeležja bez eksplicitne obaveznosti ostaju opciona. Ime/prezime i osnovni operativni podaci su obavezni na API granici.
- Statusi i tipovi događaja/prijava su zatvoreni skupovi iz specifikacije. Nema izmišljenog zahteva da svaki događaj ima vodiča ili da je događaj nužno unutar termina/perioda izložbe: takvo pravilo za događaj nije propisano. Proverava se ispravan interval početak < kraj.
- Prijava je dozvoljena samo za PLANIRAN događaj sa preostalim kapacitetom. PRIJAVLJEN i PRISUSTVOVAO zauzimaju mesto. OTKAZAN zapis čuva istoriju i može se ponovo aktivirati; druga aktivna prijava istog posetioca daje 409.
- Pri prijavi/odjavi i promeni kapaciteta zaključava se red događaja. Ne uvodi se dodatno vremensko pravilo prijave (npr. rok 24 h), jer nije u specifikaciji. ZAVRSEN/OTKAZAN/POPUNJEN događaj ne prihvata nove prijave. Kapacitet se ne može smanjiti ispod zauzetih mesta.
- Nema fizičkog automatskog brisanja naloga sa profilima/istorijom; administrator može deaktivirati nalog. Fizički DELETE ostaje ograničen FK vezama i transakcijom.

## Bezbednost i automatizacija

Registracija kreira nalog sa BCrypt hashom, ulogom VISITOR i profilom posetioca u jednoj transakciji. Klijent ne bira ulogu, aktivnost ili hash. Datum registracije/dodele i poslednja uspešna prijava se evidentiraju automatski.

JWT: HS256 preko Spring Security/Nimbus; sub je ID naloga, exp je 30 minuta, issuer museum-backend. Ključ dolazi isključivo iz environment promenljive JWT_SECRET (najmanje 32 UTF-8 bajta). Nema podrazumevanog ključa, tokeni/lozinke se ne loguju. Testovi generišu zaseban nasumičan ključ. Za svaki token se učitavaju trenutne uloge i aktivnost naloga iz baze, pa deaktivacija i oduzimanje uloge važe odmah. Nema refresh tokena u ovoj fazi.

Local profil zadržava samo postojeće anonimne CRUD rute prvog podsistema. Administratorske operacije i privatni profili ostaju zaštićeni i lokalno. Nema unapred kreiranog administratora ili fiksne lozinke; prvi administrator se dodeljuje postojećem nalogu eksplicitnom lokalnom DB administracijom, opisano u README.

## Precizni tipovi kolona

Svi ID i FK iz prethodne tabele su PostgreSQL bigint (Java Long). Samo samostalni PK id imaju GENERATED BY DEFAULT AS IDENTITY; podtipovi i spojne tabele ne generišu nove ključeve.

| Tabela | Ostale kolone sa SQL tipovima |
|---|---|
| korisnicki_nalog | email text, lozinka_hash text, aktivan boolean, datum_registracije timestamptz, poslednja_prijava timestamptz |
| uloga | naziv varchar(32), opis text |
| nalog_uloga | datum_dodele timestamptz |
| posetilac | ime text, prezime text, telefon text, saglasnost_za_preporuke boolean |
| zaposleni | ime text, prezime text, telefon text, datum_zaposlenja date, tip_zaposlenog varchar(32) |
| kustos | specijalizacija text |
| vodic | primarni_jezik text |
| blagajnik | oznaka_saltera text |
| menadzer | nivo_izvestavanja text |
| administrator | nivo_administracije text |
| interesovanje | naziv text, opis text |
| posetilac_interesovanje | nivo integer |
| dogadjaj | naziv text, tip varchar(32), pocetak timestamptz, kraj timestamptz, kapacitet integer, jezik text, status varchar(32) |
| prijava_na_dogadjaj | datum_prijave timestamptz, status varchar(32) |

FK ciljevi: nalog_id i dodelio_id → korisnicki_nalog.id; uloga_id → uloga.id; muzej_id → muzej.id; nadredjeni_id → zaposleni.id; zaposleni_id u svih pet podtipova → zaposleni.id; posetilac_id → posetilac.id; interesovanje_id → interesovanje.id; izlozba_id → izlozba.id; vodic_id → vodic.zaposleni_id; dogadjaj_id → dogadjaj.id.

CHECK skupovi: tip_zaposlenog = CURATOR/GUIDE/CASHIER/MANAGER/ADMINISTRATOR; uloga.naziv dodatno dozvoljava VISITOR; dogadjaj.tip = TURA/PREDAVANJE/RADIONICA; dogadjaj.status = PLANIRAN/POPUNJEN/ZAVRSEN/OTKAZAN; prijava.status = PRIJAVLJEN/PRISUSTVOVAO/OTKAZAN. Kapacitet >= 0, kraj > pocetak, nadredjeni_id <> id. Regex ograničava lozinka_hash na BCrypt zapis. Nije uveden proizvoljan opseg za nivo interesovanja.

## REST rute i RBAC

U narednoj tabeli CRUD znači GET kolekcije, POST kolekcije, GET/PUT/DELETE /{id}. POST vraća 201 i Location, PUT/GET 200, DELETE 204. Neispravan zahtev daje 400, nedostajući zapis 404, konflikt 409, nedostajuća/nevažeća autentifikacija 401 i nedovoljna uloga 403. Centralni postojeći ApiExceptionHandler obrađuje validaciju i integritet; dodatni AuthenticationExceptionHandler obrađuje autentifikaciju i autorizaciju u MVC sloju. JPA entiteti se ne vraćaju kroz REST.

| Ruta | Metode | Pristup |
|---|---|---|
| /api/health | GET | javno, postojeća ruta |
| /api/auth/register | POST | javno; email, password, firstName, lastName, phone?, recommendationsConsent |
| /api/auth/login | POST | javno; isključivo email i password |
| /api/accounts/me | GET | aktivan autentifikovani nalog |
| /api/admin/accounts | CRUD | ADMINISTRATOR; POST koristi registracijski DTO, formira nalog i profil posetioca |
| /api/admin/accounts/{id}/roles/{roleId} | POST, DELETE | ADMINISTRATOR |
| /api/admin/roles | CRUD | ADMINISTRATOR |
| /api/employees | CRUD | ADMINISTRATOR, administracija korisničkih profila zaposlenih |
| /api/visitors | CRUD | ADMINISTRATOR, administracija korisničkih profila posetilaca |
| /api/visitors/me | GET, PUT | VISITOR, sopstveni profil |
| /api/visitors/me/interests | GET | VISITOR, sopstvena interesovanja |
| /api/visitors/me/interests/{id} | PUT, DELETE | VISITOR, sopstveni izbor interesovanja |
| /api/visitors/me/registrations | GET | VISITOR, sopstvene prijave |
| /api/interests | CRUD | GET javno; izmene CURATOR/ADMINISTRATOR (teme/sistemski šifarnik) |
| /api/events | CRUD | GET javno; izmene GUIDE |
| /api/events/{id}/registrations | POST | VISITOR, prijava sebe |
| /api/events/{id}/registrations | GET | GUIDE, pregled prijavljenih |
| /api/events/{id}/registrations/me | DELETE | VISITOR, odjava sebe |
| /api/events/{id}/registrations/{visitorId} | PUT | GUIDE, izmena statusa prijave |

RBAC prati §3: vodič upravlja događajima i prijavljenim posetiocima; administrator upravlja nalozima, profilima, ulogama i sistemskim šifarnicima; kustos temama. Nema implicitne administratorske dozvole za upravljanje događajima: administratoru se za to eksplicitno dodeljuje i GUIDE. Specifikacija ne ograničava vodiča isključivo na događaje kojima je lično pridružen, pa se takvo dodatno pravilo ne uvodi. CASHIER i MANAGER nemaju nova ovlašćenja za upis u ovom podsistemu; prodaja i analitika nisu implementirane. Zaposlenje/podtip ne menja RBAC uloge.

Šest uloga se inicijalizuje V2 migracijom. POST nove uloge dozvoljava samo odobrene nazive; postojeći naziv daje 409. DELETE dodeljene uloge sprečava FK. PUT ne menja naziv postojeće uloge. Dodavanje novih vrsta ovlašćenja nije deo ove faze.

Automatski se ažurira status PLANIRAN/POPUNJEN nakon prijave, odjave, izmene statusa prijave ili kapaciteta. OTKAZAN/ZAVRSEN ostaju očuvani. Odloženi DB trigger dodatno proverava kapacitet, a servis zaključava događaj pre promene. Nema novih periodičnih poslova za ulaznice, posete, QR, TTS ili Neo4j.

Potvrda korisnika od 22.09.2026: email predstavlja korisničko ime za prijavu; odvojen username, korisnicko_ime ili login_name nije deo šeme niti DTO ugovora.
