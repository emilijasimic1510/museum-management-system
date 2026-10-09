# Local seed data

Local seed classes initialize demonstration content without changing Flyway V1–V3. `DemoDataSeeder` is restricted to local/development use; role-specific seed additions may require the `local` profile. Use the documented local startup rather than enabling seed data on a public server.

Passwords are stored as BCrypt hashes. Natural keys and conflict handling make repeated startup idempotent. Restarting is not a database reset and must preserve existing history.

## Accounts

| Email | Role | Purpose |
|---|---|---|
| ana.petrovic@museum.local | VISITOR | Tickets, visits, content, and recommendations |
| milica.jovanovic@museum.local | CASHIER | Counter sales and ticket validation |
| nikola.markovic@museum.local | CURATOR | Collections, participation, content, and QR boards |
| jelena.stojanovic@museum.local | GUIDE | Events and registrations |
| ivan.nikolic@museum.local | MANAGER | Aggregate reports |
| marija.ilic@museum.local | ADMINISTRATOR | Accounts, roles, and employees |

The local password is defined in `DemoDataSeeder.PASSWORD`. Email is the login identifier. These local addresses are not intended for external email delivery. Use the account for the role being demonstrated instead of relying on historical combined-role accounts.

## Content and identifiers

The richer local catalog includes museums, halls, exhibitions, exhibits, authors, prices, admission times, events, registrations, tickets, and visit history. See the [submission audit](../docs/submission-readiness.md#local-accounts-and-content) for recorded minimum coverage.

IDs depend on the existing database. Do not assume that a record has a particular numeric ID or reuse an old QR token. Read the current catalog or generate the current exhibition QR board.

The following read-only SQL can be run in an authenticated PostgreSQL client:

```sql
SELECT id, naziv FROM muzej ORDER BY id;
SELECT id, naziv, datum_pocetka, datum_zavrsetka FROM izlozba ORDER BY id;
SELECT id, izlozba_id, pocetak, kraj, kapacitet, status
FROM termin_izlozbe ORDER BY pocetak;
SELECT izlozba_id, datum_vazenja_od, kategorija, cena
FROM cenovnik_izlozbe ORDER BY izlozba_id, datum_vazenja_od;
```

Do not publish QR tokens or credentials in logs or screenshots.

## Startup

Follow the [root setup guide](../README.md#running-on-windows) for Docker, Java, JWT, and TTS configuration. Start the backend with:

```cmd
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

Seeding runs after migrations and Hibernate validation. Restart the backend normally to rerun idempotent seed logic; do not delete Docker volumes. Existing event times and history are not a guarantee of current availability. Select a currently valid admission time or manage the schedule through the authorized workspace.

## Demonstration sequence

1. Browse museums, exhibitions, admission times, and prices through the UI or local Swagger.
2. Sign in as the visitor and buy an available admission time using its actual ID and an available price category.
3. As the cashier, validate the ticket QR token for the same `termId`; this creates the active visit.
4. As the curator, generate the QR board for that exhibition from the same HTTPS origin used on the phone.
5. As the visitor, open the active visit and scan a participation from that board. Confirm actual text, image, and audio delivery.
6. Generate a route using a supported criterion such as `KOMBINACIJA`, inspect recommendations, and show scan history.
7. Finish and rate the visit. Confirm that historical records remain available and further scanning is blocked.

The business flow uses actual backend transactions, capacity checks, ticket state transitions, and PostgreSQL constraints. The local payment adapter does not charge real money. Direct API token submission tests backend scanning only; follow the [phone camera procedure](../README.md#testing-qr-scanning-with-a-phone) to verify physical scanning.
