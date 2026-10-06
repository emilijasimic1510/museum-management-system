# QR kartice za štampu — 2026-10-06

## Audit i pristup

Postojeći `ParticipationQr` koristi biblioteku qrcode i centralni `scanUrl(token)` koji čita runtime `publicBaseUrl`. Kustos je već mogao da vidi pojedinačni QR na editoru postavke; posebni download/PDF endpoint nije postojao. Lokalna demo tabla takođe postoji, ali nije opšta štampana tabla sa izborom muzeja/izložbe.

Nova stranica je **`/staff/qr-board`**, sa linkom **QR kartice za štampu / Printable QR cards** u navigaciji za **CURATOR** i **ADMINISTRATOR**. Koristi poseban frontend guard koji odgovara postojećim backend pravima za `/api/exhibit-participations`. MANAGER, GUIDE, CASHIER i VISITOR bez jedne od tih uloga nemaju pristup. Anoniman pristup ide na login. Nakon prijave zaposleni može otvoriti link iz svoje navigacije.

Backend, baza, tokeni, scan flow i poslovna pravila nisu menjani. Nema nove QR/PDF biblioteke niti dupliranja QR generatora. Proširen je postojeći `ParticipationQr` komponentni prikaz opcionalnim prevodivim tekstovima i događajem spremnosti slike za štampu.

## Izbor i pregled

1. Izaberite muzej. Izložbe se povezuju preko postojeće veze izložba → sala → muzej.
2. Izaberite izložbu. Stranica koristi zaštićeni `GET /api/exhibit-participations?exhibitionId=...` i zadržava samo postavke te izložbe koje su trenutno instalirane: `installedAt` je prazan ili nije u budućnosti, a `removedAt` je prazan ili je u budućnosti. To je filter za štampu; backend ostaje autoritet za kasniji scan.
3. Označite postavke ili koristite Izaberi sve / Poništi izbor. Prikazuje se broj izabranih kartica. Lista ima skrol kada je dugačka.
4. Kliknite Pripremi pregled. Izbor se prikazuje po četiri kartice na stranici, dve u redu. Promena muzeja, izložbe ili izbora uklanja prethodni pregled da ne biste odštampali zastarele kartice.
5. Sačekajte QR slike. Dugme za štampu postaje dostupno tek kada su sve odabrane QR slike učitane. Neuspeh generisanja ima poruku; ponovo pripremite pregled.

Kartica prikazuje naziv izložbe, eksponata, opcioni redosled obilaska i tekst „Skenirajte QR kod za digitalni sadržaj” / “Scan the QR code for digital content”. Nema raw tokena, internih ID vrednosti ni API URL-ova u vidljivom sadržaju. Redosled obilaska je korisnički podatak, nije interni ID.

Sav novi UI koristi centralni I18n. Modeli eksponata i izložbe trenutno imaju jedinstveni `name`, bez odvojenih SR/EN naziva; koristi se taj postojeći naziv u oba jezika. TEKST/AUDIO prevodi ostaju deo postojećeg visitor sadržaja i nisu deo QR kartice.

## Javna adresa i demo

Pre štampe podesite postojeći `frontend/public/runtime-config.js`:

```js
window.__MUSEUM_CONFIG__ = {
  publicBaseUrl: 'https://vas-muzejski-domen.example',
  apiBaseUrl: '/api'
};
```

Za lokalnu odbranu umesto tog primera unesite stvarni `http://<LAN-IP-laptopa>:4200`. IP se ne upisuje u bazu niti u izvorni kod generatora. QR koristi isti format `<publicBaseUrl>/scan/<URL-encoded-opaque-token>` kao ranije. Bez konfiguracije pojavljuje se jasno upozorenje pre štampe; postojeći fallback je origin pregledača. QR sa localhost adresom nije dostupan sa telefona. Nakon promene runtime fajla ponovo učitajte stranicu i pripremite pregled.

Pokrenite backend sa postojećim `local` profilom i konfiguracijom baze/JWT-a, frontend sa `npm start`, pa otvorite `/staff/qr-board` kao kustos ili administrator. Detaljni LAN/Firewall koraci nalaze se u `docs/mobile-lan-qr-2026-10-05.md`.

## A4 / PDF

Kliknite Štampaj / Sačuvaj kao PDF. U browser dijalogu izaberite A4, portrait, 100% i štampač ili Save as PDF. Isključite browser headers/footers da browser ne doda datum i URL. CSS postavlja margine od 10 mm, crn tekst/QR na beloj podlozi, QR širine 55 mm, dve kolone, do četiri kartice po grupi i zabranu presecanja pojedinačne kartice. Dugi nazivi se prelamaju; proverite browser preview kod neuobičajeno dugih naziva jer mogu povećati broj fizičkih strana.

`@media print` skriva zaglavlje, navigaciju, footer i kontrole samo kada je QR board otvoren. Ne traži background graphics za crno-belu štampu. Nedovršen pregled QR slika se ne štampa. Na uskom ekranu pregled koristi jednu kolonu, dok print uvek koristi dve.

Za odbranu prvo testirajte jedan QR kamerom telefona na istoj mreži, zatim odštampajte ostatak. Posetilac mora biti prijavljen i imati aktivnu posetu nakon validacije karte. QR samo otvara postojeći scan flow; ne daje pristup privatnom tekstu, slici ili audiju i ne zaobilazi vlasništvo, aktivnu posetu ni proveru izložbe. Postojeće ponašanje scan stranice nije menjano ovim zadatkom.

## Provere

- Relevantni frontend testovi: **39/39 PASS** (`qr-board.spec.ts`, `scan.spec.ts`, `auth.spec.ts`, `curator.spec.ts`). Obuhvataju izbor izložbe/postavki, SR/EN, URL/config fallback upozorenje, pristup ulogama, spremnost slika i print CSS koji skriva navigaciju. Očekivanja postojeće navigacije dopunjena su novim linkom.
- `node node_modules/typescript/bin/tsc --noEmit -p tsconfig.app.json`: **PASS**.
- `npm run build`: **PASS**. Ostaju upozorenja za postojeće neiskorišćene TranslatePipe importe i CommonJS biblioteku qrcode.
- `git diff --check`: **PASS**.
- Dodatno pokrenuti postojeći `visitor-content.spec.ts` nije prošao: prvi test očekuje odmah prikazan tekst, ali ostavlja neobrađen `GET /api/visits/3/content/10/text/1`; teardown greška zatim prekida naredne testove (5 neuspešnih ukupno). Taj test i text/audio implementacija nisu menjani u ovom zadatku. Puna frontend regresija zato nije potvrđena.

Backend nije menjan pa njegovi testovi nisu ponavljani. Print CSS je proveren komponentnim testom, ne fizičkim štampanjem. Browser print preview, neuobičajeno dugi nazivi i skeniranje telefonom zahtevaju završnu proveru na konkretnoj LAN mreži i štampaču. Nema commit-a ni push-a.
