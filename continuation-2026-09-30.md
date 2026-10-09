# Implementation review — 2026-09-30

This is an archived development-stage report. Its module gaps, account inventory, and test counts describe 2026-09-30, not the current application. Follow the [root README](README.md) and [later submission audit](docs/submission-readiness.md) for maintained instructions and the latest recorded integrated review.

## Initial state and scope

The review covered Git status/diffs/history, controllers, services, DTOs, security, Angular routes/guards/navigation/forms, tests, seed data, and existing reports. No AGENTS.md was present.

The recorded HEAD was `a380b73 Add responsive public digital catalog`, following catalog API, scanning, local seed/frontend, journey/Swagger, and subsystem 2 commits. The initial working tree contained 16 modified tracked files and 17 untracked files. The catalog was implemented, Curator CRUD was present in local changes, and most Guide features existed. Remaining Guide issues included attendee names and stale form data after route-ID changes.

Existing changes were preserved. The review did not alter EER diagrams, the specification, Flyway V1–V3, the schema, or entities. It did not reset/check out files, delete development data, commit, or push.

## Module status at that date

“Complete” referred to the reviewed functional scope, not production readiness.

| Module | Recorded state on 2026-09-30 |
|---|---|
| Visitor | Profile/interest API, tickets, visits, ratings, scans, events, own registrations, ownership checks, and related UI flows implemented |
| Cashier | Sales/validation API implemented; validation UI present, sales form still missing |
| Public catalog | Exhibit/author/participation browsing, filters, responsive states, and redacted public DTOs implemented |
| Curator | CRUD, authorships, participation, stable server QR identity, reference content, dashboard/profile implemented |
| Guide/events | CRUD, capacity locking, registrations, attendee names, profile, visitor event actions, and responsive UI implemented |
| Manager | Employee subtype existed; analytics API/UI still missing |
| Administrator | Account/role/employee and catalog API permissions existed; dedicated administration frontend still missing |
| Neo4j | Projection, scores, recommendations, routes, and fallback implemented without exporting personal data |

At that stage, local catalog counts were 9 exhibits, 2 authors, and 3 exhibitions. Later work expanded content and implemented Manager analytics, Administrator UI, Cashier sales UI, and stored media/TTS. Historical gaps above must not be read as current limitations.

## Changes completed in this review

- Corrected Windows Maven Wrapper handling of a null/empty local cache Target without changing the Maven distribution version.
- Added protected `ManagedRegistrationResponse` data containing visitor first/last names, without exposing email, phone, or password hash. EntityGraph loaded the visitor with registrations.
- Displayed attendee name, ID, and status in the Guide workspace and retained names while updates reloaded.
- Added `RecordRouteReuseStrategy` for marked detail/editor routes when record IDs change, preventing stale forms or confirmations from targeting the previous record.
- Added regression tests for route changes during requests, editing a second event/author, attendee names, and backend response privacy.
- Prevented invalid Guide edit IDs from becoming event-creation actions.
- Updated account documentation and then-obsolete README status.

## Routes and permissions reviewed

| Frontend | API/function |
|---|---|
| /exhibits, /exhibits/:id | Public exhibits and exhibition participations |
| /authors, /authors/:id | Public author list/details |
| /curator, /curator/profile | Catalog counts and own account |
| /curator/exhibits, new/edit routes | Exhibit CRUD, management DTO, authorships |
| /curator/authors, new/edit routes | Author CRUD |
| /curator/participations, new/edit routes | Participation CRUD |
| /curator/digital-content, new/edit routes | Content references by participation |
| /guide, /guide/profile | Events and /api/guides/me |
| /guide/events, new/edit routes | Event CRUD |
| /guide/registrations, /guide/events/:id/registrations | Protected registration lists and status changes |
| /events, /events/:id | Public events and own visitor registration/cancellation |
| /cashier | Ticket lookup and validation |
| /tickets, /visits, /visits/:id, /scan, /recommendations, /profile | Visitor journey APIs |

| Role | Public catalog/events | Curator API/UI | Guide API/UI | Visitor registration |
|---|---|---|---|---|
| Anonymous | Allowed | API 401; UI login | API 401; UI login | 401 |
| VISITOR | Allowed | 403 | 403 | Own records |
| CASHIER | Allowed | 403 | 403 unless also GUIDE | 403 |
| CURATOR | Allowed | Allowed | 403 | 403 |
| GUIDE | Allowed | 403 | Allowed | 403 |
| ADMINISTRATOR | Allowed | Allowed | 403 unless also GUIDE | 403 |
| MANAGER | Allowed | 403 | 403 | 403 |

The backend checked every assigned role. The historical combined CASHIER/GUIDE account therefore had both API permissions, while frontend navigation prioritized the cashier workspace. All guides could manage events under the reviewed policy; no additional guide ownership/museum rule was introduced.

Capacity used pessimistic event locking, registration composite keys, and database triggers. Duplicate active registrations returned 409. Cancelled registrations could reactivate only while the event was open with free places. Registration closed at event start. Visitors could not cancel recorded attendance. Events with registration history could not be deleted; cancellation preserved history. UI error handling translated 400/401/403/404/409 without rendering raw server output.

## Historical accounts

The earlier local seed used `demo.visitor@museum.local`, `demo.staff@museum.local`, `demo.curator@museum.local`, and `demo.guide@museum.local`. These addresses are historical identifiers, not current login instructions. They were later replaced by the realistic accounts listed in the [root account table](README.md#local-accounts). The local password is maintained in `DemoDataSeeder.PASSWORD` and is not reproduced here.

The isolated seed tests covered all four earlier accounts. A read-only development query confirmed visitor, staff, and curator records; the guide account was to be created on the next local startup. Existing passwords were not reset.

## Recorded verification

The final `mvnw.cmd clean test` passed **110/110 tests**, with 0 failures, errors, or skipped tests, in 7 minutes 4 seconds, finishing at 13:25 CEST on 2026-09-30. It followed an earlier full pass to include final backend changes.

| Backend suite | Tests | Result |
|---|---:|---|
| CatalogIntegration / CatalogService | 10 + 10 | Passed |
| ExhibitCatalog / LocalExhibitCatalog | 16 + 16 | Passed |
| HealthController | 2 | Passed |
| GraphProjection | 3 | Passed |
| JourneyIntegration / LocalDemoJourney | 16 + 4 | Passed |
| MuseumBackendApplication / OpenApi | 1 + 2 | Passed |
| PeopleIntegration / PeopleService | 23 + 7 | Passed |

PostgreSQL 17.9 Testcontainers applied unchanged V1–V3 and passed Hibernate validation. GraphProjection used a real Neo4j 5.26 container. Seed tests checked idempotency, roles, and subtypes.

Frontend `npm test` passed 68 tests in five files. TypeScript noEmit and the Angular production build passed, with the existing `qrcode` CommonJS warning. An initial restricted build could not access parent directories; the approved execution succeeded.

Ignored local logs were `backend-final-verification.log`, `frontend-tests.log`, `frontend-build.log`, and `schema-validation.log`. The random process JWT secret was not committed or published.

A temporary backend on 127.0.0.1:8082 validated the existing development schema with Flyway execution and scheduled jobs disabled, no local/development profile, and no seed execution. Health returned UP. Only that temporary Java process was stopped. SQL confirmed successful V1/V2/V3 history. An initial connection attempt preceded database readiness; retry after readiness succeeded.

The whitespace check passed. Git ignored .env, target, node_modules, and logs. A limited pattern check found no typical private keys/API tokens in tracked files; this was not an exhaustive secret audit. Migration/EER/specification diffs were empty.

No new visual browser review of Guide pages was performed during this continuation. Existing responsive CSS and previous Curator checks did not establish visual verification of the new Guide screens.

## Manual verification sequence

Use current accounts and startup instructions from the root README when repeating these checks.

1. Start the dependencies, backend, and frontend with a configured JWT secret. Preserve existing data and applied migrations.
2. Check anonymous exhibit/author browsing, filters, empty results, missing IDs, and absence of internal QR fields in public DTOs.
3. As curator, create disposable author/exhibit records, manage authorship, create a participation, and confirm stable QR identity after editing context.
4. Add content and check conflicts when deleting dependent participation. Remove only disposable records in dependency order.
5. As guide, create a future event with capacity 1, select the guide, edit its name, and check the list/dashboard and session refresh.
6. As visitor, register and confirm the place is occupied. Duplicate registration or a second visitor's attempt must fail; concurrency is also covered by integration tests.
7. Cancel, register again, and mark attendance as guide. Confirm that attended registrations cannot be cancelled by the visitor and closed events reject new registrations.
8. Check restricted deletion of an event with history, deletion of a disposable event without history, invalid dates, and rejection of reducing capacity below occupied places.
9. Navigate between different event/author edit URLs and confirm the correct record before saving, including browser back/forward and refresh.
10. Check Guide route denial for other roles, anonymous login redirects, API 401/403 responses, and registration-list privacy.
11. Exercise purchase, cashier validation, active visit, matching QR scan, content, recommendations, and routes.
12. Inspect Guide and visitor pages at phone/tablet/desktop widths, keyboard focus, long labels, loading/error states, and retry behavior.

## Outstanding work recorded at that date

Manager analytics, dedicated Administrator UI, Cashier sales UI, and stored media were still pending then and were implemented later. A new visual Guide review, production payment/deployment configuration, and large-catalog pagination remained separate concerns. Refer to the later audit rather than treating this list as the current backlog.

## Historical file inventory

The following snapshot records 22 modified tracked files and 20 untracked files at the end of that continuation. Nothing was staged. It is retained as historical context, not a current Git status:

```text
 M README.md
 M backend/mvnw.cmd
 M backend/src/main/java/rs/ac/uns/ftn/museum/catalog/controller/ExhibitCatalogController.java
 M backend/src/main/java/rs/ac/uns/ftn/museum/catalog/dto/ExhibitDtos.java
 M backend/src/main/java/rs/ac/uns/ftn/museum/catalog/service/ExhibitCatalogService.java
 M backend/src/main/java/rs/ac/uns/ftn/museum/config/DemoDataSeeder.java
 M backend/src/main/java/rs/ac/uns/ftn/museum/people/controller/EventController.java
 M backend/src/main/java/rs/ac/uns/ftn/museum/people/repository/EventRegistrationRepository.java
 M backend/src/main/java/rs/ac/uns/ftn/museum/people/repository/GuideRepository.java
 M backend/src/main/java/rs/ac/uns/ftn/museum/people/security/ApiSecurity.java
 M backend/src/main/java/rs/ac/uns/ftn/museum/people/service/EventService.java
 M backend/src/test/java/rs/ac/uns/ftn/museum/catalog/ExhibitCatalogIntegrationTests.java
 M backend/src/test/java/rs/ac/uns/ftn/museum/journey/LocalDemoJourneyTests.java
 M backend/src/test/java/rs/ac/uns/ftn/museum/people/PeopleIntegrationTests.java
 M database/demo-data-local.md
 M frontend/src/app/app.ts
 M frontend/src/app/core/api.ts
 M frontend/src/app/core/auth.ts
 M frontend/src/app/pages/auth-page.ts
 M frontend/src/app/routes.ts
 M frontend/src/main.ts
 M frontend/src/styles.css
?? backend/src/main/java/rs/ac/uns/ftn/museum/people/controller/GuideController.java
?? backend/src/main/java/rs/ac/uns/ftn/museum/people/dto/ManagedRegistrationResponse.java
?? docs/continuation-2026-09-30.md
?? frontend/CURATOR.md
?? frontend/src/app/core/curator.ts
?? frontend/src/app/core/events.ts
?? frontend/src/app/core/route-reuse.ts
?? frontend/src/app/pages/curator-authorships.ts
?? frontend/src/app/pages/curator-content.ts
?? frontend/src/app/pages/curator-editor.ts
?? frontend/src/app/pages/curator-list.ts
?? frontend/src/app/pages/curator-overview.ts
?? frontend/src/app/pages/curator.spec.ts
?? frontend/src/app/pages/event-detail.ts
?? frontend/src/app/pages/events.spec.ts
?? frontend/src/app/pages/events.ts
?? frontend/src/app/pages/guide-event-editor.ts
?? frontend/src/app/pages/guide-overview.ts
?? frontend/src/app/pages/guide-registrations.ts
?? frontend/src/app/shared/participation-qr.ts
```
