# Finalni SR/EN i18n audit — 2026-10-06

Frontend audit je obuhvatio stranice za posetioce, QR/scan, kustosa, vodiča, blagajnika, menadžera i administratora. Pronađeno je 728 jedinstvenih korisničkih tekstova koji su objedinjeni u `frontend/src/app/core/i18n.ts`; 911 pojavljivanja je zamenjeno centralnim ključevima. Statusi, uloge, načini plaćanja i tipovi digitalnog sadržaja koriste centralne mape/pipes.

Podrazumevani jezik ostaje srpski. Engleski prikaz koristi engleski ključ, a nedostajući engleski prevod pada na srpski. Tehničke vrednosti, ID-jevi, tokeni, URL-ovi i sadržaj iz baze ostaju neprevedeni. Backend poruke se ne prikazuju direktno; poznati HTTP statusi imaju lokalizovane poruke, dok se original greške zadržava u logovima.

Očišćene su ključne stranice: curator editor/content/authorships/overview/list, guide event editor/overview/registrations, collection editor/list/prices, manager, admin, cashier, visitor, scan, QR board, audio/image states i postojeći event/catalog/ticket/visit prikazi. Datumi i brojevi sada koriste centralni locale (`sr-Latn-RS` / `en-US`) kroz i18n pipe/helper.

Namerni tekstovi koji ostaju literalni su tehnički kodovi (`SR`, `EN`, role/status enum vrednosti u select opcijama), korisnički podaci iz API-ja, putanje, tokeni, ID-jevi i formatni obrasci HTML kontrola. Oni nisu UI prevodi.

Stabilization / regression audit:

`visitor-content.spec.ts` ima jedan poznat nezavršen zahtev: kada drugi odgovor otključa tekstualni sadržaj, `VisitorText` legitimno poziva zaštićeni endpoint `/api/visits/3/content/10/text/1`. Test je ranije proveravao samo metadata odgovor i nije flush-ovao taj zahtev; zato je teardown prijavljivao otvoren request, a naredni testovi su kaskadno padali. Ovo je test fixture problem, ne promena autorizacije niti dodatni business request. Minimalno usklađivanje testa treba da flush-uje očekivani tekstualni odgovor pre `settle`.

Centralni integrity test sada proverava kompatibilan SR/EN skup ključeva, duplikate, fallback bez raw ključa i mapiranje role/payment/content enum vrednosti. API enum vrednosti ostaju nepromenjene.

Provere:

- `tsc --noEmit -p tsconfig.app.json`: PASS.
- Production build i full frontend test trenutno nisu potvrđeni jer je lokalni Angular builder u sandboxu odbio čitanje workspace putanja; ponovni escalated zahtev je odbijen zbog usage limita. Pre toga je postojeći relevantni QR/auth/scan/curator skup imao 39/39 PASS. TypeScript kompilacija posle stabilizacije ponovo prolazi.
- `git diff --check`: treba ponoviti nakon završne provere; nema backend izmena u ovom zadatku.

Pre finalnog commit-a ostaje vizuelna provera SR/EN na svakoj radnoj stranici, naročito dužih labela u admin/manager tabelama, kao i production build/test na lokalnom toolchain-u sa normalnim filesystem pristupom.
