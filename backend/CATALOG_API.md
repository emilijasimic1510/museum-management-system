# Exhibit catalog API

The API uses the existing entities and Flyway V1–V3 schema. Responses are DTOs. The same authorization rules apply with and without the `local` profile.

## Public routes

These routes are available without login and to visitors:

| Method | Path | Response |
|---|---|---|
| GET | `/api/exhibits` | Exhibits with authors |
| GET | `/api/exhibits/{id}` | Exhibit with authors |
| GET | `/api/authors` | Authors |
| GET | `/api/authors/{id}` | Author |
| GET | `/api/exhibits/{id}/authorships` | Authors and contribution types |
| GET | `/api/exhibits/{id}/participations` | Participations in publicly visible exhibitions |
| GET | `/api/exhibitions/{id}/participations` | Participations in a publicly visible exhibition |

Public responses exclude inventory numbers, internal authorship notes, participation IDs, QR tokens, QR paths, digital content paths, and internal placement details. Exhibit, author, and exhibition IDs remain public navigation identifiers.

An exhibition with `onlineVisible=false` returns 404 through the public participation view. Exhibits and authors have no separate visibility flag in the existing model.

Illustrative `GET /api/exhibits/12` response:

```json
{
  "id": 12,
  "name": "Ceramic vessel",
  "creationPeriod": "19th century",
  "type": "Vessel",
  "material": "Ceramic",
  "origin": "Serbia",
  "description": "A vessel used for storing household provisions.",
  "authors": [{"id": 7, "name": "Ana Anic", "contributionType": "Author"}]
}
```

Illustrative `GET /api/exhibitions/3/participations` response:

```json
[
  {
    "exhibition": {"id": 3, "name": "Everyday Life"},
    "exhibit": {"id": 12, "name": "Ceramic vessel"},
    "contextDescription": "The vessel illustrates food storage in everyday domestic life."
  }
]
```

Illustrative `GET /api/authors/7` response:

```json
{"id":7,"firstName":"Ana","lastName":"Anic","birthYear":1850,"deathYear":1920,"biography":"Biographical information about the author."}
```

## Management: CURATOR or ADMINISTRATOR

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/exhibits` | Create an exhibit |
| PUT, DELETE | `/api/exhibits/{id}` | Update or delete |
| GET | `/api/exhibits/{id}/management` | Exhibit DTO and inventory number |
| POST | `/api/authors` | Create an author |
| PUT, DELETE | `/api/authors/{id}` | Update or delete |
| GET | `/api/exhibit-participations?exhibitionId={id}` | Exhibition participations with QR data, including hidden exhibitions |
| GET | `/api/exhibit-participations/{id}` | Manage a participation |
| POST | `/api/exhibit-participations` | Create a participation |
| PUT, DELETE | `/api/exhibit-participations/{id}` | Update or delete |
| GET | `/api/exhibit-participations/{id}/digital-content` | Participation content |
| GET | `/api/digital-content/{id}` | Content record |
| POST | `/api/digital-content` | Create a content reference |
| PUT, DELETE | `/api/digital-content/{id}` | Update or delete a reference |
| GET | `/api/exhibits/{id}/authorships/management` | Authorships including internal notes |
| PUT | `/api/exhibits/{id}/authorships/{authorId}` | Create or update an authorship |
| DELETE | `/api/exhibits/{id}/authorships/{authorId}` | Delete the relationship only |

POST returns 201 with Location; PUT returns 200; DELETE returns 204. Exhibit management returns an `exhibit` public DTO together with `inventoryNumber`. Exhibit creation and update require `name` and `inventoryNumber`. An author requires `firstName`; the death year cannot precede the birth year.

Example participation request:

```json
{
  "exhibitionId": 3,
  "exhibitId": 12,
  "installedAt": "2030-01-01T00:00:00Z",
  "removedAt": "2030-02-01T00:00:00Z",
  "position": "A1",
  "tourOrder": 1,
  "contextDescription": "The exhibit's role in this exhibition."
}
```

The backend generates a unique UUID QR token on creation and preserves it on update. Clients cannot assign or overwrite `qrToken` or `qrPath` through the input DTO. QR data is available only in authorized responses. Do not log requests or responses containing QR data. DTO string representations redact references and QR values; Hibernate SQL exception logging is disabled to prevent constraint details from exposing tokens. The API returns a generic 409 conflict.

An existing participation cannot be moved to another exhibition or exhibit; create another participation for a different association. Digital content cannot be moved between participations. These restrictions preserve identities referenced by scans and routes. Deletion does not cascade: dependent records cause 409 through RESTRICT constraints.

Authorship retains its composite key `(authorId, exhibitId)`. The PUT body contains `contributionType` and `note`; deleting the relationship does not delete either parent.

## Digital content

Example reference request and response:

```json
{"participationId":21,"type":"AUDIO","path":"media/audio.mp3","language":"sr","version":1,"automaticallyGenerated":false}
```

```json
{"id":8,"participationId":21,"type":"AUDIO","path":"media/audio.mp3","language":"sr","version":1,"automaticallyGenerated":false,"createdAt":"2030-01-01T10:00:00Z"}
```

Supported API enum values are `SLIKA` (image), `TEKST` (text), and `AUDIO`. Versions must be positive. A reference may be relative or use HTTP(S). File URLs, JavaScript URLs, embedded URL credentials, protocol-relative URLs, and parent-directory segments are rejected.

The generic reference endpoint does not prove that a remote resource exists. Dedicated text, image, and audio operations support stored content and TTS; see the [curator guide](../frontend/CURATOR.md). Saving text and generating audio are separate actions.

Content management reads require CURATOR or ADMINISTRATOR. Visitors use `/api/visits/{id}/content/{participationId}`, which checks ownership and an active visit. There is no public bypass.

## Existing catalog and errors

Museum, hall, exhibition, and admission-time browsing is public. Hidden exhibitions and their admission times are not exposed to anonymous users or visitors; authorized catalog staff can manage them. Museum and hall writes require ADMINISTRATOR. Exhibition and admission-time writes require CURATOR or ADMINISTRATOR.

- 400: invalid DTO, ID, enum, interval, or reference.
- 401: missing or invalid authentication on a protected route.
- 403: insufficient role or ownership.
- 404: missing resource, or a hidden exhibition in a public view.
- 409: duplicate, dependent records, or an attempt to change participation/content identity.

## Verification and implementation references

With Docker Desktop, Java 21, and a process-level test `JWT_SECRET` configured, run `mvnw.cmd test` from `backend`. Testcontainers creates isolated PostgreSQL data; the development database is not reset. V1–V3 initialize the test schema and Hibernate validates the mappings. Catalog authorization tests cover local and non-local security using signed JWTs and database roles.

Key implementation files under `backend/src` include `ExhibitCatalogController.java`, `ExhibitDtos.java`, `ExhibitCatalogService.java`, `AuthorshipRepository.java`, `ExhibitParticipationRepository.java`, and `DigitalContentRepository.java`. `ExhibitionService.java`, `TermService.java`, and `ApiSecurity.java` enforce visibility and access. `OpenApiConfiguration.java` documents authorization.

Relevant tests include `ExhibitCatalogIntegrationTests`, `LocalExhibitCatalogIntegrationTests`, `CatalogIntegrationTests`, and `OpenApiIntegrationTests`, with shared authentication support in `CatalogAuthTestBase`.
