# Curator workspace

The curator workspace uses the existing entities and Flyway V1–V3 schema. The source EER models are unchanged.

## Navigation and API

All `/curator` routes use `curatorGuard` and child-route protection. Session restoration checks `/api/accounts/me`. Unauthenticated users are redirected to login; disallowed roles reach `/forbidden`. The backend independently returns 401 or 403 for unauthorized API requests.

| Route | Function and API |
|---|---|
| `/curator` | Exhibit, author, participation, and active participation counts |
| `/curator/exhibits` | Search/list; confirmed DELETE `/api/exhibits/{id}` |
| `/curator/exhibits/new` | POST `/api/exhibits` |
| `/curator/exhibits/:id/edit` | Management GET and PUT; authorship management |
| `/curator/authors` | Author list and confirmed deletion |
| `/curator/authors/new` | POST `/api/authors` |
| `/curator/authors/:id/edit` | GET and PUT `/api/authors/{id}` |
| `/curator/participations` | Participation list with exhibit/exhibition names; confirmed deletion |
| `/curator/participations/new` | Associate an existing exhibit and exhibition |
| `/curator/participations/:id/edit` | Edit placement/context; display QR image and copy token |
| `/curator/digital-content` | Content by participation; confirmed deletion |
| `/curator/digital-content/new` | Select participation and create content |
| `/curator/digital-content/:id/edit` | Read/update content; participation identity is fixed |
| `/curator/profile` | Own account from `/api/accounts/me` |

The workspace also provides exhibition/admission-time, pricing, and QR board operations. Visitor and cashier workflows retain their own navigation.

### Authorship

| Endpoint | Behavior |
|---|---|
| GET `/api/exhibits/{id}/authorships/management` | Return author/exhibit IDs, contribution type, and internal note |
| PUT `/api/exhibits/{id}/authorships/{authorId}` | Idempotently create/update the relationship |
| DELETE `/api/exhibits/{id}/authorships/{authorId}` | Delete only the relationship; return 404 if absent |

These operations require CURATOR or ADMINISTRATOR. Authorship uses the existing composite key; no artificial ID is introduced. The participation list accepts an optional `exhibitionId`; omitting it returns all authorized management records.

## Permissions

| Role | Public catalog | Management API | Curator frontend |
|---|---|---|---|
| Anonymous | GET allowed | 401 | Login |
| VISITOR | GET allowed | 403 | Forbidden |
| CASHIER | GET allowed | 403 | Forbidden; cashier workspace available |
| GUIDE | GET allowed | 403 | Forbidden |
| CURATOR | GET allowed | CRUD allowed | Allowed |
| ADMINISTRATOR | GET allowed | Allowed by catalog rules | Allowed |

Public DTOs exclude QR tokens/paths, inventory numbers, and internal authorship notes. Visitors access contextual content through an active visit, without management controls. The local profile does not bypass these restrictions.

## QR and digital content

The frontend does not submit `qrToken` or `qrPath`. The backend creates a unique UUID token and preserves it during edits. Extra JSON fields cannot overwrite the record ID, QR identity, or content creation time. Existing QR paths are preserved. The `qrcode` library renders QR images locally without an external image service.

A participation's exhibition and exhibit are immutable. Content cannot move to another participation. Dependent records may prevent deletion with 409; historical data is not cascade-deleted.

Content records store a reference, language, positive integer version, and automatic-generation flag. The enum values are `TEKST`, `SLIKA`, and `AUDIO`. The current application supports stored text, images, and generated audio in addition to generic references. Saving text does not automatically generate audio; use the explicit generation action with a configured TTS provider. A saved remote reference alone is not evidence that media playback works.

For phone scanning, generate the QR board from the same HTTPS origin used on the phone and select the exhibition matching the active visit. Regenerate it after a temporary tunnel hostname changes. Follow the complete [QR testing procedure](../README.md#testing-qr-scanning-with-a-phone).

## Local account and manual checks

The local curator account is `nikola.markovic@museum.local`. Its password is defined in `DemoDataSeeder.PASSWORD`; the database stores a BCrypt hash. Local seed credentials must not be used for public deployment.

1. Start the application using the root guide, sign in as the curator, and refresh the page to verify session restoration.
2. Create a disposable author and exhibit with a unique inventory number. Edit their descriptive fields.
3. Add, update, and remove their authorship. Confirm that both parent records remain. Restore the relationship and verify that deleting a referenced author produces a conflict.
4. Create an exhibition participation, set its placement, tour order, and context. Check the generated QR image; edit the context and confirm the token remains stable.
5. Add digital content for that participation. Save text, upload an appropriate image, and explicitly generate audio. Test actual delivery and playback using a visitor's active visit.
6. Check that content cannot be reassigned and that deleting a participation with dependencies produces a clear conflict. Clean up only disposable records in dependency order. Do not delete seed records or visit history.
7. Log out and verify the login redirect. Check rejection with VISITOR, CASHIER, and GUIDE accounts and permitted catalog access with ADMINISTRATOR.
8. Inspect public responses for absence of internal notes and QR fields. Exercise ticket purchase, cashier validation, scanning, and recommendations separately.
9. Check keyboard focus, long names/references, and layouts at phone, tablet, and desktop widths, including 320, 375, 390, 430, and 768 pixels.

## Historical verification

The 2026-09-23 module review recorded 104 passing backend tests and 45 passing frontend tests in four files, together with successful TypeScript, production build, and whitespace checks. These are historical results, not a claim that this documentation edit reran the suite. The build reported the existing `qrcode` CommonJS warning.

The review used an additional local backend on 8082 and frontend on 4201 without stopping existing 8081/4200 processes. Browser checks covered login, session refresh, navigation, forms, authorships, profile, content references, QR display, logout, and visitor/cashier route denial. Responsive measurements found no horizontal overflow at the recorded widths. GUIDE and ADMINISTRATOR permissions were covered by automated tests.

Some writes to existing development records were blocked by the execution environment's approval review; they were not retried. CRUD behavior was covered in isolated integration and frontend HTTP tests. The initial reference-only content limitation was superseded by the current media features.

Lists target the current catalog size and do not provide server pagination. Author relationships are managed from the exhibit editor rather than loading every exhibit for an author. Neo4j refresh uses the shared projection mechanism. See the [integrated submission audit](../docs/submission-readiness.md) for later recorded results.
