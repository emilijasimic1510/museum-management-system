package rs.ac.uns.ftn.museum.people;

import com.fasterxml.jackson.databind.*;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import rs.ac.uns.ftn.museum.people.domain.*;
import rs.ac.uns.ftn.museum.support.PostgresTestBase;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("test")
class PeopleIntegrationTests extends PostgresTestBase {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    @Autowired EntityManager em;
    @Autowired JwtEncoder encoder;
    @Autowired PlatformTransactionManager transactions;

    private record User(long id, String email, String password, String token) {
        @Override public String toString() { return "Test user [credentials redacted]"; }
    }
    private JsonNode call(MockHttpServletRequestBuilder request, User user, Object body, int expected) throws Exception {
        if (user != null) request.header("Authorization", "Bearer " + user.token());
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
        String response = mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return response.isBlank() ? json.nullNode() : json.readTree(response);
    }
    private User user(String... roles) throws Exception {
        String email = UUID.randomUUID() + "@example.test";
        String password = UUID.randomUUID().toString();
        JsonNode account = call(post("/api/auth/register"), null,
                Map.of("email", email, "password", password, "firstName", "Test", "lastName", "Visitor", "recommendationsConsent", false), 201);
        long id = account.get("id").asLong();
        for (String role : roles) jdbc.update("insert into nalog_uloga(nalog_id,uloga_id,datum_dodele) select ?,id,now() from uloga where naziv=? on conflict do nothing", id, role);
        String token = call(post("/api/auth/login"), null, Map.of("email", email, "password", password), 200).get("accessToken").asText();
        return new User(id, email, password, token);
    }
    private long museum() {
        return jdbc.queryForObject("insert into muzej(naziv,adresa,aktivan) values ('Test','Test',true) returning id", Long.class);
    }
    private long exhibition() {
        return new TransactionTemplate(transactions).execute(status -> {
            long hall = jdbc.queryForObject("insert into sala(muzej_id,naziv) values (?,'Test') returning id", Long.class, museum());
            long id = jdbc.queryForObject("insert into izlozba(sala_id,naziv,datum_pocetka,datum_zavrsetka,status,online_vidljiva) values (?,'Test','2030-01-01Z','2030-02-01Z','PLANIRANA',true) returning id", Long.class, hall);
            jdbc.update("insert into termin_izlozbe(izlozba_id,pocetak,kraj,kapacitet,status) values (?,'2030-01-02Z','2030-01-03Z',20,'OTVOREN')", id);
            return id;
        });
    }
    private Map<String,Object> eventBody(long exhibition, int capacity) {
        return Map.of("exhibitionId", exhibition, "name", "Test event", "type", "TURA",
                "start", "2030-01-05T10:00:00Z", "end", "2030-01-05T11:00:00Z", "capacity", capacity, "status", "PLANIRAN");
    }
    private Map<String,Object> employeeBody(User user, long museum, EmployeeType type) {
        return new HashMap<>(Map.of("accountId", user.id(), "museumId", museum, "firstName", "Test", "lastName", "Employee",
                "hiredOn", "2026-01-01", "type", type.name()));
    }
    @Test void guideAuthorizationProfileCrudAndPrivateRegistrations() throws Exception {
        User guide=user("GUIDE"), admin=user("ADMINISTRATOR"), visitor=user();
        long guideId=call(post("/api/employees"),admin,employeeBody(guide,museum(),EmployeeType.GUIDE),201).path("id").asLong();
        assertThat(call(get("/api/guides/me"),guide,null,200).path("id").asLong()).isEqualTo(guideId);
        call(get("/api/guides/me"),null,null,401);
        call(get("/api/admin/accounts"),guide,null,403);
        call(get("/api/employees"),guide,null,403);
        var body=new HashMap<>(eventBody(exhibition(),3));body.put("guideId",guideId);
        long event=call(post("/api/events"),guide,body,201).path("id").asLong();
        assertThat(call(get("/api/events"),null,null,200).isArray()).isTrue();
        call(get("/api/events/"+event+"/registrations"),null,null,401);
        call(post("/api/events"),null,body,401);
        call(put("/api/events/"+event),null,body,401);
        for(User denied:List.of(visitor,user("CASHIER"),user("CURATOR"),admin)) {
            call(get("/api/guides/me"),denied,null,403);
            call(post("/api/events"),denied,body,403);
            call(put("/api/events/"+event),denied,body,403);
            call(delete("/api/events/"+event),denied,null,403);
            call(get("/api/events/"+event+"/registrations"),denied,null,403);
            call(put("/api/events/"+event+"/registrations/1"),denied,Map.of("status","OTKAZAN"),403);
        }
        body.put("name","Updated event");
        assertThat(call(put("/api/events/"+event),guide,body,200).path("name").asText()).isEqualTo("Updated event");
        body.put("exhibitionId",Long.MAX_VALUE);call(post("/api/events"),guide,body,404);
        body.put("exhibitionId",exhibition());body.put("guideId",Long.MAX_VALUE);call(post("/api/events"),guide,body,404);
        body.put("guideId",guideId);body.put("capacity",-1);call(post("/api/events"),guide,body,400);
        body.put("capacity",3);body.put("end",body.get("start"));call(post("/api/events"),guide,body,400);
        call(delete("/api/events/"+event),guide,null,204);
        call(get("/api/events/"+event),null,null,404);
    }
    @Test void visitorIdentityComesFromSessionAndAttendanceCannotBeCancelled() throws Exception {
        User guide=user("GUIDE"), first=user(),second=user();
        long event=call(post("/api/events"),guide,eventBody(exhibition(),2),201).path("id").asLong();
        long firstId=call(get("/api/visitors/me"),first,null,200).path("id").asLong();
        long secondId=call(get("/api/visitors/me"),second,null,200).path("id").asLong();
        call(post("/api/events/"+event+"/registrations"),null,null,401);
        var registration=call(post("/api/events/"+event+"/registrations"),first,Map.of("visitorId",secondId,"status","PRISUSTVOVAO"),201);
        assertThat(registration.path("visitorId").asLong()).isEqualTo(firstId);
        assertThat(registration.path("status").asText()).isEqualTo("PRIJAVLJEN");
        assertThat(call(get("/api/visitors/me/registrations"),second,null,200)).isEmpty();
        call(delete("/api/events/"+event+"/registrations/me"),second,null,404);
        call(delete("/api/events/"+event+"/registrations/"+firstId),second,null,403);
        call(put("/api/events/"+event+"/registrations/"+firstId),guide,Map.of("status","UNKNOWN"),400);
        call(put("/api/events/"+event+"/registrations/"+firstId),guide,Map.of("status","PRISUSTVOVAO"),200);
        call(delete("/api/events/"+event+"/registrations/me"),first,null,409);
    }
    @Test void unavailableAndPastEventsRejectRegistration() throws Exception {
        User guide=user("GUIDE"),visitor=user();long exhibition=exhibition();
        for(String status:List.of("OTKAZAN","ZAVRSEN","POPUNJEN")) {
            var body=new HashMap<>(eventBody(exhibition,10));body.put("status",status);
            long event=call(post("/api/events"),guide,body,201).path("id").asLong();
            call(post("/api/events/"+event+"/registrations"),visitor,null,409);
        }
        var body=new HashMap<>(eventBody(exhibition,10));body.put("start",Instant.now().minusSeconds(7200).toString());body.put("end",Instant.now().minusSeconds(3600).toString());
        long past=call(post("/api/events"),guide,body,201).path("id").asLong();
        call(post("/api/events/"+past+"/registrations"),visitor,null,409);
        body.put("end",Instant.now().plusSeconds(3600).toString());
        long started=call(post("/api/events"),guide,body,201).path("id").asLong();
        call(post("/api/events/"+started+"/registrations"),visitor,null,409);
    }
    @Test void migrationsMappingsAndEmailOnlySchema() {
        assertThat(jdbc.queryForList("select version from flyway_schema_history where success and version in ('1','2') order by installed_rank", String.class)).containsExactly("1","2");
        assertThat(em.getMetamodel().getEntities().stream().filter(e->!e.getJavaType().getPackageName().contains(".journey."))).hasSize(24);
        assertThat(jdbc.queryForObject("select count(*) from information_schema.tables where table_schema='public' and table_name not in ('flyway_schema_history','ulaznica','transakcija','poseta','skeniranje_eksponata','ocena_izlozbe','preporucena_ruta','stavka_rute')", Integer.class)).isEqualTo(24);
        assertThat(jdbc.queryForList("select column_name from information_schema.columns where table_name='korisnicki_nalog'", String.class))
                .containsExactlyInAnyOrder("id","email","lozinka_hash","aktivan","datum_registracije","poslednja_prijava");
    }
    @Test void registrationEmailUniquenessHashAndLogin() throws Exception {
        User visitor = user();
        String hash = jdbc.queryForObject("select lozinka_hash from korisnicki_nalog where id=?", String.class, visitor.id());
        assertThat(passwords.matches(visitor.password(), hash)).isTrue();
        JsonNode me = call(get("/api/accounts/me"), visitor, null, 200);
        assertThat(me.has("passwordHash") || me.has("password") || me.has("username")).isFalse();
        assertThat(me.get("roles").toString()).isEqualTo("[\"VISITOR\"]");
        assertThat(me.get("lastLogin").isNull()).isFalse();
        call(post("/api/auth/register"), null, Map.of("email", visitor.email().toUpperCase(Locale.ROOT), "password", UUID.randomUUID().toString(),
                "firstName", "Test", "lastName", "Test", "recommendationsConsent", false), 409);
        call(post("/api/auth/login"), null, Map.of("email", visitor.email().toUpperCase(Locale.ROOT), "password", visitor.password()), 200);
        call(post("/api/auth/login"), null, Map.of("email", visitor.email(), "password", UUID.randomUUID().toString()), 401);
        call(post("/api/auth/register"), null, Map.of("email", "invalid"), 400);
        assertThatThrownBy(() -> jdbc.update("update korisnicki_nalog set lozinka_hash='plaintext' where id=?", visitor.id())).isInstanceOf(RuntimeException.class);
        User second = user();
        assertThatThrownBy(() -> jdbc.update("update korisnicki_nalog set email=? where id=?", visitor.email().toUpperCase(Locale.ROOT), second.id())).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> jdbc.update("update korisnicki_nalog set email=null where id=?", second.id())).isInstanceOf(RuntimeException.class);
    }
    @Test void jwtValidationLiveRolesAndAccountDeactivation() throws Exception {
        User visitor = user();
        call(get("/api/admin/accounts"), null, null, 401);
        call(get("/api/admin/accounts"), visitor, null, 403);
        mvc.perform(get("/api/accounts/me").header("Authorization","Bearer invalid")).andExpect(status().isUnauthorized());
        Instant now = Instant.now();
        for (String issuer : List.of("museum-backend", "wrong-issuer")) {
            JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer).subject(Long.toString(visitor.id()))
                    .issuedAt(now.minusSeconds(7200)).expiresAt(issuer.equals("museum-backend") ? now.minusSeconds(3600) : now.plusSeconds(3600)).build();
            String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
            mvc.perform(get("/api/accounts/me").header("Authorization","Bearer " + token)).andExpect(status().isUnauthorized());
        }
        jdbc.update("insert into nalog_uloga(nalog_id,uloga_id,datum_dodele) select ?,id,now() from uloga where naziv='ADMINISTRATOR'", visitor.id());
        call(get("/api/admin/accounts"), visitor, null, 200);
        jdbc.update("delete from nalog_uloga where nalog_id=? and uloga_id=(select id from uloga where naziv='ADMINISTRATOR')", visitor.id());
        call(get("/api/admin/accounts"), visitor, null, 403);
        jdbc.update("update korisnicki_nalog set aktivan=false where id=?", visitor.id());
        call(get("/api/accounts/me"), visitor, null, 401);
        call(post("/api/auth/login"), null, Map.of("email",visitor.email(),"password",visitor.password()), 401);
    }
    @ParameterizedTest @EnumSource(EmployeeType.class)
    void joinedEmployeeSubtypeAndProfileUniqueness(EmployeeType type) throws Exception {
        User admin = user("ADMINISTRATOR");
        User target = user();
        Map<String,Object> body = employeeBody(target, museum(), type);
        String field = switch(type) {
            case CURATOR -> "specialization"; case GUIDE -> "primaryLanguage"; case CASHIER -> "counterLabel";
            case MANAGER -> "reportingLevel"; case ADMINISTRATOR -> "administrationLevel";
        };
        body.put(field, "Test attribute");
        long id = call(post("/api/employees"), admin, body, 201).get("id").asLong();
        JsonNode employee = call(get("/api/employees/"+id), admin, null, 200);
        assertThat(employee.get("type").asText()).isEqualTo(type.name());
        assertThat(employee.get(field).asText()).isEqualTo("Test attribute");
        call(post("/api/employees"), admin, body, 409);
        call(put("/api/employees/"+id), admin, body, 200);
        assertThat(jdbc.queryForObject("select count(*) from posetilac where nalog_id=?", Integer.class, target.id())).isEqualTo(1);
        String table = switch(type) { case CURATOR -> "kustos"; case GUIDE -> "vodic"; case CASHIER -> "blagajnik"; case MANAGER -> "menadzer"; case ADMINISTRATOR -> "administrator"; };
        assertThatThrownBy(() -> jdbc.update("delete from " + table + " where zaposleni_id=?", id)).isInstanceOf(RuntimeException.class);
        String other = type == EmployeeType.GUIDE ? "kustos" : "vodic";
        assertThatThrownBy(() -> jdbc.update("insert into " + other + "(zaposleni_id) values (?)", id)).isInstanceOf(RuntimeException.class);
        call(delete("/api/employees/"+id), admin, null, 204);
        call(get("/api/employees/"+id), admin, null, 404);
    }
    @Test void employeeRequiresSubtypeAndSupervisorCannotCycle() throws Exception {
        User admin = user("ADMINISTRATOR");
        User a = user(); User b = user();
        long museum = museum();
        assertThatThrownBy(() -> jdbc.update("insert into zaposleni(nalog_id,muzej_id,ime,prezime,datum_zaposlenja,tip_zaposlenog) values (?,?,'Test','Test',current_date,'GUIDE')", a.id(), museum)).isInstanceOf(RuntimeException.class);
        var first = employeeBody(a,museum,EmployeeType.GUIDE);
        long firstId = call(post("/api/employees"),admin,first,201).get("id").asLong();
        var second = employeeBody(b,museum,EmployeeType.MANAGER); second.put("supervisorId",firstId);
        long secondId = call(post("/api/employees"),admin,second,201).get("id").asLong();
        first.put("supervisorId",secondId);
        call(put("/api/employees/"+firstId),admin,first,409);
        first.put("supervisorId",Long.MAX_VALUE);
        call(put("/api/employees/"+firstId),admin,first,404);
        call(delete("/api/employees/"+firstId),admin,null,409);
        call(get("/api/employees"),a,null,403);
    }
    @Test void roleAssignmentsMinimumOneAndAdministrativeCrud() throws Exception {
        User admin = user("ADMINISTRATOR"); User visitor = user();
        long guide = jdbc.queryForObject("select id from uloga where naziv='GUIDE'",Long.class);
        long visitorRole = jdbc.queryForObject("select id from uloga where naziv='VISITOR'",Long.class);
        call(get("/api/admin/roles"),admin,null,200);
        call(post("/api/admin/accounts/"+visitor.id()+"/roles/"+guide),admin,null,200);
        assertThat(jdbc.queryForObject("select dodelio_id from nalog_uloga where nalog_id=? and uloga_id=?",Long.class,visitor.id(),guide)).isEqualTo(admin.id());
        call(post("/api/admin/accounts/"+visitor.id()+"/roles/"+guide),admin,null,409);
        call(delete("/api/admin/accounts/"+visitor.id()+"/roles/"+guide),admin,null,204);
        call(delete("/api/admin/accounts/"+visitor.id()+"/roles/"+visitorRole),admin,null,409);
        assertThatThrownBy(() -> jdbc.update("delete from nalog_uloga where nalog_id=?",visitor.id())).isInstanceOf(RuntimeException.class);
        call(get("/api/admin/accounts/"+Long.MAX_VALUE),admin,null,404);
        call(put("/api/admin/accounts/"+visitor.id()),admin,Map.of("email",visitor.email(),"active",true),200);
        call(delete("/api/admin/accounts/"+visitor.id()),admin,null,409);
        call(post("/api/admin/roles"),admin,Map.of("name","GUIDE"),409);
        call(put("/api/admin/roles/"+guide),admin,Map.of("name","GUIDE","description","Guided events"),200);
        call(get("/api/admin/roles/"+guide),admin,null,200);
    }
    @Test void interestsAndVisitorProfileArePrivateAndUseCompositeKeys() throws Exception {
        User curator = user("CURATOR"); User visitor = user(); User admin = user("ADMINISTRATOR");
        long interest = call(post("/api/interests"),curator,Map.of("name",UUID.randomUUID().toString()),201).get("id").asLong();
        call(get("/api/interests/"+interest),null,null,200);
        call(put("/api/visitors/me/interests/"+interest),visitor,Map.of("level",3),200);
        call(put("/api/visitors/me/interests/"+interest),visitor,Map.of("level",4),200);
        assertThat(call(get("/api/visitors/me/interests"),visitor,null,200).size()).isEqualTo(1);
        call(delete("/api/interests/"+interest),curator,null,409);
        call(delete("/api/visitors/me/interests/"+interest),visitor,null,204);
        call(delete("/api/visitors/me/interests/"+interest),visitor,null,404);
        call(put("/api/interests/"+interest),curator,Map.of("name",UUID.randomUUID().toString()),200);
        call(delete("/api/interests/"+interest),curator,null,204);
        JsonNode profile = call(get("/api/visitors/me"),visitor,null,200);
        call(get("/api/visitors/"+profile.get("id").asLong()),curator,null,403);
        call(put("/api/visitors/me"),visitor,Map.of("firstName","Updated","lastName","Visitor","recommendationsConsent",true),200);
        call(post("/api/visitors"),admin,Map.of("accountId",visitor.id(),"firstName","Test","lastName","Test","recommendationsConsent",false),409);
        call(delete("/api/visitors/"+profile.get("id").asLong()),admin,null,204);
        call(delete("/api/admin/accounts/"+visitor.id()),admin,null,204);
    }
    @Test void eventRegistrationCapacityCancellationAndErrors() throws Exception {
        User guide = user("GUIDE"); User first = user(); User second = user();
        long exhibition = exhibition();
        Map<String,Object> body = eventBody(exhibition,1);
        long id = call(post("/api/events"),guide,body,201).get("id").asLong();
        call(post("/api/events"),first,body,403);
        call(get("/api/events/"+id),null,null,200);
        call(post("/api/events/"+id+"/registrations"),first,null,201);
        call(post("/api/events/"+id+"/registrations"),first,null,409);
        call(post("/api/events/"+id+"/registrations"),second,null,409);
        call(put("/api/events/"+id),guide,eventBody(exhibition,0),409);
        call(get("/api/events/"+id+"/registrations"),first,null,403);
        var attendees=call(get("/api/events/"+id+"/registrations"),guide,null,200);
        assertThat(attendees.size()).isEqualTo(1);
        var profile=call(get("/api/visitors/me"),first,null,200);
        assertThat(attendees.get(0).path("firstName")).isEqualTo(profile.path("firstName"));
        assertThat(attendees.get(0).path("lastName")).isEqualTo(profile.path("lastName"));
        assertThat(attendees.get(0).has("email") || attendees.get(0).has("phone") || attendees.get(0).has("passwordHash")).isFalse();
        call(delete("/api/events/"+id+"/registrations/me"),first,null,204);
        call(post("/api/events/"+id+"/registrations"),second,null,201);
        call(delete("/api/events/"+id+"/registrations/me"),second,null,204);
        call(post("/api/events/"+id+"/registrations"),first,null,201);
        long visitorId = call(get("/api/visitors/me"),first,null,200).get("id").asLong();
        call(put("/api/events/"+id+"/registrations/"+visitorId),guide,Map.of("status","PRISUSTVOVAO"),200);
        call(delete("/api/events/"+id+"/registrations/me"),first,null,409);
        call(delete("/api/events/"+id),guide,null,409);
        call(post("/api/events/"+Long.MAX_VALUE+"/registrations"),first,null,404);
        call(get("/api/visitors/me/registrations"),first,null,200);
        call(put("/api/events/"+id),guide,eventBody(exhibition,2),200);
        assertThat(call(get("/api/events/"+id),null,null,200).get("status").asText()).isEqualTo("PLANIRAN");
        long disposable = call(post("/api/events"),guide,eventBody(exhibition,0),201).get("id").asLong();
        call(post("/api/events/"+disposable+"/registrations"),second,null,409);
        call(delete("/api/events/"+disposable),guide,null,204);
    }
    @Test void databaseCapacityCompositeKeyAndGuideForeignKey() throws Exception {
        User guide = user("GUIDE"); User first = user(); User second = user(); User admin = user("ADMINISTRATOR");
        long exhibition = exhibition();
        var employee = employeeBody(guide,museum(),EmployeeType.GUIDE);
        long employeeId = call(post("/api/employees"),admin,employee,201).get("id").asLong();
        var body = new HashMap<String,Object>(eventBody(exhibition,1)); body.put("guideId",employeeId);
        long id = call(post("/api/events"),guide,body,201).get("id").asLong();
        assertThat(call(get("/api/events/"+id),null,null,200).get("guideId").asLong()).isEqualTo(employeeId);
        long firstId = call(get("/api/visitors/me"),first,null,200).get("id").asLong();
        long secondId = call(get("/api/visitors/me"),second,null,200).get("id").asLong();
        jdbc.update("insert into prijava_na_dogadjaj values (?,?,now(),'PRIJAVLJEN')",firstId,id);
        assertThatThrownBy(() -> jdbc.update("insert into prijava_na_dogadjaj values (?,?,now(),'PRIJAVLJEN')",firstId,id)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> jdbc.update("insert into prijava_na_dogadjaj values (?,?,now(),'PRIJAVLJEN')",secondId,id)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> jdbc.update("update dogadjaj set kapacitet=0 where id=?",id)).isInstanceOf(RuntimeException.class);
        call(delete("/api/employees/"+employeeId),admin,null,409);
        body.put("guideId",Long.MAX_VALUE);
        call(post("/api/events"),guide,body,404);
        body.remove("guideId"); body.put("status","OTKAZAN");
        long cancelled = call(post("/api/events"),guide,body,201).get("id").asLong();
        call(post("/api/events/"+cancelled+"/registrations"),first,null,409);
    }
    @ParameterizedTest @EnumSource(RoleName.class)
    void protectedOperationsFollowRoleMatrix(RoleName role) throws Exception {
        User account = user(role.name());
        call(get("/api/employees"),account,null,role==RoleName.ADMINISTRATOR ? 200 : 403);
        call(post("/api/events"),account,eventBody(exhibition(),3),role==RoleName.GUIDE ? 201 : 403);
        call(get("/api/admin/roles"),account,null,role==RoleName.ADMINISTRATOR ? 200 : 403);
    }
    @Test void concurrentVisitorsCannotOverbookLastPlace() throws Exception {
        User guide = user("GUIDE"); User first = user(); User second = user();
        long id = call(post("/api/events"),guide,eventBody(exhibition(),1),201).get("id").asLong();
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<Integer>> results = new ArrayList<>();
            for (User visitor : List.of(first,second)) results.add(executor.submit(() -> {
                start.await();
                return mvc.perform(post("/api/events/"+id+"/registrations").header("Authorization","Bearer "+visitor.token())).andReturn().getResponse().getStatus();
            }));
            start.countDown();
            assertThat(List.of(results.get(0).get(30,TimeUnit.SECONDS),results.get(1).get(30,TimeUnit.SECONDS))).containsExactlyInAnyOrder(201,409);
        }
        assertThat(jdbc.queryForObject("select count(*) from prijava_na_dogadjaj where dogadjaj_id=? and status<>'OTKAZAN'",Integer.class,id)).isEqualTo(1);
    }
}
