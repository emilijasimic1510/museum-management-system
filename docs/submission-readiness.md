# Submission readiness — 2026-10-09

## Conclusion

**Spremno uz navedene provere.** Kod, testovi i lokalni demonstracioni tokovi su provereni. Za javnu produkciju i fizičku demonstraciju telefonom ostaju operativni koraci: stvarni HTTPS domen/reverse proxy, produkcioni payment provider, SMTP/TTS konfiguracija i test na stvarnom telefonu. Ti koraci nisu lažno označeni kao završeni.

## Izvršene provere

- `backend\mvnw.cmd clean test package` — **PASS**, 182 testa, 0 neuspeha, 0 grešaka; Maven package i repackaged JAR **SUCCESS**. Testcontainers koristi zaseban PostgreSQL, a graf test zaseban Neo4j; razvojni Docker volume-i nisu brisani/resetovani.
- `frontend\npm test` — **PASS**, 19 test fajlova / 164 testa.
- `frontend\npx --no-install tsc --noEmit -p tsconfig.app.json` — **PASS**.
- `frontend\npm run build` — **PASS**, izlaz `frontend/dist/museum`; postoji samo Angular upozorenje da `qrcode` nije ESM.
- `git diff --check` — **PASS**; prijavljena su samo standardna upozorenja o CRLF konverziji.

Izolovani HTTP smoke test (`tmp/submission-smoke.ps1`) je prošao: registracija i prijava; kupovina; zabrana tuđeg pristupa; Cashier validacija i zabrana druge validacije; aktivna poseta; QR sken; SR tekst; eSpeak WAV; slika; preporuke; ruta; Manager; Guide događaj/prijava; Administrator nalozi; završetak i ocena; zabrana skena posle završetka. Neo4j je zatim zaustavljen u istoj izolovanoj proveri i fallback na kustoski redosled je vraćen za manje od 10 sekundi. Lokalni browser je pokazao SR/EN homepage/katalog/detalj izložbe i dostupne termine.

Fizička kamera telefona, dozvola kamere, javni HTTPS origin i fizičko štampanje nisu proverljivi iz ovog okruženja. Za njih treba otvoriti aplikaciju preko stvarnog pouzdanog HTTPS domena sa telefona i odštampati QR tablu; `localhost` i običan HTTP sa LAN IP adresom nisu dovoljan uslov za mobilnu kameru.

Za praktičnu demonstraciju koristi se Cloudflare quick tunnel: `cloudflared tunnel --url http://localhost:4200`. QR tabla se generiše tek nakon što se kustos prijavi preko iste `https://....trycloudflare.com` adrese koju će koristiti telefon, sa izabranom izložbom koja odgovara aktivnoj poseti i validiranoj ulaznici. QR kod sa prethodne localhost adrese ili sa druge tunnel adrese biće namerno odbijen. Posetilac zatim na telefonu otvara istu HTTPS adresu, ulazi u aktivnu posetu, dozvoljava kameru i skenira novu tablu. Posle svakog novog pokretanja quick tunnel-a tabla se generiše ponovo.

## Popravke urađene u ovoj proveri

- Test istorijske ulaznice je usklađen sa pravilom baze da je snapshot kupljene ulaznice nepromenljiv; istorijski zapis se sada kreira sa istorijskim vremenima.
- Idempotency test vodiča proverava tri seed događaja po njihovim nazivima i proverava stabilnost celog rezultata, bez lažnog očekivanja starog ukupnog broja.
- Katalog kartica sada ima vidljiv lokalizovan link `Detalji`/`Details`.
- Test QR table proverava stvarno generisanu QR sliku, bez nestabilnog mock-a CommonJS biblioteke.
- Poruka server greške sa statusom se ponovo prevodi kada korisnik promeni jezik.
- README fajlovi su usklađeni sa stvarnim modulima i pokretanjem; ne sadrže stvarne tajne ili lozinke.

## Stanje domena i seed-a

Lokalni seed sadrži šest naloga: `ana.petrovic@museum.local` VISITOR, `milica.jovanovic@museum.local` CASHIER, `nikola.markovic@museum.local` CURATOR, `jelena.stojanovic@museum.local` GUIDE, `ivan.nikolic@museum.local` MANAGER i `marija.ilic@museum.local` ADMINISTRATOR. Lokalna odbrambena lozinka je definisana u `DemoDataSeeder.PASSWORD` i namerno nije prepisana u dokumentaciju.

Seed katalog obuhvata najmanje 3 muzeja, 9 sala, 8 izložbi, 50 eksponata, više autora, buduće i istorijske termine, događaje/ture, cenovnike, prijave, ulaznice, posete i preporuke. Tačan broj nakon pokretanja uvek zavisi od postojeće lokalne baze i idempotentnog seed-a; clean testovi potvrđuju veze i ponovljivost bez brisanja istorije. Lokalni image seed ima 5 open-access asseta i njihove attribution podatke izlaže API; slike su označene kao tematske reference kada nisu fotografija konkretnog izmišljenog eksponata.

Izvori slika su The Metropolitan Museum of Art Open Access (CC0 1.0), sačuvan source URL, autorom i attribution metapodacima; detalji su dostupni kroz `/api/media/...?...attribution=true`.

## Specifikacija i EER

Flyway V1–V3 i JPA implementacija čuvaju kardinalitete kroz FK, unique/composite ključeve i deferred constraint trigere: termin pripada jednoj izložbi, učešće eksponata jednoj izložbi i jednom eksponatu, digitalni sadržaj jednom učešću, ulaznica jednom terminu, poseta najviše jednom ulaznicom, a sken jednoj poseti i učešću. EER slika prvog podsistema grafički prikazuje učešće uz `(0,N)` na strani izložbe, dok implementacija namerno ima `ucesce.izlozba_id NOT NULL` — jedna postavka može imati više učešća, ali svako učešće pripada tačno jednoj izložbi. To je razlika dijagrama i implementacije, nije automatski menjana migracija.

Neo4j projekcija trenutno koristi čvorove `Eksponat`, `Izlozba`, `Autor` i veze `IZLOZEN_NA`, `AUTOR_JE`, `CESTO_SLEDI`. Šira specifikacija navodi dodatne čvorove teme/perioda/civilizacije/istorijskih ličnosti i događaja; implementacija period i tip čuva kao svojstva eksponata, a ne kao posebne čvorove. Projekcija je periodično osvežavanje iz PostgreSQL-a, ne outbox po svakoj transakciji. Kriterijum `BLIZINA` koristi razliku u `redosled_obilaska`; nema stvarnih prostornih koordinata niti metrike udaljenosti.

## Redosled demonstracije

1. Otvoriti katalog i promeniti SR/EN; pokazati prošlu izložbu van javnog browse-a i budući termin.
2. Registrovati/prijaviti posetioca, kupiti termin i pokazati ulaznicu sa izložbom.
3. Prijaviti blagajnika, validirati QR ulaznicu, pokušati ponovnu validaciju i pokazati odbijanje.
4. Kao posetilac otvoriti aktivnu posetu, skenirati QR sa table, pokazati tekst, sliku, audio i preporuke; isključiti Neo4j samo u lokalnoj demonstraciji da se pokaže fallback.
5. Završiti posetu, oceniti je i pokazati istoriju.
6. Redom pokazati Curator sadržaj/QR tablu, Cashier prodaju/validaciju, Guide događaje/prijave, Manager analitiku i Administrator naloge/uloge.
7. Zaštitu potvrditi pokušajem pristupa tuđoj poseti/ulaznici i nedozvoljenoj radnji sa VISITOR nalogom.

## Preostale prepreke

Nema potvrđenog javnog hostinga, domena, TLS sertifikata, realnog payment gateway naloga, produkcionog SMTP-a ili spoljnog TTS naloga. Lokalni `DemoPaymentGateway` je namerno dostupan samo u `local`/`test`; u produkcionom profilu vraća konfiguracionu grešku dok se ne poveže stvarni provider. `health` je liveness endpoint, ne puna readiness provera. Kamera telefona i štampa ostaju ručne provere. Postojeći deployment fajlovi su priprema, ne dokaz da je javna infrastruktura već postavljena.

Nema novih commitova ni push-a.
