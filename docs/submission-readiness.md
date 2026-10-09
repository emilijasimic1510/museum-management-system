# Submission readiness — 2026-10-09

## Conclusion

**Ready subject to the checks listed below.** The recorded audit verified code, tests, and local demonstration flows. Physical phone scanning and public production deployment require additional operational checks. They are not recorded as completed.

This report describes the 2026-10-09 audit. Translating the documentation does not constitute a new application test run.

## Recorded checks

| Command and working directory | Recorded result |
|---|---|
| `mvnw.cmd clean test package` in `backend` | PASS: 182 tests, 0 failures, 0 errors; package and repackaged JAR succeeded |
| `npm test` in `frontend` | PASS: 19 files, 165 tests in the final saved execution log |
| `npx --no-install tsc --noEmit -p tsconfig.app.json` in `frontend` | PASS |
| `npm run build` in `frontend` | PASS; output under `frontend/dist/museum`; existing `qrcode` non-ESM warning |
| `git diff --check` at the repository root | PASS; only standard CRLF conversion warnings recorded |

The earlier report stated 164 frontend tests; the final local log, `tmp/submission-frontend-final.log`, records 165. This correction is based on the saved log, not a new execution.

Testcontainers used isolated PostgreSQL and Neo4j instances. Existing development volumes were not deleted or reset.

The isolated HTTP smoke run recorded in the local `tmp/submission-smoke.ps1` workflow covered registration/login, purchase, rejection of access to another visitor's records, cashier validation and rejection of repeated validation, an active visit, QR scanning, Serbian text, eSpeak WAV audio, an image, recommendations, routes, Manager access, Guide events/registrations, Administrator accounts, visit completion/rating, and rejection of scanning after completion. Stopping Neo4j in the isolated environment returned the curator-order fallback in under 10 seconds.

The browser review covered the Serbian/English homepage, catalog, exhibition details, and available admission times. This is narrower than a complete browser-driven check of every role workflow. The local temporary files are audit artifacts, not a portable test harness guaranteed to exist in every checkout.

## Manual phone and print checks

The audit did not verify a physical phone camera, its permission prompt, or physical printing. Use a trusted HTTPS address on the phone. A computer's localhost URL or ordinary LAN HTTP address is insufficient for mobile camera access.

For a temporary demonstration, run:

```cmd
cloudflared tunnel --url http://localhost:4200
```

Use the same current HTTPS address for the curator on the computer and visitor on the phone. Generate the QR board after selecting the exhibition matching the validated ticket and active visit. A board from localhost or an earlier tunnel address can fail the same-origin check. Regenerate it after a hostname change and update Angular's exact allowed hostname if required.

The full [QR procedure](../README.md#testing-qr-scanning-with-a-phone) includes ticket validation, camera permission, content checks, and tunnel troubleshooting. Repeated exhibit scans are recorded; repeated ticket validation is rejected.

## Fixes recorded during the audit

- The historical-ticket test now creates records with historical timestamps instead of modifying an immutable purchase snapshot.
- The guide seed idempotency test identifies its three events by name and checks result stability rather than relying on an obsolete total count.
- Catalog cards expose a visible localized details link.
- The QR board test checks the actual generated QR image instead of relying on an unstable CommonJS library mock.
- Server error messages with status information are translated again when the language changes.
- README instructions were aligned with the implemented modules and configuration, without publishing real secrets.

## Local accounts and content

| Email | Role |
|---|---|
| ana.petrovic@museum.local | VISITOR |
| milica.jovanovic@museum.local | CASHIER |
| nikola.markovic@museum.local | CURATOR |
| jelena.stojanovic@museum.local | GUIDE |
| ivan.nikolic@museum.local | MANAGER |
| marija.ilic@museum.local | ADMINISTRATOR |

The local password is defined in `DemoDataSeeder.PASSWORD` and is not reproduced here.

The seeded catalog includes at least 3 museums, 9 halls, 8 exhibitions, 50 exhibits, multiple authors, future and historical admission times, events/tours, prices, registrations, tickets, visits, and recommendation data. Actual database totals depend on existing records and idempotent seeding; these are not destructive reset instructions.

The recorded local image seed contains 5 open-access assets with attribution metadata. The source is The Metropolitan Museum of Art Open Access under CC0 1.0. Source URL, author, and attribution are exposed through the media API's attribution query, `/api/media/...?...attribution=true`. Thematic reference images are identified as such rather than presented as photographs of fictional collection objects.

## Specification and EER comparison

Flyway V1–V3 and JPA enforce cardinalities through foreign keys, unique/composite keys, and deferred constraint triggers. An admission time belongs to one exhibition; a participation belongs to one exhibition and one exhibit; content belongs to one participation; a ticket belongs to one admission time; a visit belongs to exactly one ticket, while a ticket has at most one visit; a scan belongs to one visit and one participation.

The subsystem 1 diagram's exhibition–participation notation differs from the implemented required FK `ucesce_eksponata_na_izlozbi.izlozba_id`. An exhibition may have multiple participations, but each participation belongs to exactly one exhibition. The audit reported the discrepancy without automatically modifying the diagram or migration.

The Neo4j projection uses `Eksponat`, `Izlozba`, and `Autor` nodes with `IZLOZEN_NA`, `AUTOR_JE`, and `CESTO_SLEDI` relationships. The broader specification describes additional theme, period, civilization, historical-person, and event nodes. The implementation instead stores period/type as exhibit properties. Projection refresh is periodic rather than driven by a transactional outbox.

The `BLIZINA` criterion uses differences in `redosled_obilaska` (tour order). There are no actual spatial coordinates or measured physical distances.

## Defense demonstration sequence

1. Open the catalog and switch Serbian/English. Show current/future availability and explain why past exhibitions are excluded from public browsing.
2. Register or sign in as a visitor, buy an available admission time, and show the ticket with its exhibition.
3. Sign in as the cashier, validate the ticket, and demonstrate rejection of a second validation.
4. As the visitor, open the active visit, scan an exhibit QR board, and show text, image, prepared audio, and recommendations. Stop Neo4j only in a controlled local demonstration to show fallback.
5. Finish and rate the visit, then show history.
6. Demonstrate Curator content/QR tools, Cashier sales/validation, Guide events/registrations, Manager analytics, and Administrator accounts/roles.
7. Demonstrate rejection of another visitor's ticket/visit access and an operation not permitted to VISITOR.

## Outstanding limits

Public hosting, domain/TLS setup, a real payment gateway, production SMTP delivery, and external TTS service credentials were not established by this audit. Local eSpeak can generate audio without an external TTS account when its executable is installed and configured.

`DemoPaymentGateway` simulates payments only in local/test profiles. Production payment attempts remain unavailable until a real provider is connected. The health endpoint is a liveness check, not full readiness. Deployment files are preparation rather than proof of deployed infrastructure.

Phone camera and physical print checks remain manual. Run the commands in the table above to verify a newer checkout, with Docker available for Testcontainers. No unexecuted check should be reported as passing.

No commit or push was performed as part of the recorded audit.
