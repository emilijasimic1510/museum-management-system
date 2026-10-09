# Relational model — subsystem 2

Sources: specification sections 3, 8–10, and 12; the subsystem 2 and combined EER diagrams. V1 and the approved source documents remain unchanged.

## Mapping

Standalone IDs use bigint identity. Process timestamps use timestamptz/Java Instant; employment dates use date/LocalDate. Text uses text, enums varchar(32), and flags boolean. All primary-key columns are NOT NULL. SQL identifiers retain their implemented names.

| Entity / table | Columns | PK | FK | UNIQUE | Nullability | CHECK | Cardinality | ON DELETE |
|---|---|---|---|---|---|---|---|---|
| Account / korisnicki_nalog | id, email, lozinka_hash, aktivan, datum_registracije, poslednja_prijava | id | — | email, lower(email) | Last login optional; others required | BCrypt format | 1..N roles; 0..1 visitor and 0..1 employee | RESTRICT for dependent profiles/assignments |
| Role / uloga | id, naziv, opis | id | — | naziv | Description optional | Six supported role names | 0..N assignments | RESTRICT |
| Role assignment / nalog_uloga | nalog_id, uloga_id, datum_dodele, dodelio_id | (nalog_id, uloga_id) | Account, role, optional assigning account | PK | Assigning account optional | At least one role per account, deferred trigger | M:N plus optional assigner | RESTRICT; assigner SET NULL |
| Visitor / posetilac | id, nalog_id, ime, prezime, telefon, saglasnost_za_preporuke | id | nalog_id | nalog_id | Phone optional; others required | — | Exactly 1 account; account has 0..1 visitor profile | RESTRICT |
| Employee / zaposleni | id, nalog_id, muzej_id, ime, prezime, telefon, datum_zaposlenja, tip_zaposlenog, nadredjeni_id | id | Account, V1 museum, employee | nalog_id | Phone and supervisor optional | Five subtypes; no self-supervision | Exactly 1 account/museum/subtype; 0..1 supervisor; 0..N subordinates | RESTRICT |
| Curator / kustos | zaposleni_id, specijalizacija | zaposleni_id | Employee | PK | Specialization optional | IS-A trigger | Exactly 1 supertype | RESTRICT |
| Guide / vodic | zaposleni_id, primarni_jezik | zaposleni_id | Employee | PK | Primary language optional | IS-A trigger | Exactly 1 supertype; 0..N events | RESTRICT |
| Cashier / blagajnik | zaposleni_id, oznaka_saltera | zaposleni_id | Employee | PK | Counter designation optional | IS-A trigger | Exactly 1 supertype | RESTRICT |
| Manager / menadzer | zaposleni_id, nivo_izvestavanja | zaposleni_id | Employee | PK | Reporting level optional | IS-A trigger | Exactly 1 supertype | RESTRICT |
| Administrator / administrator | zaposleni_id, nivo_administracije | zaposleni_id | Employee | PK | Administration level optional | IS-A trigger | Exactly 1 supertype | RESTRICT |
| Interest / interesovanje | id, naziv, opis | id | — | naziv | Description optional | — | 0..N visitors | RESTRICT |
| Visitor interest / posetilac_interesovanje | posetilac_id, interesovanje_id, nivo | (posetilac_id, interesovanje_id) | Visitor, interest | PK | Level optional | No arbitrary level range | M:N, 0..N on both sides | RESTRICT |
| Event / dogadjaj | id, izlozba_id, vodic_id, naziv, tip, pocetak, kraj, kapacitet, jezik, status | id | V1 exhibition, guide | — | Guide and language optional; others required | Type/status, valid interval, nonnegative capacity | Exactly 1 exhibition; 0..1 guide; 0..N registrations | RESTRICT |
| Event registration / prijava_na_dogadjaj | posetilac_id, dogadjaj_id, datum_prijave, status | (posetilac_id, dogadjaj_id) | Visitor, event | PK | All required | PRIJAVLJEN/PRISUSTVOVAO/OTKAZAN | M:N | RESTRICT |

All foreign keys use ON UPDATE RESTRICT. Indexes cover foreign keys and event start times. No direct event–museum FK is added: the relationship is event → exhibition → hall → museum.

## IS-A and minimum cardinalities

JPA uses abstract `Employee`, `@Inheritance(JOINED)`, the `tip_zaposlenog` discriminator, and five concrete subtypes with `@PrimaryKeyJoinColumn`. Deferred PostgreSQL constraint triggers lock the supertype row, count subtype records, and require exactly one matching subtype. Direct SQL therefore cannot commit an employee with zero or multiple subtypes.

Employees are created as concrete subtypes in one transaction. PUT updates the existing subtype; changing subtype implicitly returns 409. Employment type and RBAC role are independent: employment does not automatically grant permissions.

Role assignments use `@EmbeddedId`/`@MapsId`. A deferred trigger enforces at least one role even for direct SQL, and the service rejects removal of the last role. Assignment time is automatic; the assigner is the authenticated administrator, or NULL for registration.

An account may have both visitor and employee profiles through two independent unique FKs. Supervision is an optional self-reference; the service prevents cycles and serializes hierarchy changes. A same-museum supervisor restriction is not added because the specification does not require it.

## Interpretations and limits

- Email, trimmed and lowercased, is the login identifier. No separate username column is introduced. Services and the database enforce case-insensitive uniqueness.
- Descriptive fields and subtype attributes without explicit mandatory requirements remain optional. Names and core operational fields are required at the API boundary.
- Event and registration types/statuses are closed sets. A guide is optional; no additional requirement places every event inside an exhibition admission interval. Start must precede end.
- Registration requires a planned event with remaining capacity. Registered and attended records occupy places. Cancelled records preserve history and may be reactivated; a duplicate active registration returns 409.
- Registration, cancellation, and capacity changes lock the event row. Finished, cancelled, and full events reject new registrations. Capacity cannot be reduced below occupied places. No arbitrary 24-hour deadline is introduced.
- Accounts with dependent profiles/history are not automatically deleted. Administrators can deactivate accounts; physical deletion remains subject to FK and transaction constraints.

## Security and automation

Registration atomically creates a BCrypt account, VISITOR role, and visitor profile. Clients cannot choose their initial role, active state, or password hash. Registration/assignment times and successful last login are recorded automatically.

JWT uses HS256 through Spring Security/Nimbus, an account-ID subject, a 30-minute expiry, and issuer `museum-backend`. The key comes from the process-level `JWT_SECRET` and requires at least 32 UTF-8 bytes. There is no default key. Tests generate independent random keys. Current account activity and roles are loaded for each token, so deactivation and role removal take effect immediately. This design does not include refresh tokens.

Protected operations remain protected in the local profile. Local seed accounts now include an administrator; see the [root guide](../README.md#local-accounts). Earlier subsystem reports describing anonymous catalog writes or manual-only administrator provisioning are historical.

## Column types

All IDs and FKs above are PostgreSQL bigint (Java Long). Only standalone `id` primary keys generate identities; subtype and join tables do not generate separate keys.

| Table | Other columns and SQL types |
|---|---|
| korisnicki_nalog | email text, lozinka_hash text, aktivan boolean, datum_registracije timestamptz, poslednja_prijava timestamptz |
| uloga | naziv varchar(32), opis text |
| nalog_uloga | datum_dodele timestamptz |
| posetilac | ime text, prezime text, telefon text, saglasnost_za_preporuke boolean |
| zaposleni | ime text, prezime text, telefon text, datum_zaposlenja date, tip_zaposlenog varchar(32) |
| kustos | specijalizacija text |
| vodic | primarni_jezik text |
| blagajnik | oznaka_saltera text |
| menadzer | nivo_izvestavanja text |
| administrator | nivo_administracije text |
| interesovanje | naziv text, opis text |
| posetilac_interesovanje | nivo integer |
| dogadjaj | naziv text, tip varchar(32), pocetak timestamptz, kraj timestamptz, kapacitet integer, jezik text, status varchar(32) |
| prijava_na_dogadjaj | datum_prijave timestamptz, status varchar(32) |

FK targets: `nalog_id` and `dodelio_id` → `korisnicki_nalog.id`; `uloga_id` → `uloga.id`; `muzej_id` → `muzej.id`; `nadredjeni_id` → `zaposleni.id`; subtype `zaposleni_id` → `zaposleni.id`; `posetilac_id` → `posetilac.id`; `interesovanje_id` → `interesovanje.id`; `izlozba_id` → `izlozba.id`; `vodic_id` → `vodic.zaposleni_id`; `dogadjaj_id` → `dogadjaj.id`.

CHECK sets: employee type = CURATOR/GUIDE/CASHIER/MANAGER/ADMINISTRATOR; role names also include VISITOR. Event type = TURA/PREDAVANJE/RADIONICA; event status = PLANIRAN/POPUNJEN/ZAVRSEN/OTKAZAN; registration status = PRIJAVLJEN/PRISUSTVOVAO/OTKAZAN. Capacity >= 0, end > start, and supervisor ID differs from employee ID. A regular expression restricts password hashes to BCrypt format. Interest level has no arbitrary range.

## REST routes and RBAC

CRUD denotes collection GET/POST and item GET/PUT/DELETE. POST returns 201 with Location, GET/PUT 200, and DELETE 204. Invalid requests return 400; missing records 404; conflicts 409; missing/invalid authentication 401; insufficient permissions 403. Central exception handlers process validation, integrity, authentication, and authorization. REST responses do not serialize JPA entities.

| Route | Methods | Access |
|---|---|---|
| /api/health | GET | Public |
| /api/auth/register | POST | Public; email, password, firstName, lastName, optional phone, recommendationsConsent |
| /api/auth/login | POST | Public; email and password |
| /api/accounts/me | GET | Active authenticated account |
| /api/admin/accounts | CRUD | ADMINISTRATOR; POST creates an account and visitor profile |
| /api/admin/accounts/{id}/roles/{roleId} | POST, DELETE | ADMINISTRATOR |
| /api/admin/roles | CRUD | ADMINISTRATOR |
| /api/employees | CRUD | ADMINISTRATOR |
| /api/visitors | CRUD | ADMINISTRATOR |
| /api/visitors/me | GET, PUT | VISITOR, own profile |
| /api/visitors/me/interests | GET | VISITOR, own interests |
| /api/visitors/me/interests/{id} | PUT, DELETE | VISITOR, own interest selection |
| /api/visitors/me/registrations | GET | VISITOR, own registrations |
| /api/interests | CRUD | Public GET; CURATOR/ADMINISTRATOR writes |
| /api/events | CRUD | Public GET; GUIDE writes |
| /api/events/{id}/registrations | POST | VISITOR, self-registration |
| /api/events/{id}/registrations | GET | GUIDE, registration list |
| /api/events/{id}/registrations/me | DELETE | VISITOR, own cancellation |
| /api/events/{id}/registrations/{visitorId} | PUT | GUIDE, registration status updates |

Role permissions are explicit: an administrator is not automatically a guide and needs GUIDE for event management. CASHIER sales and MANAGER analytics are implemented in their respective modules, not granted by employee subtype.

V2 initializes six roles. New role requests accept only supported names; duplicates return 409. Assigned roles cannot be deleted due to FKs, and PUT does not rename existing roles.

Registration, cancellation, attendance updates, and capacity changes recalculate PLANIRAN/POPUNJEN while preserving cancelled/finished states. A deferred trigger checks capacity, and services lock the event before changes. Later journey jobs maintain time-dependent statuses and refresh the graph.

The approved login interpretation uses email; separate `username`, `korisnicko_ime`, and `login_name` fields are not part of the schema or DTO contract.
