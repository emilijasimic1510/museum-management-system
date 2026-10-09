# Frontend

Angular 21.2 / TypeScript 5.9. Implementirani su javni katalog i detalji, SR/EN, registracija/prijava, kupovina i istorija ulaznica, posete, QR kamera, tekst/slika/audio, preporuke i ocenjivanje, kao i Curator, Cashier, Guide, Manager i Administrator radne table.

Iz ovog direktorijuma:

```powershell
npm ci
npm start
npm test
npx --no-install tsc --noEmit -p tsconfig.app.json
npm run build
```

Razvojni URL: http://localhost:4200. Proxy `/api` koristi backend na 127.0.0.1:8081. Build: `dist/museum/browser`. `public/runtime-config.js` podešava API i javni URL QR kodova. Telefon zahteva pouzdan HTTPS i adresu dostupnu sa telefona; `npm run start:https` sa samopotpisanim sertifikatom nije dovoljno bez uspostavljenog poverenja.

Autorizaciju sprovodi i backend, ne samo Angular guard. UI prevodi i mapirani seed prevodi su u `src/app/core/i18n.ts`; nedostajući EN sadržaj vraća SR. Novi proizvoljan sadržaj se ne prevodi automatski.

Podešavanja baza, JWT, TTS i tok pokretanja: [glavni README](../README.md). Aktuelni rezultati i ručne provere: [spremnost za predaju](../docs/submission-readiness.md).
