# Production deployment

The repository includes backend/frontend Dockerfiles, a Caddy reverse proxy configuration, and `compose.production.yaml`. These files are deployment preparation, not evidence of an operational public installation.

## Preparation

Provide a server with Docker Compose, a hostname pointing to it, and inbound ports 80 and 443. Configure private credentials and persistent storage. Build and verify the application before deployment, including the backend JAR required by the Docker build.

From the repository root on the server:

```sh
cp .env.production.example .env.production
# Populate the file with environment-specific settings and independently generated secrets.
docker compose --env-file .env.production -f compose.production.yaml up -d --build
```

Do not overwrite an existing private configuration file. Review the Dockerfiles and Compose configuration for the selected host before running them.

## Configuration

- Serve the frontend and `/api` through the same HTTPS origin. Use that public origin when generating exhibit QR boards.
- Set a private `JWT_SECRET` with at least 32 UTF-8 bytes and supply it to the backend process.
- Keep PostgreSQL and Neo4j credentials private, restrict database network exposure, and persist their data and uploaded media.
- Configure DNS, TLS certificate storage/renewal, backups, and a tested restore procedure.
- Import only approved content and accounts. The production profile does not run local seed data or enable local payment simulation and Swagger access.
- Connect a real payment provider before accepting real card payments.
- Configure SMTP if email delivery is required. Configure and provision the chosen TTS provider; selecting a provider variable alone does not install its executable, models, or credentials.

## Verification

```sh
docker compose --env-file .env.production -f compose.production.yaml ps
curl --fail https://YOUR_DOMAIN/api/health
```

Replace `YOUR_DOMAIN` with the configured hostname. Health is a liveness endpoint, not a full dependency readiness check. Verify database connectivity, authentication, role restrictions, media delivery, payment behavior, backup recovery, and phone scanning separately.

Do not commit `.env.production`, passwords, tokens, or private keys. See the [submission readiness report](submission-readiness.md) for the limits of the recorded local verification.
