# Rich local demo dataset — 2026-10-03

## Obim

Dodat je `RichDemoDataSeeder`, nakon postojećeg `DemoDataSeeder`, isključivo za profil `local` bez `prod`/`production`. Nema izmene API ruta, entiteta, EER-a, PostgreSQL šeme, Flyway V1–V3, poslovnih servisa niti Neo4j projekcije. Postojeće necommitovane izmene ostaju sačuvane. Nema commit-a/push-a.

Nazivi institucija, adrese, biografije i dela novih autora su izmišljeni. Stari muzej, tri izložbe, devet eksponata, dva autora, postojeći QR tokeni i šest prijavnih naloga ostaju. Nove institucije su Demo Muzej savremenih tokova, Demo Galerija obalske fotografije i Demo Muzej forme i zanata. Njihove sale imaju različite spratove, površine i kapacitete.

`RichDemoCatalog` sadrži uređene biografije, materijale, poreklo, pojedinačne opise dela i četiri rečenice konteksta za svaku novu postavku. Tip eksponata, doprinos autorstva i kategorija cene već su slobodan tekst u odobrenom modelu; nisu uvedeni novi enum-i. Događaji koriste samo postojeće TURA/PREDAVANJE/RADIONICA, sadržaj samo TEKST. Porodični programi su RADIONICA, razgovori sa kustosom PREDAVANJE.

## Tačne količine

Potvrđeno prvim ciljanim pokretanjem na praznom PostgreSQL 17.9 kontejneru sa neizmenjenim migracijama. Na postojećoj lokalnoj bazi korisnički podaci se ne brišu: ukupni brojevi mogu biti veći. „Postojeći seed” znači osnovne podatke pre ovog dodatka, bez ručnih aktivnosti.

| Podaci | Postojeći seed | Novi dodatak | Ukupno |
|---|---:|---:|---:|
| Muzeji | 1 | 3 | **4** |
| Sale | 3 | 9 | **12** |
| Autori | 2 | 26 | **28** |
| Eksponati | 9 | 56 | **65** |
| Autorstva | 11 | 64 | **75** |
| Izložbe | 3 | 12 | **15** |
| Participation / postavke | 10 | 96 | **106** |
| TEKST sadržaji | 10 | 96 | **106** |
| Termini | 5 | 30 | **35** |
| Cene / cenovni periodi po kategoriji | 6 | 96 | **102** |
| Događaji | 4 | 16 | **20** |
| Karte | 0 | 97 | **97** |
| Transakcije | 0 | 97 | **97** |
| Završene posete | 0 | 58 | **58** |
| Skeniranja | 0 | 288 | **288** |
| Ocene | 0 | 47 | **47** |
| Prijave na događaje | 1 | 84 | **85** |

Od 97 karata, **85 pripada istorijskim terminima**, a **12 budućim terminima**. Statusi pri prvom seed-u: 58 ISKORISCENA, 18 ISTEKLA, 9 OTKAZANA, 12 KUPLJENA; nema veštački trajno aktivnih poseta. Transakcije: **88 USPESNA**, **9 REFUNDIRANA**. Postoji **230 uzastopnih prelaza** između različitih postavki (288 skeniranja − 58 poseta), a trajanja uključuju različite vrednosti i nullable zapise.

Osam novih dela imaju po dva autora; ostala po jednog. Svaka nova izložba ima osam postavki. Stare tri izložbe namerno zadržavaju 4/3/3 postavke zbog postojećeg demo ugovora i testova; prosek ukupne zbirke je 7,07. Ponovljeni eksponati imaju različite kontekstualne opise, pozicije, redosled, QR i tekstualnu datoteku na svakoj izložbi.

Novi katalog obuhvata SLIKA, SKULPTURA, FOTOGRAFIJA, GRAFIKA, CRTEZ, INSTALACIJA i PRIMENJENA_UMETNOST. Sve nove cene izražene su u RSD: ODRASLI 550–750 kroz istorijske/važeće periode, STUDENT 350/400, PENZIONER 300/350 i DETE 200/250. Svaka nova izložba ima četiri kategorije sa dva susedna, nepreklapajuća perioda. Stare cene 8/5, 6/4 i 7/4 nisu menjane.

## Kalendar i idempotentnost

**D** je datum prvog rich seed-a u zoni Europe/Belgrade. Datum ostaje usidren preko datuma registracije posebnog istorijskog naloga `demo.history.01@museum.local` (D−120). To polje se ne menja kroz postojeći account API. Restart ne pomera istoriju i ne kreira novi paket podataka za novi datum.

- Šest novih aktivnih izložbi: D−45 do D+120; svaka ima istorijski termin, produženi demo termin D 00:00 do D+1 00:00 za trenutnu kupovinu/validaciju i termin D+2 od 10:00 do 18:00.
- Tri buduće: D+14 do D+100; termini D+14 i D+16 od 10:00 do 18:00.
- Tri završene: D−90 do D−49; svaka ima dva istorijska termina.
- Istorijske posete su na D−2, −5, −8, −11, −14, −17, −55, −63 i −71; kupovine prethode ulascima.
- Pri prvom kreiranju ukupno je 9 aktivnih, 3 planirane i 3 završene izložbe. Nema nedozvoljenog preklapanja u istoj sali.
- Pri prvom kreiranju ukupno je 12 planiranih, 7 završenih i 1 otkazan događaj, računajući i stari seed. Jezik je pretežno sr, uz jedan en događaj. Sve nove događaje vodi postojeći staff GUIDE podtip; zasebni demo.guide i dalje može da upravlja njima po postojećoj politici.

Stabilni ključevi su naziv muzeja, muzej+naziv sale, ime+prezime autora, inventarski broj `DEMO-RICH-EX-001..056`, naziv izložbe, QR postavke, izložba+početak termina, složeni ključ cene, naziv događaja, email i QR karte. Sadržaj se dopunjava po participation+TEKST+sr+verzija. Autorstva i prijave koriste postojeće složene ključeve. Ne koristi se uslov „baza prazna”.

Ceo rich seed je jedna transakcija sa PostgreSQL advisory zaključavanjem, pa konkurentna rich pokretanja ne dupliraju podatke. Nova istorijska karta, uplata, ulazak, skeniranja, izlazak i ocena nastaju u istoj transakciji. Tok poštuje postojeće trigere: KUPLJENA → uspešna uplata → AKTIVNA poseta → skeniranja → ZAVRSENA/ISKORISCENA → ocena. Ne isključuju se ograničenja baze. Već sačuvane istorijske karte se ne rekonstruišu i ne resetuju.

Ponovni seed dopunjava nedostajuće katalog-veze i TEKST zapise, a čuva kustoske tekstove i postojeće cene. Test proverava popravku delimičnog kataloga, sve redove pre/posle drugog pokretanja i isti rich seed uz Clock pomeren za 40 dana.

U starom `DemoDataSeeder` postoji mala korekcija isključivo zakazivanja seeda: par budućih termina sada se računa od originalnog početka izložbe, a inicijalni live termin dodaje se samo ako nema ranije započetog termina. Time prestaje ranije gomilanje termina pri svakom novom danu/restartu. Postojeći termini se ne prepisuju niti brišu. Ranija tvrdnja da svaki restart stalno pravi novi live termin više ne važi.

Kalendar je konačan. Posle isteka termina koristi postojeći Collection ekran za nove termine unutar trajanja izložbe; restart ne osvežava datume istorije ili produžava izložbe. Postojeći JourneyMaintenance normalno menja statuse protekom vremena. Ne treba preimenovati seed prirodne ključeve ako se očekuje identifikacija istog zapisa na restartu.

## Nalozi, tekst i audio

Ostaju isti nalozi sa lozinkom `MuseumDemo!2026` (osim ako ju je korisnik ranije ručno promenio):

- `demo.visitor@museum.local`
- `demo.staff@museum.local`
- `demo.curator@museum.local`
- `demo.guide@museum.local`
- `demo.manager@museum.local`
- `demo.admin@museum.local`

Dodato je 18 izmišljenih istorijskih visitor profila, sa **neaktivnim** nalozima `demo.history.01..18@museum.local` i BCrypt hash-om nasumične, nesačuvane lozinke. Nisu novi nalozi za prijavljivanje. Demo visitor ne dobija tuđu istoriju, niti mu se prekida postojeća aktivna poseta. Na čistom seed-u ukupno postoje 24 naloga i 19 visitor profila.

Dodato je 96 stvarnih UTF-8 datoteka u `frontend/public/demo/rich/`, jer postojeći TEKST prikaz otvara reference sa frontend origin-a. Nije promenjen Angular kod, stil, ruta ili API. Datoteke imaju izložbu, delo, konkretni kontekst, opis i materijal. Kao i prethodni demo tekstovi, statički URL-ovi nisu privatni storage; scan-based pravila postojećeg API-ja ostaju nepromenjena.

Nema novih AUDIO zapisa, lažnih `.mp3` putanja ili automatskog pokretanja eSpeak-a. Za audio prijaviti kustosa i generisati srpski audio postojeće postavke kroz postojeći TTS tok. Neo4j se i dalje puni isključivo postojećom PostgreSQL projekcijom; nije dodat paralelni graph seed.

## Najbolji scenariji za odbranu

1. **Katalog i povezanost:** anonimno otvoriti „Grad između stanica”, „Ravnoteža materije”, „Predmeti koji traju” i buduću „Prostor za susret”. Pokazati autore, materijale, osam postavki i različite teme. Na autorstvima pogledati „Krovovi sa severa”, „Tkanje” ciklus ili „Zajednički sto” i dva različita doprinosa kolektivnom radu.
2. **Isto delo, druga priča:** „Poslednji tramvaj” je na aktivnoj „Grad između stanica” (`DEMO-PHOTO-001`) i budućoj „Noćne putanje” (`DEMO-URBAN-007`). Uporediti kontekst, položaj, redosled 1/7 i odvojene TEKST reference. „Težište” povezuje aktivnu „Ravnoteža materije” i završenu „Unutrašnja geometrija”.
3. **Kupovina → validacija → scan → TTS:** na dan D kao demo.visitor kupiti kartu za produženi termin „Grad između stanica”, kategoriju ODRASLI. Kao demo.staff validirati QR te karte. Kao visitor skenirati `DEMO-PHOTO-001`, pa `DEMO-PHOTO-002`. Pokazati tekst, narednu postavku i rutu. Kustos može generisati audio za konkretno učešće. Zatim završiti posetu i oceniti je.
4. **Manager:** prijaviti demo.manager i izabrati **D−90 do D+16**. Prikazati posećenost po muzeju/danu, uspešan prihod, kategorije/metode plaćanja, četiri zastupljena statusa karata, popularnost, različita trajanja, 47 ocena, događaje i 230 prelaza. Uspešna uplata istorijske karte ostaje vezana za tadašnju cenu.
5. **Guide:** kroz događaje otvoriti „Razgovor sa kustosom · Ravnoteža materije”, „Radionica posmatranja · Svetlost iza prozora” i završenu „Porodična radionica · Grad između stanica”. Prikazati različite kapacitete, prijave, prisustvo i otkazivanja.
6. **Collection:** na „Predmeti koji traju” prikazati četiri kategorije, stariji i važeći period. Na sali „Velika galerija” pokazati završenu „Unutrašnja geometrija” i kasniju aktivnu „Grad između stanica” bez preklapanja.

Najbolji pojedinačni eksponati: Poslednji tramvaj, Težište, Lampa za čitanje, Herbarium trotoara, Portret čitačice, Soba od niti i Zajednički sto.

| QR | Izložba | Eksponat |
|---|---|---|
| `DEMO-PHOTO-001` | Grad između stanica | Poslednji tramvaj |
| `DEMO-PHOTO-002` | Grad između stanica | Prozor iznad pijace |
| `DEMO-SCULPTURE-007` | Ravnoteža materije | Težište |
| `DEMO-DESIGN-004` | Predmeti koji traju | Lampa za čitanje |
| `DEMO-SPACE-001` | Prostor za susret | Soba od niti |
| `DEMO-URBAN-007` | Noćne putanje | Poslednji tramvaj |
| `DEMO-CITY-QR-001` | Grad kroz objektiv | Jutro na trgu — postojeći token |

Postojeća local-demo QR tabla namerno ostaje ograničena na ranijih 10 tokena. Nove tokene uneti u postojeći scan unos ili prikazati QR iz kustoskog pregleda postavke. Nisu proširivane API rute radi nove table.

Ako je prvi rich seed **03.10.2026**, najbogatiji Manager period je **05.07.2026–19.10.2026**. Za isključivo istorijsku aktivnost dovoljan je 05.07–03.10, ali širi opseg uključuje i buduće događaje. Za drugi datum instalacije pomeriti oba datuma zajedno. D se može proveriti SQL-om:

```sql
select (datum_registracije at time zone 'Europe/Belgrade')::date + 120 as rich_seed_day
from korisnicki_nalog where email='demo.history.01@museum.local';
```

## Pokretanje i provere

Pokrenuti backend sa postojećom konfiguracijom i profilom `local`:

```powershell
# Iz backend direktorijuma, uz postojeće baze i JWT_SECRET:
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
```

Razvojna baza nije menjana tokom implementacije; seed će se izvršiti na sledećem local pokretanju. Provere se izvršavaju na izolovanim Testcontainers bazama. Ne instaliraju se dodatne zavisnosti.

- Ciljani prvi prolaz: `mvnw.cmd -Dtest=RichDemoDataSeederTests test` — **7/7 PASS**, 0 failures/errors/skipped; 1 min 13 s. Potvrđene gornje količine na čistoj bazi.
- Završni rich testovi: dodata je i osma provera stvarnog Manager dashboard servisa, uključena u puni backend prolaz.
- Puni backend: rezultat se dopunjava nakon završetka.
- Frontend: `npm.cmd test` — **140/140 PASS**, 14 fajlova. Pokrenut zbog dodatih statičkih tekstova iako Angular kod nije menjan.
- `git diff --check`: završna provera se dopunjava nakon završetka.

Testovi proveravaju količine, sve autorstvo-veze, jedinstvene tokene, višestruko izlaganje dela, različite kontekste/pozicije/redoslede/sadržaj, stvarne UTF-8 reference, rasporede/kapacitete, cene bez preklapanja, istorijski lifecycle, uplate, skeniranja, ocene/prijave, Manager agregacije, idempotentnost i očuvanje izmena. Ne tvrdi se da je izvršen novi vizuelni browser test ili stvarno eSpeak slušanje.

Fajlovi ovog zadatka: postojeći `DemoDataSeeder.java` (redosled i stabilni termini), novi `RichDemoCatalog.java`, `RichDemoDataSeeder.java`, `RichDemoDataSeederTests.java`, 96 `frontend/public/demo/rich/*.txt` i ovaj dokument. Lokalni logovi su gitignored: `backend/rich-seed-tests.log`, `backend/rich-full-backend-tests.log`, `frontend/rich-assets-frontend-tests.log`.
