package rs.ac.uns.ftn.museum.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import rs.ac.uns.ftn.museum.catalog.domain.*;
import rs.ac.uns.ftn.museum.support.PostgresTestBase;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "local"})
class CatalogIntegrationTests extends PostgresTestBase {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactionManager;
    private static final String START = "2030-01-01T00:00:00Z";
    private static final String END = "2030-02-01T00:00:00Z";

    private JsonNode postJson(String route, Object body, int status) throws Exception {
        return json.readTree(mvc.perform(post(route).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().is(status)).andReturn().getResponse().getContentAsString());
    }
    private Map<String, Object> museumRequest() { return Map.of("name", "Test museum", "address", "Test address", "active", true); }
    private long museum() throws Exception { return postJson("/api/museums", museumRequest(), 201).get("id").asLong(); }
    private long hall(long museum) throws Exception {
        return postJson("/api/halls", Map.of("museumId", museum, "name", "Test hall", "capacity", 50), 201).get("id").asLong();
    }
    private Map<String, Object> term(long exhibition) { return Map.of("exhibitionId", exhibition, "start", "2030-01-05T10:00:00Z", "end", "2030-01-05T11:00:00Z", "capacity", 10, "status", "OTVOREN"); }
    private Map<String, Object> exhibitionRequest(long hall, boolean initial) {
        Map<String, Object> result = new HashMap<>(Map.of("hallId", hall, "name", "Test exhibition", "start", START, "end", END, "status", "PLANIRANA", "onlineVisible", true));
        if (initial) result.put("terms", List.of(Map.of("start", "2030-01-05T10:00:00Z", "end", "2030-01-05T11:00:00Z", "capacity", 10, "status", "OTVOREN")));
        return result;
    }
    private JsonNode exhibition(long hall) throws Exception { return postJson("/api/exhibitions", exhibitionRequest(hall, true), 201); }

    @Test void migrationAndHibernateValidateRealPostgres() {
        assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where version='1' and success", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from information_schema.tables where table_schema='public' and table_name in ('muzej','sala','izlozba','termin_izlozbe','eksponat','autor','autorstvo','ucesce_eksponata_na_izlozbi','digitalni_sadrzaj','cenovnik_izlozbe')", Integer.class)).isEqualTo(10);
        assertThat(em.getMetamodel().getEntities().stream().filter(e -> e.getJavaType().getPackageName().endsWith(".catalog.domain"))).hasSize(10);
    }

    @Test void fullCrudForAllFourResources() throws Exception {
        long m = museum(); long h = hall(m);
        JsonNode e = exhibition(h); long eid = e.get("id").asLong();
        long initialId = e.get("terms").get(0).get("id").asLong();
        long tid = postJson("/api/exhibition-terms", term(eid), 201).get("id").asLong();
        Map<String, Long> ids = Map.of("museums", m, "halls", h, "exhibitions", eid, "exhibition-terms", tid);
        for (var entry : ids.entrySet()) {
            mvc.perform(get("/api/" + entry.getKey())).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
            mvc.perform(get("/api/" + entry.getKey() + "/" + entry.getValue())).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(entry.getValue()));
        }
        mvc.perform(put("/api/museums/"+m).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("name","Changed museum","address","New address","active",false))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        mvc.perform(put("/api/halls/"+h).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("name","Changed hall","museumId",m,"floor",2))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.floor").value(2));
        mvc.perform(put("/api/exhibitions/"+eid).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(exhibitionRequest(h,false))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.terms.length()").value(2));
        Map<String,Object> updated = new HashMap<>(term(eid)); updated.put("capacity",0);
        mvc.perform(put("/api/exhibition-terms/"+tid).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(updated)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.capacity").value(0));
        mvc.perform(delete("/api/museums/"+m)).andExpect(status().isConflict());
        mvc.perform(delete("/api/halls/"+h)).andExpect(status().isConflict());
        mvc.perform(delete("/api/exhibition-terms/"+tid)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/exhibition-terms/"+initialId)).andExpect(status().isConflict());
        mvc.perform(delete("/api/exhibitions/"+eid)).andExpect(status().isNoContent());
        mvc.perform(get("/api/exhibition-terms/"+initialId)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/halls/"+h)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/museums/"+m)).andExpect(status().isNoContent());
    }

    @ParameterizedTest @ValueSource(strings={"museums","halls","exhibitions","exhibition-terms"})
    void notFoundAndInvalidBody(String resource) throws Exception {
        mvc.perform(get("/api/"+resource+"/9223372036854775807")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/"+resource+"/9223372036854775807")).andExpect(status().isNotFound());
        mvc.perform(post("/api/"+resource).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").isArray());
        mvc.perform(get("/api/"+resource+"/not-a-number")).andExpect(status().isBadRequest());
    }

    @Test void invalidParentsAndEnumAreRejected() throws Exception {
        postJson("/api/halls",Map.of("museumId",Long.MAX_VALUE,"name","A"),404);
        postJson("/api/exhibitions",exhibitionRequest(Long.MAX_VALUE,true),404);
        postJson("/api/exhibition-terms",term(Long.MAX_VALUE),404);
        Map<String,Object> bad=exhibitionRequest(1,true); bad.put("status","UNKNOWN");
        postJson("/api/exhibitions",bad,400);
    }

    @Test void overlapPeriodAndMinimumCardinalityAreEnforced() throws Exception {
        long h=hall(museum()); JsonNode e=exhibition(h); long id=e.get("id").asLong();
        postJson("/api/exhibitions",exhibitionRequest(h,true),409);
        Map<String,Object> noTerms=exhibitionRequest(h,true); noTerms.put("terms",List.of()); postJson("/api/exhibitions",noTerms,400);
        Map<String,Object> outside=new HashMap<>(term(id)); outside.put("start","2029-12-31T10:00:00Z"); postJson("/api/exhibition-terms",outside,409);
        Map<String,Object> negative=new HashMap<>(term(id)); negative.put("capacity",-1); postJson("/api/exhibition-terms",negative,400);
        Map<String,Object> shortened=exhibitionRequest(h,false); shortened.put("end","2030-01-02T00:00:00Z");
        mvc.perform(put("/api/exhibitions/"+id).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(shortened))).andExpect(status().isConflict());
        long other=exhibition(hall(museum())).get("id").asLong();
        mvc.perform(put("/api/exhibition-terms/"+e.get("terms").get(0).get("id").asLong()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(term(other)))).andExpect(status().isConflict());
        // Direct SQL cannot bypass minimum-one or containment constraints.
        TransactionTemplate tx=new TransactionTemplate(transactionManager);
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> jdbc.update("delete from termin_izlozbe where izlozba_id=?",id))).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> jdbc.update("update izlozba set datum_zavrsetka='2030-01-02T00:00:00Z' where id=?",id))).isInstanceOf(RuntimeException.class);
        assertThat(jdbc.queryForObject("select count(*) from termin_izlozbe where izlozba_id=?",Integer.class,id)).isEqualTo(1);
    }

    @Test void allCatalogMappingsCompositeKeysAndUniqueConstraints() throws Exception {
        long eid=exhibition(hall(museum())).get("id").asLong();
        TransactionTemplate tx=new TransactionTemplate(transactionManager);
        Long participationId=tx.execute(s -> {
            Exhibition exhibition=em.find(Exhibition.class,eid);
            Author author=new Author(); author.setFirstName("Test author"); em.persist(author);
            Exhibit exhibit=new Exhibit(); exhibit.setName("Test exhibit"); exhibit.setInventoryNumber(UUID.randomUUID().toString()); em.persist(exhibit);
            Authorship authorship=new Authorship(); authorship.setId(new AuthorshipId(author.getId(),exhibit.getId()));
            authorship.setAuthor(author); authorship.setExhibit(exhibit); authorship.setContributionType("autor"); em.persist(authorship);
            ExhibitionPrice price=new ExhibitionPrice(); price.setId(new ExhibitionPriceId(eid,Instant.parse(START),"redovna"));
            price.setExhibition(exhibition); price.setPrice(new BigDecimal("100.00")); em.persist(price);
            ExhibitParticipation p=new ExhibitParticipation(); p.setExhibition(exhibition); p.setExhibit(exhibit); p.setQrToken(UUID.randomUUID().toString()); p.setContextDescription("Exhibition-specific story"); em.persist(p);
            DigitalContent content=new DigitalContent(); content.setParticipation(p); content.setType(ContentType.TEKST);
            content.setPath("test/content.txt"); content.setVersion(1); content.setAutomaticallyGenerated(false); content.setCreatedAt(Instant.parse(START)); em.persist(content);
            em.flush(); em.clear();
            assertThat(em.find(Authorship.class,new AuthorshipId(author.getId(),exhibit.getId())).getContributionType()).isEqualTo("autor");
            assertThat(em.find(ExhibitionPrice.class,new ExhibitionPriceId(eid,Instant.parse(START),"redovna")).getPrice()).isEqualByComparingTo("100.00");
            assertThat(em.find(DigitalContent.class,content.getId()).getParticipation().getId()).isEqualTo(p.getId());
            return p.getId();
        });
        assertThatThrownBy(() -> jdbc.update("insert into ucesce_eksponata_na_izlozbi (izlozba_id,eksponat_id,qr_token) select izlozba_id,eksponat_id,qr_token from ucesce_eksponata_na_izlozbi where id_ucesca=?",participationId))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("insert into autorstvo select * from autorstvo limit 1"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("insert into cenovnik_izlozbe select * from cenovnik_izlozbe where izlozba_id=?",eid))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("update digitalni_sadrzaj set ucesce_id=null where ucesce_id=?",participationId))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        // Restrictive FK failure rolls back deletion of terms as well.
        mvc.perform(delete("/api/exhibitions/"+eid)).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select count(*) from termin_izlozbe where izlozba_id=?",Integer.class,eid)).isEqualTo(1);
    }

    @Test void localProfileKeepsHealthPublicAndOtherRoutesProtected() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.application").value("museum-backend"));
        mvc.perform(get("/api/private")).andExpect(status().isUnauthorized());
    }
}
