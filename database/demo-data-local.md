# Demo podaci za lokalni profil

`DemoDataSeeder` ubacuje podatke samo kada je aktivan Spring profil `local` ili
`development`. Ne menja V1, V2 ili V3 migraciju, ne radi u `test`, staging ili
production profilu i koristi BCrypt za obe demo lozinke. Ponovno pokretanje je
bezbedno: prirodni ključevi demo zapisa i `ON CONFLICT DO NOTHING` sprečavaju
duplikate.

## Demo nalozi

Oba naloga koriste lozinku `MuseumDemo!2026` (u bazi je samo BCrypt hash):

| Email | Uloge | Svrha |
| --- | --- | --- |
| `demo.visitor@museum.local` | `VISITOR` | prijava, kupovina, posete i preporuke |
| `demo.staff@museum.local` | `CASHIER`, `GUIDE` | validacija ulaznice i događaji |

Email je identifikator za prijavu. Podaci koriste lokalne, lažne email adrese i
telefone.

## Stabilni demo identifikatori

Seed traži zapise po imenima i inventarskim brojevima, pa su identiteti stabilni
nakon prvog pokretanja. Posle prvog pokretanja ID-jeve proverite ovim komandama:

```powershell
docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "select id,naziv from muzej where naziv=\u0027Demo Museum Novi Sad\u0027; select id,naziv from izlozba where naziv=\u0027Tragovi svetlosti\u0027; select id,izlozba_id,pocetak from termin_izlozbe where izlozba_id=(select id from izlozba where naziv=\u0027Tragovi svetlosti\u0027); select izlozba_id,datum_vazenja_od,kategorija,cena from cenovnik_izlozbe where izlozba_id=(select id from izlozba where naziv=\u0027Tragovi svetlosti\u0027); select id,naziv,inventarski_broj from eksponat where inventarski_broj like \u0027DEMO-EX-%\u0027 order by id; select id_ucesca,qr_token from ucesce_eksponata_na_izlozbi where qr_token like \u0027DEMO-QR-%\u0027 order by id_ucesca;"' 
```

Očekivani poslovni ključevi su muzej `Demo Museum Novi Sad`, izložba `Tragovi
svetlosti`, kategorije `ODRASLI` i `STUDENT`, inventarski brojevi
`DEMO-EX-001` do `DEMO-EX-003` i QR tokeni `DEMO-QR-001` do `DEMO-QR-003`.

## Aktiviranje

```powershell
docker compose up -d
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
cd backend
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
```

Seed se izvršava pri startu aplikacije, posle Flyway migracija i Hibernate
validacije. Za ponovno pokretanje samo zaustavite i ponovo pokrenite backend.

## Redosled demonstracije

1. Otvorite `http://127.0.0.1:8081/swagger-ui/index.html` i proverite muzeje,
   izložbe, termine i `/api/prices?exhibitionId=...`.
2. Pozovite `POST /api/auth/login` sa demo posetiocem i sačuvajte `accessToken`.
3. Sa tokenom pozovite `POST /api/tickets` sa `termId`, kategorijom `ODRASLI`,
   `paymentMethod=KARTICA` i `demoDecline=false`.
4. Demo osobljem se prijavite i pozovite `POST /api/cashier/validate` sa
   dobijenim `qrToken` ulaznice i istim `termId`; to kreira aktivnu posetu.
5. Posetiocem proverite `GET /api/visits/me`, zatim skenirajte
   `POST /api/visits/{id}/scans` sa `DEMO-QR-001`.
6. Pozovite `POST /api/visits/{id}/routes` sa kriterijumom `KOMBINACIJA`, pa
   `GET /api/visits/{id}/recommendations` i `GET /api/recommendations/exhibitions`.
7. Na kraju proverite `GET /api/visits/{id}/scans`, rutu i sadržaj eksponata.

Kupovina i validacija koriste stvarne backend transakcije, kapacitet, payment
status, QR i PostgreSQL ograničenja; demo payment adapter ne naplaćuje stvarni
novac.
