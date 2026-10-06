# Mobilni LAN i QR demo — 2026-10-05

## Šta je postojalo

Backend je već imao nepromenjeni secured endpoint `POST /api/visits/{visitId}/scans`, opaque `ExhibitParticipation.qrToken`, proveru VISITOR uloge, vlasništva aktivne posete, izložbe i dostupnosti postavke. Frontend je imao `/scan?token=...`, izbor aktivne posete i postojeći `VisitDetailPage` koji poziva backend scan endpoint. Login guard je već čuvao `state.url` kao `returnUrl`.

## Dodato

- Angular dev server sada sluša na `0.0.0.0:4200`.
- Local Spring profil sada sluša na `0.0.0.0:8081` (može se promeniti preko `MUSEUM_BIND_ADDRESS`).
- Runtime konfiguracija `frontend/public/runtime-config.js` podržava `apiBaseUrl` i `publicBaseUrl` kroz `window.__MUSEUM_CONFIG__`.
- API klijent koristi konfigurabilni base URL; podrazumevano ostaje `/api`, pa Angular proxy i postojeći testovi rade isto.
- QR generisanje za curator i lokalnu demo tablu sada kodira URL oblika `/scan/<opaque-token>`.
- Frontend prihvata i novu path rutu `/scan/:token` i staru query formu radi kompatibilnosti.
- Ako postoji tačno jedna aktivna poseta, QR flow automatski otvara detalj te posete sa tokenom; postojeći `VisitDetailPage` zatim poziva secured backend scan.
- Local CORS je ograničen konfigurisanom listom origin-a, sa `Authorization`, `Content-Type` i `X-Museum-Language` headerima. Production security profil ne dobija wildcard CORS.

Nema promene baze, QR token modela, ticket/visit pravila, authorization provera, content endpointa, audio/image/email logike ili Neo4j preporuka.

## Konfiguracija za telefon

Na laptopu pronađite Wi-Fi IPv4 adresu:

```powershell
ipconfig
```

Izaberite IPv4 adresu aktivnog Wi-Fi adaptera, na primer `192.168.1.23`. Ne upisujte DHCP adresu u bazu; koristite je samo za lokalni runtime.

U `frontend/public/runtime-config.js` za demo možete postaviti:

```js
window.__MUSEUM_CONFIG__ = {
  publicBaseUrl: 'http://192.168.1.23:4200',
  apiBaseUrl: '/api'
};
```

`publicBaseUrl` određuje URL koji se upisuje u QR kod. `apiBaseUrl: '/api'` koristi Angular dev proxy i zato je najjednostavniji LAN režim. Ako frontend servira drugi host, može se postaviti direktan URL, na primer `http://192.168.1.23:8081/api`; tada backend `MUSEUM_ALLOWED_ORIGINS` mora sadržati origin frontenda.

Backend, iz `backend` direktorijuma:

```powershell
$env:JWT_SECRET='najmanje-32-znaka-samo-za-local-demo'
$env:MUSEUM_BIND_ADDRESS='0.0.0.0'
$env:MUSEUM_ALLOWED_ORIGINS='http://localhost:4200,http://127.0.0.1:4200,http://192.168.1.23:4200'
./mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

Frontend, iz `frontend` direktorijuma:

```powershell
npm start
```

Na telefonu otvorite `http://192.168.1.23:4200`. Telefon i laptop moraju biti na istoj mreži. Windows Defender Firewall može tražiti inbound TCP pravila za portove **4200** (Angular) i **8081** (backend, samo ako se koristi direktan API base URL). Za proxy režim dovoljan je port 4200 iz perspektive telefona; laptop Angular proces pristupa backendu lokalno.

## QR tok

Curator QR prikaz i local `/demo-qr` tabla generišu URL poput:

```text
http://192.168.1.23:4200/scan/DEMO-CITY-QR-001
```

U URL-u je samo postojeći opaque token postavke. Nema user ID-a, visit ID-a ili ličnih podataka. QR se ne čuva u bazi kao URL; token u bazi ostaje postojeći server-generated token.

1. Telefon otvori `/scan/<token>`.
2. Ako korisnik nije prijavljen, `visitorGuard` vodi na `/login?returnUrl=/scan/<token>`.
3. Posle uspešnog logina postojeći `safeReturnUrl` vraća korisnika na originalni scan URL.
4. Frontend učitava samo sopstvene aktivne posete. Ako postoji jedna, prosleđuje token detalju posete; ako ih ima više, korisnik bira posetu. Ako nema aktivne posete, prikazuje jasnu poruku i link ka ulaznicama.
5. Scan se evidentira isključivo preko postojećeg `POST /api/visits/{id}/scans`. Backend ostaje source of truth.
6. Samo uspešan secured scan otključava contextual tekst, zaštićenu sliku, audio i sledeću preporuku. QR URL nikada direktno ne vraća contextual sadržaj.

Nevažeći token backend odbija. Tuđa poseta, tuđa izložba, završena/istekla poseta i pokušaj direktnog content URL-a ostaju zaštićeni postojećim pravilima.

## Fallback i ograničenja

Ako runtime konfiguracija nije postavljena, API je relativni `/api`, javni QR base je trenutni browser origin, a lokalni server fallback je `0.0.0.0`. To je bezbednije od upisivanja konkretnog DHCP IP-a u kod ili bazu, ali QR generisan dok se aplikacija otvara preko `localhost` neće raditi na telefonu. Za telefonski demo obavezno postavite `publicBaseUrl` na laptop LAN adresu.

HTTPS domen može koristiti isti format u production okruženju. Dev server nije produkcioni reverse proxy i ne treba ga izlagati van pouzdane lokalne mreže.

## Provere

Dodati su unit testovi za path/query token parsing i configurable QR URL. Raniji frontend auth/scan testovi ostaju relevantni. Pokrenuti završno: relevantne Angular testove, TypeScript `noEmit`, production build, backend scan/integration testove i `git diff --check`.
