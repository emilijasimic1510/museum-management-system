# Museum Management System

An information system for museum collections, exhibitions, accounts, events, ticket sales, and digital visitor guidance. The thesis project implements all three subsystems: the catalog; users, employees, and events; and sales, visits, and recommendations.

The stack comprises Angular 21.2, TypeScript 5.9, Java 21, Spring Boot 3.5.16, Maven Wrapper 3.9.11, PostgreSQL 17.9, and Neo4j 5.26. PostgreSQL is the source of business data; Neo4j is a derived recommendation projection. Flyway V1–V3 manages the schema, while Hibernate validates it.

## Implemented features

- Public museum, exhibition, exhibit, and author catalogs, with filters and available admission times.
- Registration, JWT login, visitor profiles, recommendation consent, and an API for managing visitor interests.
- Ticket purchases, payment records, counter sales, and single-use ticket validation; active visits, exhibit participation QR scanning, digital text/images/audio, visit completion, and ratings.
- Curator tools for collections, exhibition participation, contextual content, prices, admission times, and QR boards. Cashier sales and validation. Guide events and registrations. Manager aggregate reports. Administrator accounts, roles, and employees.
- Serbian/English UI, versioned text and generated audio, image storage, and optional SMTP notifications.
- Neo4j ranking with a PostgreSQL fallback when the graph, useful data, or consent is unavailable. The `BLIZINA` criterion uses differences in tour order, not meters or spatial coordinates.

The `local` profile enforces authentication and role permissions. Local seed data and simulated payments support demonstrations; no real public payment processor is connected.

## Running on Windows

Requirements: Java 21 on PATH, Node.js 22.12+ compatible with Angular 21, npm, and Docker Desktop using Linux containers. Install eSpeak NG or configure another implemented TTS provider to generate actual audio.

From the project root in PowerShell:

```powershell
# Create the file only if it does not already exist.
if (!(Test-Path .env)) { Copy-Item .env.example .env }
notepad .env
docker compose up -d
```

Set your own `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT` (default 15432), `NEO4J_USERNAME` (neo4j), `NEO4J_PASSWORD`, `NEO4J_HTTP_PORT` (7474), `NEO4J_BOLT_PORT` (7687), and `BACKEND_PORT` (8081). Existing Docker volumes retain their original database credentials: editing `.env` alone does not change an existing database password. Do not use `docker compose down -v` for routine startup.

In the same terminal, prepare the JWT key and start the backend:

```powershell
$jwtBytes = New-Object byte[] 48
$jwtRng = [Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRng.GetBytes($jwtBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
$jwtRng.Dispose()
$env:MUSEUM_TTS_PROVIDER = 'espeak'
$env:MUSEUM_TTS_ESPEAK_PATH = 'C:\Program Files\eSpeak NG\espeak-ng.exe'
cd backend
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
```

`JWT_SECRET` is read directly from the process environment; adding it only to `.env` is insufficient. It must contain at least 32 UTF-8 bytes. This example generates a new key without displaying it; changing the key invalidates previous JWT sessions. For persistent environments, keep and reuse a private key outside the repository. Never commit secrets.

In a second terminal, starting from the project root:

```powershell
cd frontend
npm ci
npm start
```

Open [the application](http://localhost:4200). Angular proxies `/api` to `127.0.0.1:8081` through `frontend/proxy.conf.json`. Update the proxy if the backend port changes. The local [health endpoint](http://localhost:8081/api/health) is a liveness check, not a complete dependency readiness check. [Swagger UI](http://localhost:8081/swagger-ui.html) is available in the local profile.

### Local accounts

| Email | Role |
|---|---|
| ana.petrovic@museum.local | VISITOR |
| milica.jovanovic@museum.local | CASHIER |
| nikola.markovic@museum.local | CURATOR |
| jelena.stojanovic@museum.local | GUIDE |
| ivan.nikolic@museum.local | MANAGER |
| marija.ilic@museum.local | ADMINISTRATOR |

The local password is defined in `DemoDataSeeder.PASSWORD` and is not reproduced here. Registration creates a new visitor account. Do not expose local seed accounts on a public server.

## Camera, media, and languages

Phone camera access requires permission and a trusted HTTPS origin. An ordinary `http://computer-IP:4200` address is insufficient; replacing `http` with `https` does not configure TLS. An HTTPS reverse proxy or tunnel must forward both the frontend and `/api`. `frontend/public/runtime-config.js` controls the API and public QR URL configuration. A QR URL used on a phone must not contain `localhost`. A self-signed certificate requires explicit trust on the phone.

### Testing QR scanning with a phone

A scan identifies an exhibit's participation in a particular exhibition. The ticket, active visit, and QR board must refer to the same exhibition. Sharing a museum is not sufficient.

1. Start the databases, backend, and frontend. Keep their terminals running.
2. In a separate CMD window, start the HTTPS tunnel:

   ```cmd
   cloudflared tunnel --url http://localhost:4200
   ```

3. Copy the current `https://....trycloudflare.com` address printed by Cloudflare. Open this same address on both the computer and phone.
4. If Angular reports **Blocked request**, add the exact current hostname, without `https://` or a path, to `serve.options.allowedHosts` in `frontend/angular.json`. Preserve existing entries and restart `npm start`. The string `"all"` is not a wildcard hostname.
5. As a visitor, purchase a ticket for an admission time that can currently be validated. As a cashier, validate that ticket for the matching admission time. This creates the active visit.
6. In a separate browser session on the computer, sign in as the curator using the same HTTPS address. Open the QR board, select the museum and the exhibition matching the visitor's ticket, and generate the preview. Print it or display it on another screen.
7. On the phone, sign in as the visitor, open the active visit, select **Scan exhibit**, allow camera access, and scan an exhibit QR code from the newly generated board.
8. Confirm that the exhibit title, contextual text, image, and prepared audio are available. Repeated exhibit scans are recorded; repeated ticket validation is rejected. Scanning after the visit has finished is not allowed.

A board generated from `http://localhost:4200` or an old tunnel URL can fail the frontend's same-origin QR URL check. The message about the QR URL belonging to this museum can therefore indicate a hostname mismatch. A QR participation from another exhibition also fails the separate backend exhibition check.

If restarting the tunnel produces a new hostname, use that new address on both devices, update the allowed host if needed, and regenerate the QR board. The tunnel remains available only while its process is running. A direct API request with a QR token can test backend behavior, but does not test the camera. The visitor scanner does not require a manual URL field.

A computer camera can also be tested through the active visit's **Scan exhibit** action, with the QR code displayed on another device.

### Content and TTS

Digital content belongs to an exhibit participation in a specific exhibition. Visitor access requires the appropriate ticket and visit flow. The curator saves text and then explicitly generates audio; saving text alone does not generate audio.

For eSpeak, configure `MUSEUM_TTS_PROVIDER=espeak` and `MUSEUM_TTS_ESPEAK_PATH`. Alternatives are `azure` with `MUSEUM_TTS_AZURE_KEY`, `MUSEUM_TTS_AZURE_REGION`, and `MUSEUM_TTS_AZURE_VOICE`, or `piper` with `MUSEUM_TTS_PIPER_PYTHON` and `MUSEUM_TTS_PIPER_MODEL`. The executable, model, or service voice must actually be available. Storage settings are `MUSEUM_AUDIO_STORAGE_PATH`, `MUSEUM_TEXT_STORAGE_PATH`, and `MUSEUM_IMAGE_STORAGE_PATH`; their defaults resolve under `backend/runtime/...` when started from the backend directory.

UI translations are maintained in `frontend/src/app/core/i18n.ts`. Serbian mode uses Serbian content; English mode uses an available English translation and falls back to Serbian when it is absent. Some seed translations are mapped in the frontend dictionary; this is not an automatic translator for arbitrary new content.

SMTP is disabled until `MUSEUM_MAIL_ENABLED=true`. Configure the host, port, private credentials, sender, and TLS through the `MUSEUM_MAIL_*` variables in `application.yml`. Successful business operations alone do not demonstrate email delivery.

## Verification

Docker Desktop must be running. Backend tests use a separate PostgreSQL Testcontainer, and graph tests use a separate Neo4j container. They do not reset the local demonstration database. With a process-level `JWT_SECRET` configured, run from the backend directory:

```powershell
.\mvnw.cmd clean test package
```

From the frontend directory:

```powershell
npm test
npx --no-install tsc --noEmit -p tsconfig.app.json
npm run build
```

The packaged backend is `backend/target/museum-backend-*.jar`; the frontend output is `frontend/dist/museum/browser`. A production Angular build does not mean the application is publicly deployed. Deployment files alone do not establish production readiness.

See the [submission readiness report](docs/submission-readiness.md) for the recorded audit results, specification/EER differences, outstanding manual checks, and defense sequence. Dated reports describe their respective snapshots and do not prove that a later revision passes.
