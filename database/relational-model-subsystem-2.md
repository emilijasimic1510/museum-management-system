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
