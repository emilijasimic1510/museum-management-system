# Frontend

Angular 21.2 and TypeScript 5.9 application for public catalog browsing, visitor ticketing and digital guidance, and the Curator, Cashier, Guide, Manager, and Administrator workspaces.

## Run

Start the backend and its databases as described in the [root setup guide](../README.md). From this directory:

```cmd
npm ci
npm start
```

Open [the application](http://localhost:4200). The development proxy forwards `/api` to `127.0.0.1:8081`; adjust `proxy.conf.json` when changing the backend port.

## Verify

```cmd
npm test
npx --no-install tsc --noEmit -p tsconfig.app.json
npm run build
```

The production output is `dist/museum/browser`.

## Configuration and content

UI translations are centralized in `src/app/core/i18n.ts`. English content uses an available translation and otherwise falls back to Serbian. Database content is not automatically translated.

`public/runtime-config.js` provides deployment API/public URL settings. Phone scanning requires a trusted HTTPS address. Use the same origin for the visitor application and the curator QR board; regenerate the board when a temporary tunnel address changes. Follow the [phone QR testing procedure](../README.md#testing-qr-scanning-with-a-phone), including the Angular allowed-host setting.

See the [curator guide](CURATOR.md) and [recorded submission audit](../docs/submission-readiness.md).
