# Produkcioni deployment

Repozitorijum sada sadrži produkcioni paket: `backend/Dockerfile`, `frontend/Dockerfile`, `frontend/Caddyfile` i `compose.production.yaml`.

Na serveru sa domenom i otvorenim portovima 80/443:

```bash
cp .env.production.example .env.production
# popuniti .env.production stvarnim, nasumičnim secrets vrednostima
docker compose --env-file .env.production -f compose.production.yaml up -d --build
```

Frontend prosleđuje `/api` backendu kroz isti origin, pa kamera na telefonu dobija HTTPS kontekst kada domen dobije sertifikat. PostgreSQL, Neo4j i media storage koriste persistent Docker volume-e; baze nisu objavljene na javnim portovima.

Pre prvog javnog puštanja potrebno je uraditi inicijalni import odobrenih seed podataka u produkcionu bazu, podesiti DNS domena ka serveru i napraviti prvi backup. Produkcioni profil namerno ne pokreće lokalne seedere, lokalni QR board ili Swagger.

Minimalna provera posle deploymenta:

```bash
docker compose --env-file .env.production -f compose.production.yaml ps
curl https://your-domain.example/api/health
```

Ne upisivati `.env.production` u Git i ne koristiti razvojni `JWT_SECRET` na javnom serveru.
