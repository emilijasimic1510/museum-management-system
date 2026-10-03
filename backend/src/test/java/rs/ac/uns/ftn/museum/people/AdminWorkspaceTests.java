package rs.ac.uns.ftn.museum.people;

import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import rs.ac.uns.ftn.museum.people.domain.EmployeeType;
import rs.ac.uns.ftn.museum.support.CatalogAuthTestBase;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest @AutoConfigureMockMvc(print=MockMvcPrint.NONE) @ActiveProfiles("test")
class AdminWorkspaceTests extends CatalogAuthTestBase {
    @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JdbcTemplate jdbc;
    private static final String PASSWORD="Admin-test-password-2035";
    private JsonNode call(MockHttpServletRequestBuilder req,String token,Object body,int expected) throws Exception {
        if(token!=null)req.header("Authorization","Bearer "+token);
        if(body!=null)req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
        String text=mvc.perform(req).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return text.isBlank()?json.nullNode():json.readTree(text);
    }
    private JsonNode create(String admin) throws Exception{return call(post("/api/admin/accounts"),admin,Map.of("email",UUID.randomUUID()+"@admin-test.local","password",PASSWORD,"firstName","Test","lastName","Employee","recommendationsConsent",false),201);}
    private void safeAccount(JsonNode a){List<String> fields=new ArrayList<>();a.fieldNames().forEachRemaining(fields::add);assertThat(fields).containsExactlyInAnyOrder("id","email","active","registeredAt","lastLogin","roles");assertThat(a.toString()).doesNotContain("password","lozinka","secret",PASSWORD);}
    @Test void adminOperationsRejectOtherRolesAndAnonymousRequests() throws Exception {
        for(String role:Arrays.asList(null,"MANAGER","CURATOR","CASHIER","GUIDE","VISITOR")){
            String token=role==null?null:tokenFor(role);int code=role==null?401:403;
            for(String path:List.of("/api/admin/accounts","/api/admin/roles","/api/employees"))call(get(path),token,null,code);
            call(post("/api/admin/accounts"),token,Map.of(),code);
            call(put("/api/admin/accounts/1"),token,Map.of(),code);
            call(post("/api/admin/accounts/1/roles/1"),token,null,code);
            call(delete("/api/admin/accounts/1/roles/1"),token,null,code);
            call(post("/api/employees"),token,Map.of(),code);call(put("/api/employees/1"),token,Map.of(),code);
        }
        String admin=tokenFor("ADMINISTRATOR");
        call(get("/api/admin/accounts"),admin,null,200);call(get("/api/admin/roles"),admin,null,200);call(get("/api/employees"),admin,null,200);
    }
    @Test void activationAndDeactivationInvalidateSessionsWithoutDeletingHistoryOrExposingCredentials() throws Exception {
        String admin=tokenFor("ADMINISTRATOR");JsonNode a=create(admin);safeAccount(a);long id=a.path("id").asLong();String email=a.path("email").asText();
        String token=call(post("/api/auth/login"),null,Map.of("email",email,"password",PASSWORD),200).path("accessToken").asText();
        JsonNode disabled=call(put("/api/admin/accounts/"+id),admin,Map.of("email",email,"active",false),200);safeAccount(disabled);assertThat(disabled.path("active").asBoolean()).isFalse();
        call(get("/api/accounts/me"),token,null,401);call(post("/api/auth/login"),null,Map.of("email",email,"password",PASSWORD),401);
        call(delete("/api/admin/accounts/"+id),admin,null,409);
        safeAccount(call(get("/api/admin/accounts/"+id),admin,null,200));
        call(put("/api/admin/accounts/"+id),admin,Map.of("email",email,"active",true),200);call(get("/api/accounts/me"),token,null,200);
        for(JsonNode row:call(get("/api/admin/accounts"),admin,null,200))safeAccount(row);
    }
    @Test void roleAssignmentRecordsActorAndDateAndLastRoleCannotBeRemoved() throws Exception {
        String admin=tokenFor("ADMINISTRATOR");long actor=call(get("/api/accounts/me"),admin,null,200).path("id").asLong();long account=create(admin).path("id").asLong();
        long manager=jdbc.queryForObject("select id from uloga where naziv='MANAGER'",Long.class),visitor=jdbc.queryForObject("select id from uloga where naziv='VISITOR'",Long.class);
        safeAccount(call(post("/api/admin/accounts/"+account+"/roles/"+manager),admin,null,200));
        assertThat(jdbc.queryForObject("select dodelio_id from nalog_uloga where nalog_id=? and uloga_id=?",Long.class,account,manager)).isEqualTo(actor);
        assertThat(jdbc.queryForObject("select datum_dodele from nalog_uloga where nalog_id=? and uloga_id=?",java.sql.Timestamp.class,account,manager)).isNotNull();
        call(post("/api/admin/accounts/"+account+"/roles/"+manager),admin,null,409);
        call(delete("/api/admin/accounts/"+account+"/roles/"+visitor),admin,null,204);call(delete("/api/admin/accounts/"+account+"/roles/"+manager),admin,null,409);
        assertThat(call(get("/api/admin/accounts/"+account),admin,null,200).path("roles").toString()).isEqualTo("[\"MANAGER\"]");
        assertThat(jdbc.queryForObject("select count(*) from zaposleni where nalog_id=?",Long.class,account)).isZero();
    }
    @ParameterizedTest @EnumSource(EmployeeType.class)
    void employeeHasOneImmutableSubtypeAndDoesNotAcquireRoleImplicitly(EmployeeType type) throws Exception {
        String admin=tokenFor("ADMINISTRATOR");long account=create(admin).path("id").asLong();
        long museum=call(post("/api/museums"),admin,Map.of("name","Admin test museum","address","Adresa","active",true),201).path("id").asLong();
        Map<String,Object> body=new HashMap<>(Map.of("accountId",account,"museumId",museum,"firstName","Employee","lastName","Test","hiredOn","2026-01-01","type",type.name()));
        String field=switch(type){case CURATOR->"specialization";case GUIDE->"primaryLanguage";case CASHIER->"counterLabel";case MANAGER->"reportingLevel";case ADMINISTRATOR->"administrationLevel";};body.put(field,"Test detail");
        long id=call(post("/api/employees"),admin,body,201).path("id").asLong();
        call(post("/api/employees"),admin,body,409);body.put("phone","0601234567");call(put("/api/employees/"+id),admin,body,200);
        String table=switch(type){case CURATOR->"kustos";case GUIDE->"vodic";case CASHIER->"blagajnik";case MANAGER->"menadzer";case ADMINISTRATOR->"administrator";};
        for(String subtype:List.of("kustos","vodic","blagajnik","menadzer","administrator"))assertThat(jdbc.queryForObject("select count(*) from "+subtype+" where zaposleni_id=?",Long.class,id)).isEqualTo(subtype.equals(table)?1L:0L);
        String other=table.equals("vodic")?"kustos":"vodic";
        assertThatThrownBy(()->jdbc.update("insert into "+other+"(zaposleni_id) values(?)",id)).isInstanceOf(RuntimeException.class);
        body.put("type",type==EmployeeType.GUIDE?"CURATOR":"GUIDE");call(put("/api/employees/"+id),admin,body,409);
        assertThat(call(get("/api/admin/accounts/"+account),admin,null,200).path("roles").toString()).isEqualTo("[\"VISITOR\"]");
        assertThat(call(get("/api/employees/"+id),admin,null,200).path("type").asText()).isEqualTo(type.name());
    }
}
