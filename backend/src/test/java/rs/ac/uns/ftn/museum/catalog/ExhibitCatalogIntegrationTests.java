package rs.ac.uns.ftn.museum.catalog;

import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import rs.ac.uns.ftn.museum.support.CatalogAuthTestBase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(print=MockMvcPrint.NONE)
@ActiveProfiles("test")
class ExhibitCatalogIntegrationTests extends CatalogAuthTestBase {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;

    private JsonNode call(MockHttpServletRequestBuilder request, String token, Object body, int expected) throws Exception {
        if (token!=null) request.header("Authorization","Bearer "+token);
        if (body!=null) request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
        String result=mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return result.isBlank()?json.nullNode():json.readTree(result);
    }
    private long exhibition(boolean visible) {
        return new TransactionTemplate(transactions).execute(status -> {
            long museum=jdbc.queryForObject("insert into muzej(naziv,adresa,aktivan) values ('Museum','Address',true) returning id",Long.class);
            long hall=jdbc.queryForObject("insert into sala(muzej_id,naziv) values (?,'Hall') returning id",Long.class,museum);
            long id=jdbc.queryForObject("insert into izlozba(sala_id,naziv,datum_pocetka,datum_zavrsetka,status,online_vidljiva) values (?,'Exhibition','2030-01-01Z','2030-02-01Z','PLANIRANA',?) returning id",Long.class,hall,visible);
            jdbc.update("insert into termin_izlozbe(izlozba_id,pocetak,kraj,kapacitet,status) values (?,'2030-01-02Z','2030-01-03Z',20,'OTVOREN')",id);
            return id;
        });
    }
    private Map<String,Object> exhibitBody() {
        return Map.of("name","Keramička posuda","inventoryNumber",UUID.randomUUID().toString(),"description","Opis",
                "creationPeriod","19. vek","type","Posuda","material","Keramika","origin","Srbija");
    }
    private long exhibit(String token) throws Exception {
        return call(post("/api/exhibits"),token,exhibitBody(),201).path("exhibit").path("id").asLong();
    }
    private Map<String,Object> participationBody(long exhibit,long exhibition) {
        return Map.of("exhibitId",exhibit,"exhibitionId",exhibition,"contextDescription","Priča u ovoj izložbi",
                "qrToken",UUID.randomUUID().toString(),"qrPath","private/qr.png","tourOrder",1);
    }
    private Map<String,Object> contentBody(long participation) {
        return Map.of("participationId",participation,"type","AUDIO","path","media/audio.mp3","language","sr","version",1,"automaticallyGenerated",false);
    }

    @Test void publicExhibitListsDistinctVisibleExhibitionsAndSeparateContexts() throws Exception {
        String curator=tokenFor("CURATOR"); long e=exhibit(curator);
        assertThat(call(get("/api/exhibits/"+e),null,null,200).path("exhibitions")).isEmpty();
        long first=exhibition(true), second=exhibition(true), hidden=exhibition(false);
        for(long x:new long[]{first,first,second,hidden}) {
            call(post("/api/exhibit-participations"),curator,
                    Map.of("exhibitId",e,"exhibitionId",x,"contextDescription","Kontekst izložbe "+x),201);
        }
        JsonNode detail=call(get("/api/exhibits/"+e),null,null,200);
        assertThat(detail.path("exhibitions").findValuesAsText("id")).containsExactly(""+first,""+second);
        JsonNode list=call(get("/api/exhibits"),null,null,200);
        JsonNode row=null; for(JsonNode item:list)if(item.path("id").asLong()==e)row=item;
        assertThat(row).isEqualTo(detail);
        assertThat(row.findValues("qrToken")).isEmpty();
        assertThat(row.findValues("qrPath")).isEmpty();
        assertThat(row.findValues("inventoryNumber")).isEmpty();
        JsonNode entries=call(get("/api/exhibits/"+e+"/participations"),null,null,200);
        assertThat(entries).hasSize(3);
        assertThat(entries.get(0).path("contextDescription").asText()).isNotEqualTo(entries.get(2).path("contextDescription").asText());
    }

    @Test void publicCatalogReadsUseSafeProjections() throws Exception {
        String curator=tokenFor("CURATOR"); long exhibit=exhibit(curator), exhibition=exhibition(true);
        long author=call(post("/api/authors"),curator,Map.of("firstName","Ana","lastName","Anić","biography","Biografija"),201).path("id").asLong();
        jdbc.update("insert into autorstvo(autor_id,eksponat_id,vrsta_doprinosa,napomena) values (?,?,'Autor','INTERNAL-NOTE')",author,exhibit);
        long participation=call(post("/api/exhibit-participations"),curator,participationBody(exhibit,exhibition),201).path("id").asLong();
        call(post("/api/digital-content"),curator,contentBody(participation),201);
        for (String token: new String[]{null,tokenFor("VISITOR")}) {
            for (String path:List.of("/api/exhibits","/api/exhibits/"+exhibit,"/api/authors","/api/authors/"+author,
                    "/api/exhibits/"+exhibit+"/authorships","/api/exhibits/"+exhibit+"/participations","/api/exhibitions/"+exhibition+"/participations")) {
                JsonNode response=call(get(path),token,null,200);
                // Check keys recursively without including response data in assertion/log output.
                assertThat(response.findValues("qrToken").isEmpty()).isTrue();
                assertThat(response.findValues("qrPath").isEmpty()).isTrue();
                assertThat(response.findValues("inventoryNumber").isEmpty()).isTrue();
                assertThat(response.findValues("participationId").isEmpty()).isTrue();
                assertThat(response.findValues("path").isEmpty()).isTrue();
                assertThat(response.toString().contains("INTERNAL-NOTE")).isFalse();
            }
            assertThat(call(get("/api/exhibits/"+exhibit),token,null,200).path("authors").get(0).path("name").asText()).isEqualTo("Ana Anić");
            JsonNode p=call(get("/api/exhibits/"+exhibit+"/participations"),token,null,200).get(0);
            assertThat(p.has("id")).isFalse();
            assertThat(p.path("exhibition").path("id").asLong()).isEqualTo(exhibition);
            assertThat(p.path("contextDescription").asText()).isEqualTo("Priča u ovoj izložbi");
        }
    }

    @ParameterizedTest @ValueSource(strings={"CURATOR","ADMINISTRATOR"})
    void managementUsesCompositeAuthorshipKeyAndServerGeneratedQr(String role) throws Exception {
        String token=tokenFor(role); long e=exhibit(token), x=exhibition(true);
        long a=call(post("/api/authors"),token,Map.of("firstName","Novi autor"),201).path("id").asLong();
        String link="/api/exhibits/"+e+"/authorships/"+a;
        call(put(link),token,Map.of("contributionType","Koautor","note","Interna napomena"),200);
        call(put(link),token,Map.of("contributionType","Autor","note","Izmenjena napomena"),200);
        JsonNode links=call(get("/api/exhibits/"+e+"/authorships/management"),token,null,200);
        assertThat(links.size()).isEqualTo(1);
        assertThat(links.get(0).path("authorId").asLong()).isEqualTo(a);
        assertThat(links.get(0).path("exhibitId").asLong()).isEqualTo(e);
        assertThat(links.get(0).path("note").asText()).isEqualTo("Izmenjena napomena");
        call(put("/api/exhibits/"+e+"/authorships/9223372036854775807"),token,Map.of(),404);
        call(delete("/api/authors/"+a),token,null,409);
        JsonNode p=call(post("/api/exhibit-participations"),token,Map.of("exhibitionId",x,"exhibitId",e),201);
        JsonNode second=call(post("/api/exhibit-participations"),token,Map.of("exhibitionId",x,"exhibitId",e),201);
        assertThat(!p.path("qrToken").asText().isBlank()).isTrue();
        assertThat(!p.path("qrToken").asText().equals(second.path("qrToken").asText())).isTrue();
        JsonNode updated=call(put("/api/exhibit-participations/"+p.path("id").asLong()),token,Map.of("exhibitionId",x,"exhibitId",e,"contextDescription","Izmenjeno"),200);
        assertThat(updated.path("qrToken").asText().equals(p.path("qrToken").asText())).isTrue();
        assertThat(call(get("/api/exhibit-participations"),token,null,200).isArray()).isTrue();
        call(delete(link),token,null,204);
        assertThat(call(get("/api/exhibits/"+e+"/authorships/management"),token,null,200).size()).isZero();
        call(get("/api/authors/"+a),token,null,200);
        call(get("/api/exhibits/"+e),token,null,200);
        call(delete(link),token,null,404);
    }

    @Test void newManagementRoutesEnforceRoles() throws Exception {
        for(String role:Arrays.asList(null,"VISITOR","CASHIER","GUIDE")) {
            String token=role==null?null:tokenFor(role); int status=role==null?401:403;
            call(get("/api/exhibit-participations"),token,null,status);
            call(get("/api/exhibits/1/authorships/management"),token,null,status);
            call(put("/api/exhibits/1/authorships/1"),token,Map.of(),status);
            call(delete("/api/exhibits/1/authorships/1"),token,null,status);
        }
    }

    @Test void extraJsonCannotOverwriteServerOwnedFields() throws Exception {
        String token=tokenFor("CURATOR"); long e=exhibit(token),x=exhibition(true);
        Map<String,Object> body=new HashMap<>(participationBody(e,x));
        body.put("id",Long.MAX_VALUE); body.put("qrToken","client-supplied-value"); body.put("qrPath","client/path.png");
        JsonNode created=call(post("/api/exhibit-participations"),token,body,201);
        long id=created.path("id").asLong();
        assertThat(id).isNotEqualTo(Long.MAX_VALUE);
        assertThat(!created.path("qrToken").asText().equals("client-supplied-value")).isTrue();
        assertThat(created.path("qrPath").isNull()).isTrue();
        jdbc.update("update ucesce_eksponata_na_izlozbi set qr_putanja='server/path.png' where id_ucesca=?",id);
        body.put("qrToken","replacement-value");
        JsonNode updated=call(put("/api/exhibit-participations/"+id),token,body,200);
        assertThat(updated.path("qrToken").asText().equals(created.path("qrToken").asText())).isTrue();
        assertThat(updated.path("qrPath").asText().equals("server/path.png")).isTrue();
        Map<String,Object> content=new HashMap<>(contentBody(id));
        content.put("id",Long.MAX_VALUE);content.put("createdAt","1900-01-01T00:00:00Z");
        JsonNode c=call(post("/api/digital-content"),token,content,201);
        assertThat(c.path("id").asLong()).isNotEqualTo(Long.MAX_VALUE);
        assertThat(c.path("createdAt").asText()).isNotEqualTo("1900-01-01T00:00:00Z");
        JsonNode persisted=call(get("/api/digital-content/"+c.path("id").asLong()),token,null,200);
        content.put("createdAt","1901-01-01T00:00:00Z");
        JsonNode changed=call(put("/api/digital-content/"+c.path("id").asLong()),token,content,200);
        assertThat(changed.path("createdAt")).isEqualTo(persisted.path("createdAt"));
        assertThat(changed.path("participationId").asLong()).isEqualTo(id);
        JsonNode publicExhibit=call(get("/api/exhibits/"+e),null,null,200);
        assertThat(publicExhibit.findValues("qrToken")).isEmpty();
        assertThat(publicExhibit.findValues("qrPath")).isEmpty();
    }

    @ParameterizedTest @ValueSource(strings={"VISITOR","CASHIER","GUIDE","MANAGER"})
    void nonCatalogRolesCannotWriteOrReadManagement(String role) throws Exception {
        String token=tokenFor(role);
        for (String resource:List.of("exhibits","authors","exhibit-participations","digital-content")) {
            call(post("/api/"+resource),token,Map.of(),403);
            call(put("/api/"+resource+"/1"),token,Map.of(),403);
            call(delete("/api/"+resource+"/1"),token,null,403);
        }
        for (String path:List.of("/api/exhibits/1/management","/api/exhibit-participations?exhibitionId=1",
                "/api/exhibit-participations/1","/api/exhibit-participations/1/digital-content","/api/digital-content/1"))
            call(get(path),token,null,403);
    }

    @Test void anonymousCannotWriteOrReadManagement() throws Exception {
        for (String resource:List.of("exhibits","authors","exhibit-participations","digital-content","museums","halls","exhibitions","exhibition-terms")) {
            call(post("/api/"+resource),null,Map.of(),401);
            call(put("/api/"+resource+"/1"),null,Map.of(),401);
            call(delete("/api/"+resource+"/1"),null,null,401);
        }
        call(get("/api/exhibits/1/management"),null,null,401);
        call(get("/api/digital-content/1"),null,null,401);
        call(get("/api/exhibit-participations/1"),null,null,401);
    }

    @ParameterizedTest @ValueSource(strings={"CURATOR","ADMINISTRATOR"})
    void authorizedCrudPreservesRelationshipsAndRestrictiveDeletes(String role) throws Exception {
        String token=tokenFor(role); long exhibition=exhibition(true);
        JsonNode e=call(post("/api/exhibits"),token,exhibitBody(),201);
        long exhibit=e.path("exhibit").path("id").asLong();
        call(put("/api/exhibits/"+exhibit),token,Map.of("name","Promenjeno","inventoryNumber",e.path("inventoryNumber").asText()),200);
        assertThat(call(get("/api/exhibits/"+exhibit+"/management"),token,null,200).has("inventoryNumber")).isTrue();
        long author=call(post("/api/authors"),token,Map.of("firstName","Autor"),201).path("id").asLong();
        call(put("/api/authors/"+author),token,Map.of("firstName","Novi autor"),200);
        jdbc.update("insert into autorstvo(autor_id,eksponat_id) values (?,?)",author,exhibit);
        call(delete("/api/authors/"+author),token,null,409);
        Map<String,Object> pbody=new HashMap<>(participationBody(exhibit,exhibition));
        pbody.remove("qrToken");
        JsonNode created=call(post("/api/exhibit-participations"),token,pbody,201);
        long participation=created.path("id").asLong();
        assertThat(created.path("qrToken").asText().isBlank()).isFalse();
        pbody.put("contextDescription","Nov kontekst");
        JsonNode updated=call(put("/api/exhibit-participations/"+participation),token,pbody,200);
        assertThat(created.path("qrToken").equals(updated.path("qrToken"))).isTrue();
        call(get("/api/exhibit-participations/"+participation),token,null,200);
        assertThat(call(get("/api/exhibit-participations?exhibitionId="+exhibition),token,null,200).size()).isEqualTo(1);
        long content=call(post("/api/digital-content"),token,contentBody(participation),201).path("id").asLong();
        call(get("/api/digital-content/"+content),token,null,200);
        assertThat(call(get("/api/exhibit-participations/"+participation+"/digital-content"),token,null,200).size()).isEqualTo(1);
        Map<String,Object> cbody=new HashMap<>(contentBody(participation)); cbody.put("type","TEKST"); cbody.put("path","media/text.txt");
        call(put("/api/digital-content/"+content),token,cbody,200);
        call(delete("/api/exhibits/"+exhibit),token,null,409);
        call(delete("/api/exhibit-participations/"+participation),token,null,409);
        pbody.put("exhibitId",exhibit(token));
        call(put("/api/exhibit-participations/"+participation),token,pbody,409);
        call(delete("/api/digital-content/"+content),token,null,204);
        call(delete("/api/exhibit-participations/"+participation),token,null,204);
        jdbc.update("delete from autorstvo where autor_id=? and eksponat_id=?",author,exhibit);
        call(delete("/api/authors/"+author),token,null,204);
        call(delete("/api/exhibits/"+exhibit),token,null,204);
    }

    @Test void missingIdsAndInvalidBodiesReturn404And400() throws Exception {
        String token=tokenFor("CURATOR");
        for (String path:List.of("/api/exhibits/","/api/authors/","/api/exhibit-participations/","/api/digital-content/")) {
            call(get(path+Long.MAX_VALUE),token,null,404);
            call(delete(path+Long.MAX_VALUE),token,null,404);
        }
        for (String path:List.of("/api/exhibits/"+Long.MAX_VALUE+"/authorships","/api/exhibits/"+Long.MAX_VALUE+"/participations","/api/exhibitions/"+Long.MAX_VALUE+"/participations"))
            call(get(path),null,null,404);
        for (String resource:List.of("exhibits","authors","exhibit-participations","digital-content"))
            call(post("/api/"+resource),token,Map.of(),400);
        call(get("/api/exhibits/invalid"),null,null,400);
        long exhibit=exhibit(token), exhibition=exhibition(true);
        Map<String,Object> p=new HashMap<>(participationBody(exhibit,exhibition));
        p.put("exhibitId",Long.MAX_VALUE); call(post("/api/exhibit-participations"),token,p,404);
        p.put("exhibitId",exhibit); p.put("installedAt","2030-01-03T00:00:00Z"); p.put("removedAt","2030-01-02T00:00:00Z");
        call(post("/api/exhibit-participations"),token,p,400);
        long participation=call(post("/api/exhibit-participations"),token,participationBody(exhibit,exhibition),201).path("id").asLong();
        Map<String,Object> c=new HashMap<>(contentBody(participation));
        for (String bad:List.of("javascript:alert(1)","file:///private/file","../private/file","https://user:password@example.test/file","//example.test/file")) {
            c.put("path",bad); call(post("/api/digital-content"),token,c,400);
        }
        c.put("path","media/file.txt"); c.put("type","UNKNOWN"); call(post("/api/digital-content"),token,c,400);
        c.put("type","TEKST"); c.put("version",0); call(post("/api/digital-content"),token,c,400);
        Map<String,Object> duplicate=exhibitBody(); call(post("/api/exhibits"),token,duplicate,201); call(post("/api/exhibits"),token,duplicate,409);
    }

    @Test void hiddenExhibitionsStayOutOfPublicCatalogAndStaffCanManageThem() throws Exception {
        String token=tokenFor("CURATOR"); long exhibition=exhibition(false), exhibit=exhibit(token);
        long term=jdbc.queryForObject("select id from termin_izlozbe where izlozba_id=?",Long.class,exhibition);
        call(post("/api/exhibit-participations"),token,participationBody(exhibit,exhibition),201);
        call(get("/api/exhibitions/"+exhibition),null,null,404);
        call(get("/api/exhibition-terms/"+term),null,null,404);
        call(get("/api/exhibitions/"+exhibition+"/participations"),null,null,404);
        assertThat(call(get("/api/exhibits/"+exhibit+"/participations"),null,null,200).isEmpty()).isTrue();
        for (JsonNode e:call(get("/api/exhibitions"),null,null,200)) assertThat(e.path("id").asLong()).isNotEqualTo(exhibition);
        call(get("/api/exhibitions/"+exhibition),token,null,200);
        call(get("/api/exhibit-participations?exhibitionId="+exhibition),token,null,200);
    }

    @Test void legacyCatalogGetsArePublicAndWritesKeepTheirRoles() throws Exception {
        long exhibition=exhibition(true);
        long hall=jdbc.queryForObject("select sala_id from izlozba where id=?",Long.class,exhibition);
        long museum=jdbc.queryForObject("select muzej_id from sala where id=?",Long.class,hall);
        String visitor=tokenFor("VISITOR");
        for (String token:new String[]{null,visitor})
            for (String path:List.of("/api/museums","/api/museums/"+museum,"/api/halls","/api/halls/"+hall,"/api/exhibitions","/api/exhibitions/"+exhibition,"/api/exhibition-terms"))
                call(get(path),token,null,200);
        for (String resource:List.of("museums","halls","exhibitions","exhibition-terms")) {
            call(post("/api/"+resource),visitor,Map.of(),403);
            call(put("/api/"+resource+"/1"),visitor,Map.of(),403);
            call(delete("/api/"+resource+"/1"),visitor,null,403);
        }
        String curator=tokenFor("CURATOR");
        call(post("/api/museums"),curator,Map.of(),403);
        call(post("/api/halls"),curator,Map.of(),403);
        call(post("/api/exhibitions"),curator,Map.of(),400);
        call(post("/api/exhibition-terms"),curator,Map.of(),400);
    }

    @Test void hibernateValidatesUnchangedV1ToV3Schema() {
        assertThat(jdbc.queryForList("select version from flyway_schema_history where success order by installed_rank",String.class)).containsExactly("1","2","3");
        // Context startup uses ddl-auto=validate against the real PostgreSQL schema.
    }
}
