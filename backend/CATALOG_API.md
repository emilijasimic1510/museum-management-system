# Katalog eksponata

API koristi postojeće entitete i Flyway V1–V3, bez nove šeme. Odgovori su DTO-i.
Ista pravila autorizacije važe sa `local` profilom i bez njega.

## Javne rute

Bez prijave (i za VISITOR):

| Metod | Putanja | Odgovor |
| --- | --- | --- |
| GET | `/api/exhibits` | Lista eksponata sa autorima |
| GET | `/api/exhibits/{id}` | Eksponat sa autorima |
| GET | `/api/authors` | Lista autora |
| GET | `/api/authors/{id}` | Autor |
| GET | `/api/exhibits/{id}/authorships` | Autori i vrsta doprinosa |
| GET | `/api/exhibits/{id}/participations` | Učešća u javno vidljivim izložbama |
| GET | `/api/exhibitions/{id}/participations` | Učešća na javno vidljivoj izložbi |

Javni odgovor ne sadrži inventarski broj, napomenu autorstva, ID učešća,
QR token, QR putanju, digitalnu putanju niti interne podatke rasporeda.
ID eksponata, autora i izložbe su javni identifikatori za navigaciju.
Izložba sa `onlineVisible=false` daje 404 na javnom pregledu učešća.
Eksponat nema oznaku javne vidljivosti u postojećem modelu, pa su svi eksponati
i autori dostupni u javnom katalogu; nisu dodavani novi atributi.

Primer `GET /api/exhibits/12` (ilustrativni podaci):

```json
{
  "id": 12,
  "name": "Keramička posuda",
  "creationPeriod": "19. vek",
  "type": "Posuda",
  "material": "Keramika",
  "origin": "Srbija",
  "description": "Opis predmeta.",
  "authors": [{"id": 7, "name": "Ana Anić", "contributionType": "Autor"}]
}
```

Primer `GET /api/exhibitions/3/participations`:

```json
[
  {
    "exhibition": {"id": 3, "name": "Svakodnevni život"},
    "exhibit": {"id": 12, "name": "Keramička posuda"},
    "contextDescription": "Uloga posude u svakodnevnom životu."
  }
]
```

Primer `GET /api/authors/7`:

```json
{"id":7,"firstName":"Ana","lastName":"Anić","birthYear":1850,"deathYear":1920,"biography":"Biografija."}
```

## Upravljanje: CURATOR ili ADMINISTRATOR

| Metod | Putanja | Namena |
| --- | --- | --- |
| POST | `/api/exhibits` | Kreiranje eksponata |
| PUT, DELETE | `/api/exhibits/{id}` | Izmena / brisanje |
| GET | `/api/exhibits/{id}/management` | DTO eksponata i inventarski broj |
| POST | `/api/authors` | Kreiranje autora |
| PUT, DELETE | `/api/authors/{id}` | Izmena / brisanje |
| GET | `/api/exhibit-participations?exhibitionId={id}` | Učešća izložbe sa QR podacima, uključujući skrivene izložbe |
| GET | `/api/exhibit-participations/{id}` | Upravljanje konkretnim učešćem |
| POST | `/api/exhibit-participations` | Kreiranje učešća |
| PUT, DELETE | `/api/exhibit-participations/{id}` | Izmena / brisanje |
| GET | `/api/exhibit-participations/{id}/digital-content` | Lista sadržaja učešća |
| GET | `/api/digital-content/{id}` | Sadržaj |
| POST | `/api/digital-content` | Kreiranje reference sadržaja |
| PUT, DELETE | `/api/digital-content/{id}` | Izmena / brisanje reference |

POST vraća 201 i Location; PUT vraća 200; DELETE vraća 204. Upravljanje
eksponatom vraća `{ "exhibit": { ...javni DTO... }, "inventoryNumber": "INV-12" }`.
Za novi ili izmenjeni eksponat `name` i `inventoryNumber` su obavezni.
Autor zahteva `firstName`; godina smrti ne sme biti pre godine rođenja.

Primer zahteva za učešće:

```json
{
  "exhibitionId": 3,
  "exhibitId": 12,
  "installedAt": "2030-01-01T00:00:00Z",
  "removedAt": "2030-02-01T00:00:00Z",
  "position": "A1",
  "tourOrder": 1,
  "contextDescription": "Priča ovog eksponata na izložbi."
}
```

Ako je `qrToken` izostavljen pri kreiranju, servis generiše UUID token;
izostavljanje pri izmeni čuva postojeći token. Kustos/admin može uneti token
i `qrPath`. Token nema razmake, ima najviše 512 znakova i mora biti jedinstven.
QR podaci postoje samo u upravljačkom odgovoru. Ne beležiti zahteve/odgovore
sa ovih ruta u logove. DTO `toString()` rediguje reference i QR podatke;
Hibernate SQL exception logger je isključen jer detalji ograničenja mogu
uključiti token. API i dalje vraća generičku 409 grešku.

Postojećem učešću se ne menjaju eksponat ili izložba: za drugu vezu kreira se
novo učešće. Digitalni sadržaj se ne premešta na drugo učešće. Time se čuva
identitet na koji se oslanjaju skeniranja i rute. Brisanje ne kaskadira:
postojeće RESTRICT veze daju 409 kada postoje zavisni zapisi.
Autorstvo ima složeni ključ `(authorId, exhibitId)` koji ostaje nepromenjen.
Njegove write rute nisu deo ovog koraka; postojeće veze se čitaju kroz DTO.

## Digitalni sadržaj: reference, bez uploada

Primer POST zahteva i odgovora:

```json
{"participationId":21,"type":"AUDIO","path":"media/audio.mp3","language":"sr","version":1,"automaticallyGenerated":false}
```

```json
{"id":8,"participationId":21,"type":"AUDIO","path":"media/audio.mp3","language":"sr","version":1,"automaticallyGenerated":false,"createdAt":"2030-01-01T10:00:00Z"}
```

Dozvoljeni tipovi: `SLIKA`, `TEKST`, `AUDIO`. Verzija je pozitivna.
Putanja je relativna referenca ili HTTP(S) URL; ne prihvataju se `file:`,
`javascript:`, URL sa korisničkim podacima, protokol-relativni URL ili `..` segmenti.
Servis ne preuzima URL, ne proverava da li fajl postoji i ne skladišti fajlove.
`TEKST` označava referencu na tekstualni resurs, ne novi atribut za tekst.

U postojećoj šemi nema oznake da je digitalna putanja bezbedna za javnost.
Zato su novi content read endpointi samo za kustosa/admina. Posetilac koristi
neizmenjenu `/api/visits/{id}/content/{participationId}` rutu sa proverom
vlasništva i aktivne posete. Nema javnog zaobilaženja te provere.

## Postojeći katalog i greške

GET liste i detalja muzeja, hala, izložbi i termina su javni u oba profila.
Skrivene izložbe i njihovi termini se ne prikazuju anonimnim/visitor korisnicima;
kustos/admin ih vidi. Write muzeja/hala ostaje ADMINISTRATOR; write
izložbi/termina CURATOR ili ADMINISTRATOR.

- 400: neispravan DTO, ID, enum, interval ili referenca.
- 401: neprijavljen korisnik na zaštićenoj ruti.
- 403: pogrešna uloga.
- 404: nepostojeći resurs (ili skrivena izložba u javnom pregledu).
- 409: duplikat, postojeće zavisnosti ili promena identiteta učešća/sadržaja.

## Provera

Sa Docker Desktop, JDK 21 i postavljenim testnim `JWT_SECRET` (najmanje 32 bajta),
iz direktorijuma `backend` pokrenuti `mvn test` ili `mvnw.cmd test`.
Testcontainers koristi izolovani PostgreSQL; razvojna baza se ne menja.
Postojeće V1–V3 migracije kreiraju testnu šemu, a Hibernate `ddl-auto=validate`
proverava mapiranja. Isti novi katalog testovi izvršavaju se sa `local` profilom
i bez njega, uz stvarne potpisane JWT-ove i uloge iz testne baze.

## Izmenjeni fajlovi u ovom koraku

Putanje u tabeli su relativne na direktorijum `backend`.

| Fajl | Izmena |
| --- | --- |
| `src/main/java/rs/ac/uns/ftn/museum/catalog/controller/ExhibitCatalogController.java` | Nove javne i upravljačke REST rute |
| `src/main/java/rs/ac/uns/ftn/museum/catalog/dto/ExhibitDtos.java` | Validirani zahtevi i odvojeni javni/upravljački DTO odgovori |
| `src/main/java/rs/ac/uns/ftn/museum/catalog/service/ExhibitCatalogService.java` | CRUD, reference, javne projekcije i očuvanje veza |
| `src/main/java/rs/ac/uns/ftn/museum/catalog/repository/AuthorshipRepository.java` | Upit za autore konkretnog eksponata |
| `src/main/java/rs/ac/uns/ftn/museum/catalog/repository/ExhibitParticipationRepository.java` | Upiti po eksponatu/izložbi i zaključavanje učešća |
| `src/main/java/rs/ac/uns/ftn/museum/catalog/repository/DigitalContentRepository.java` | Uređen sadržaj konkretnog učešća |
| `src/main/java/rs/ac/uns/ftn/museum/catalog/service/ExhibitionService.java` | Javna vidljivost izložbi; osoblje vidi i skrivene |
| `src/main/java/rs/ac/uns/ftn/museum/catalog/service/TermService.java` | Javna vidljivost termina prati vidljivost izložbe |
| `src/main/java/rs/ac/uns/ftn/museum/people/security/ApiSecurity.java` | Javne GET rute i zaštita write/management ruta u oba profila |
| `src/main/java/rs/ac/uns/ftn/museum/config/OpenApiConfiguration.java` | Swagger ne označava write rute kao javne |
| `src/main/resources/application.yml` | Sprečavanje ispisa QR vrednosti kroz Hibernate SQL constraint detalje |
| `src/test/java/rs/ac/uns/ftn/museum/catalog/ExhibitCatalogIntegrationTests.java` | CRUD, autorizacija, redakcija, 400/404/409, skriveni resursi, V1–V3 |
| `src/test/java/rs/ac/uns/ftn/museum/catalog/LocalExhibitCatalogIntegrationTests.java` | Isti ugovor testiran u local profilu |
| `src/test/java/rs/ac/uns/ftn/museum/support/CatalogAuthTestBase.java` | Testni nalozi sa stvarnim JWT-om i baznim ulogama |
| `src/test/java/rs/ac/uns/ftn/museum/catalog/CatalogIntegrationTests.java` | Postojeći write testovi sada koriste administratorski nalog |
| `src/test/java/rs/ac/uns/ftn/museum/OpenApiIntegrationTests.java` | Provera dokumentovane autorizacije |
| `README.md` | Ažurirana pravila pristupa i opis V1–V3 |
| `CATALOG_API.md` | Ugovor API-ja, primeri, ograničenja i spisak izmena |
