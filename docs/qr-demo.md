# QR skeniranje postavke eksponata

QR pripada učešću eksponata na izložbi. Javni katalog ne izlaže token.
POST /api/visits/{id}/scans prima { "qrToken": "...", "durationSeconds": 30 }.
Proverava VISITOR ulogu, vlasništvo, aktivnu posetu i dostupnost postavke u istoj izložbi.
Odgovor sadrži participation (naziv, kontekst, digitalni sadržaji) i next.
Ponovljeno skeniranje beleži novu aktivnost; već posećena postavka ne vraća se u next.
POST /api/visits/{id}/routes sa criterion KOMBINACIJA generiše rutu svih preostalih dostupnih postavki.

## Lokalna demonstracija

1. Restartovati backend sa profilom local i pokrenuti frontend pomoću npm start.
2. Posetilac kupuje kartu za trenutno aktivni termin izložbe „Tragovi svetlosti“ ili „Veče skulpture“.
3. Blagajnik validira kartu. Posetilac otvara aktivnu posetu.
4. Na poseti proširiti „Demo skeniranje · samo lokalno“ i izabrati eksponat.
5. Prikazuju se kontekst i sledeći eksponat; kliknuti „Generiši rutu“.
6. Za QR slike otvoriti /demo-qr kao demo.staff@museum.local ili kustos.
   Tabla prikazuje po tri stvarne demo postavke iz baze, označene nazivom i ID-jem izložbe.
7. Kamera telefona može otvoriti /scan?token=...; prijaviti se kao posetilac,
   izabrati aktivnu posetu i potvrditi skeniranje u formi posete.

Frontend koristi kameru telefona kroz otvaranje QR URL-a, bez ugrađenog video skenera.
Telefon mora imati pristup adresi aplikacije; localhost na telefonu nije računar.
Za test samo na računaru koristite demo dugmad ili kopirajte URL/token.
Tokeni DEMO-QR-001/002/003 pripadaju „Tragovima svetlosti“.
Tokeni DEMO-SCULPTURE-QR-001/002/003 pripadaju „Večeri skulpture“.
Izložba „Grad kroz objektiv“ ima aktivan demo termin (60 mesta), cenu ODRASLI 7.00
i STUDENT 4.00, i sledeće postavke:

| Eksponat | QR token |
| --- | --- |
| Jutro na trgu | DEMO-CITY-QR-001 |
| Odraz na Dunavu | DEMO-CITY-QR-002 |
| Noćna svetla | DEMO-CITY-QR-003 |

Restart backenda sa profilom local izvršava dopunjeni seed. Novi aktivan termin
počinje pet minuta pre izvršavanja i završava se dva sata nakon izvršavanja;
postojeći otvoreni aktivan termin se ponovo koristi bez pomeranja kupljenih karata.
QR ulaznice se generiše prilikom kupovine i kopira iz „Moje ulaznice“ na blagajnu.
Tek nakon validacije ulaznice koristiti gornje tokene na aktivnoj poseti.
Demo izbor na poseti filtrira se prema izložbi na backendu, uz dodatnu proveru
exhibitionId naspram karte u frontendu. QR druge izložbe i dalje se odbija.
Digitalni sadržaji dolaze kao putanje iz baze; dostupnost datoteka zavisi od servera sadržaja.

Demo kontroler postoji samo u local profilu (ne uz prod/production).
Produkcioni frontend ne prikazuje demo opcije i guard odbija /demo-qr.
GET /api/local-demo/board zahteva CASHIER, CURATOR, GUIDE ili ADMINISTRATOR.
GET /api/local-demo/visits/{id} zahteva vlasnika aktivne posete i vraća samo
demo postavke iste izložbe. Ove rute nisu javni katalog.
