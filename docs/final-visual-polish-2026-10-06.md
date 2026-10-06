# Final visual polish — 2026-10-06

Vizuelna izmena je ograničena na postojeći frontend CSS. Nisu menjani business rules, backend, API ugovori, ticket/visit, QR/security, audio/image semantika ili baza.

Usklađeni su zajednički spacing i typography helpers za page heading, panel, buttons, badges, messages, staff akcije i prazna/loading stanja. Manager tabele imaju čitljiv padding, minimalnu širinu i horizontalni wrapper na manjim ekranima. Admin role/action blokovi i collection/guide/curator forme dobijaju konzistentne razmake i wrap dugih labela. Scan/visit blokovi, route stavke, audio player i slike ostaju unutar viewport-a; mobilni breakpoint koristi pune, lako klikabilne akcije.

Posebno su proverene strukture za screenshot stranice: login/register, profile, catalog/exhibition/exhibit, tickets, visits/visit detail, scan, recommendations, cashier, curator, guide, collection, manager/admin i QR board. QR board postojeći A4/2-column print CSS nije menjan sem zajedničkih overflow pravila.

TypeScript `noEmit`: PASS. `git diff --check`: PASS.

Relevantni Angular testovi i production build nisu mogli da se izvrše u ovom sandboxu: Angular builder ne može da čita workspace roditeljske putanje (`Access is denied`) i zato prijavljuje nerešene `src/styles.css`, `main.ts`, QR CSS i test fajlove. To je environment problem, ne potvrđena aplikaciona greška. Prethodno potvrđeni relevantni skup ostaje 39/39 PASS.

Pre finalne demo spremnosti ostaje lokalni `npm test` i `npm run build` na normalnom Node/Angular toolchain-u, zatim fizička provera 360–430px i print preview QR board-a.
