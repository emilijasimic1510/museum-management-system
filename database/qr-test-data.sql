DO $$
DECLARE e1 bigint; e2 bigint; t1 bigint; t2 bigint; x1 bigint; x2 bigint; x3 bigint;
BEGIN
INSERT INTO izlozba(sala_id,naziv,opis,datum_pocetka,datum_zavrsetka,status,online_vidljiva)
VALUES (2,'QR test: Tragovi svetlosti','Izložba za testiranje QR skeniranja i poseta.',now()-interval '1 hour',now()+interval '30 days','AKTIVNA',true) RETURNING id INTO e1;
INSERT INTO termin_izlozbe(izlozba_id,pocetak,kraj,kapacitet,status)
VALUES (e1,now()+interval '2 hours',now()+interval '4 hours',20,'OTVOREN') RETURNING id INTO t1;
INSERT INTO cenovnik_izlozbe VALUES (e1,now(),'REDOVNA',null,12.50,'QR test');
INSERT INTO eksponat(naziv,godina_nastanka,tip,materijal,poreklo,opis,inventarski_broj)
VALUES ('Svetlosni prag','2026','INSTALACIJA','Metal','Srbija','Eksponat za QR test 1','QR-TEST-001') RETURNING id INTO x1;
INSERT INTO eksponat(naziv,godina_nastanka,tip,materijal,poreklo,opis,inventarski_broj)
VALUES ('Plava putanja','2026','SLIKA','Ulje','Srbija','Eksponat za QR test 2','QR-TEST-002') RETURNING id INTO x2;
INSERT INTO ucesce_eksponata_na_izlozbi(izlozba_id,eksponat_id,datum_postavljanja,redosled_obilaska,kontekstualni_opis,qr_token,qr_putanja)
VALUES (e1,x1,now(),1,'Skeniraj ovaj QR za prvi eksponat','QR-DEMO-TRAGOVI-001','qr/QR-DEMO-TRAGOVI-001.png'),
       (e1,x2,now(),2,'Skeniraj ovaj QR za drugi eksponat','QR-DEMO-TRAGOVI-002','qr/QR-DEMO-TRAGOVI-002.png');
INSERT INTO izlozba(sala_id,naziv,opis,datum_pocetka,datum_zavrsetka,status,online_vidljiva)
VALUES (5,'QR test: Forma i prostor','Druga izložba sa posebnim QR kodovima.',now()+interval '1 day',now()+interval '45 days','PLANIRANA',true) RETURNING id INTO e2;
INSERT INTO termin_izlozbe(izlozba_id,pocetak,kraj,kapacitet,status)
VALUES (e2,now()+interval '1 day',now()+interval '1 day 2 hours',10,'OTVOREN') RETURNING id INTO t2;
INSERT INTO cenovnik_izlozbe VALUES (e2,now(),'REDOVNA',null,10.00,'QR test');
INSERT INTO eksponat(naziv,godina_nastanka,tip,materijal,poreklo,opis,inventarski_broj)
VALUES ('Geometrijski sto','2025','SKULPTURA','Drvo','Srbija','Eksponat za QR test 3','QR-TEST-003') RETURNING id INTO x3;
INSERT INTO ucesce_eksponata_na_izlozbi(izlozba_id,eksponat_id,datum_postavljanja,redosled_obilaska,kontekstualni_opis,qr_token,qr_putanja)
VALUES (e2,x3,now(),1,'Skeniraj ovaj QR za treći eksponat','QR-DEMO-FORMA-001','qr/QR-DEMO-FORMA-001.png');
RAISE NOTICE 'created exhibitions %, %; terms %, %',e1,e2,t1,t2;
END $$;

SELECT i.id AS exhibition_id,i.naziv,t.id AS term_id,t.pocetak,t.kraj,u.id_ucesca,u.qr_token
FROM izlozba i JOIN termin_izlozbe t ON t.izlozba_id=i.id
JOIN ucesce_eksponata_na_izlozbi u ON u.izlozba_id=i.id
WHERE i.naziv LIKE 'QR test:%' ORDER BY i.id,u.id_ucesca;
