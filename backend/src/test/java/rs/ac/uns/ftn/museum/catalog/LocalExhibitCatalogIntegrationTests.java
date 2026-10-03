package rs.ac.uns.ftn.museum.catalog;

import org.springframework.test.context.ActiveProfiles;

/** Run the exact same public/write authorization contract with the local profile enabled. */
@ActiveProfiles(value={"test","local"}, inheritProfiles=false)
class LocalExhibitCatalogIntegrationTests extends ExhibitCatalogIntegrationTests {
    @org.springframework.beans.factory.annotation.Autowired rs.ac.uns.ftn.museum.config.DemoDataSeeder seed;

    @org.junit.jupiter.api.Test void installationSeedUpgradesOriginalTextPreservesEditsAndIsIdempotent() {
        seed.seed();
        String query="select p.id_ucesca,p.eksponat_id,p.izlozba_id,p.kontekstualni_opis,p.qr_token,d.id,d.putanja,d.tip_sadrzaja,d.datum_kreiranja from ucesce_eksponata_na_izlozbi p join digitalni_sadrzaj d on d.ucesce_id=p.id_ucesca where p.qr_token like 'DEMO-%' order by p.id_ucesca,d.id";
        var before=jdbc.queryForList(query);
        seed.seed();
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForList(query)).isEqualTo(before);
        var shared=jdbc.queryForList("select p.eksponat_id,p.izlozba_id,p.kontekstualni_opis,d.putanja from ucesce_eksponata_na_izlozbi p join digitalni_sadrzaj d on d.ucesce_id=p.id_ucesca where p.qr_token in ('DEMO-CITY-QR-001','DEMO-LIGHT-CITY-QR-001') and d.tip_sadrzaja='TEKST' order by p.id_ucesca");
        org.assertj.core.api.Assertions.assertThat(shared).hasSize(2);
        org.assertj.core.api.Assertions.assertThat(shared.get(0).get("eksponat_id")).isEqualTo(shared.get(1).get("eksponat_id"));
        for(String field:new String[]{"izlozba_id","kontekstualni_opis","putanja"})
            org.assertj.core.api.Assertions.assertThat(shared.get(0).get(field)).isNotEqualTo(shared.get(1).get(field));
        new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(status->{
            jdbc.update("update ucesce_eksponata_na_izlozbi set kontekstualni_opis='Počnite obilazak svetlom jutra na gradskom trgu.' where qr_token='DEMO-CITY-QR-001'");
            seed.seed();
            org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("select kontekstualni_opis from ucesce_eksponata_na_izlozbi where qr_token='DEMO-CITY-QR-001'",String.class)).contains("U izložbi Grad kroz objektiv");
            jdbc.update("update ucesce_eksponata_na_izlozbi set kontekstualni_opis='Izmena kustosa' where qr_token='DEMO-CITY-QR-001'");
            seed.seed();
            org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("select kontekstualni_opis from ucesce_eksponata_na_izlozbi where qr_token='DEMO-CITY-QR-001'",String.class)).isEqualTo("Izmena kustosa");
            status.setRollbackOnly();
        });
    }
}
