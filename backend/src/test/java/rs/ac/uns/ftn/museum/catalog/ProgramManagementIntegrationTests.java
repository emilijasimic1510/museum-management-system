package rs.ac.uns.ftn.museum.catalog;

import com.fasterxml.jackson.databind.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import rs.ac.uns.ftn.museum.support.CatalogAuthTestBase;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest @AutoConfigureMockMvc(print=MockMvcPrint.NONE) @ActiveProfiles("test")
class ProgramManagementIntegrationTests extends CatalogAuthTestBase {
    @Autowired MockMvc mvc;@Autowired ObjectMapper json;
    private final Instant now=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
    private JsonNode call(MockHttpServletRequestBuilder request,String token,Object body,int expected) throws Exception {
        if(token!=null)request.header("Authorization","Bearer "+token);
        if(body!=null)request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
        String response=mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return response.isBlank()?json.nullNode():json.readTree(response);
    }
    private record Fixture(long museum,long hall,long exhibition,long term) {}
    private Map<String,Object> termBody(long exhibition,int capacity){return Map.of("exhibitionId",exhibition,"start",now.minusSeconds(3600),"end",now.plusSeconds(7200),"capacity",capacity,"status","OTVOREN");}
    private Map<String,Object> exhibitionBody(long hall){return Map.of("hallId",hall,"name","Program test","start",now.minusSeconds(86400),"end",now.plusSeconds(864000),"status","AKTIVNA","onlineVisible",true,"terms",List.of(termBody(1,2)));}
    private Fixture fixture(String token) throws Exception {
        long museum=call(post("/api/museums"),token,Map.of("name","Program museum","address","Adresa","foundationYear",1900,"active",true),201).path("id").asLong();
        long hall=call(post("/api/halls"),token,Map.of("museumId",museum,"name","Sala","area",50,"capacity",20,"floor",1),201).path("id").asLong();
        JsonNode e=call(post("/api/exhibitions"),token,exhibitionBody(hall),201);
        return new Fixture(museum,hall,e.path("id").asLong(),e.path("terms").get(0).path("id").asLong());
    }
    private String prices(Fixture f){return "/api/exhibitions/"+f.exhibition()+"/prices/management";}
    private Map<String,Object> price(String category,Instant from,Instant to,int amount){Map<String,Object> body=new HashMap<>();body.put("category",category);body.put("validFrom",from);body.put("validTo",to);body.put("price",amount);body.put("note","INTERNAL-PRICE-NOTE");return body;}

    @Test void deactivationAndRestrictionPreserveTicketHistoryAndPriceSnapshot() throws Exception {
        String admin=tokenFor("ADMINISTRATOR"),cashier=tokenFor("CASHIER");Fixture f=fixture(admin);
        Instant from=now.minusSeconds(3600);
        call(post(prices(f)),admin,price("ODRASLI",from,null,8),201);
        JsonNode ticket=call(post("/api/cashier/tickets"),cashier,Map.of("termId",f.term(),"category","ODRASLI","paymentMethod","GOTOVINA"),201);
        call(put(prices(f)).param("category","ODRASLI").param("validFrom",from.toString()),admin,Map.of("price",99,"note","Changed"),200);
        assertThat(call(get("/api/tickets/"+ticket.path("id").asLong()),cashier,null,200).path("paidPrice").decimalValue()).isEqualByComparingTo("8");
        call(put(prices(f)).param("category","ODRASLI").param("validFrom",from.toString()),admin,Map.of("price",99,"validTo",now.minusSeconds(1)),409);
        call(put("/api/museums/"+f.museum()),admin,Map.of("name","Program museum","address","Adresa","active",false),200);
        assertThat(call(get("/api/museums/"+f.museum()),null,null,200).path("active").asBoolean()).isFalse();
        for(String path:List.of("museums/"+f.museum(),"halls/"+f.hall(),"exhibitions/"+f.exhibition(),"exhibition-terms/"+f.term()))call(delete("/api/"+path),admin,null,409);
        JsonNode after=call(get("/api/tickets/"+ticket.path("id").asLong()),cashier,null,200);
        for(String field:List.of("id","termId","status","paidPrice","qrToken"))assertThat(after.path(field)).isEqualTo(ticket.path(field));
    }
    @Test void validatesHallAndTermInputsAndProtectsSoldTermDatesAndCapacity() throws Exception {
        String admin=tokenFor("ADMINISTRATOR"),cashier=tokenFor("CASHIER");Fixture f=fixture(admin);
        for(String field:List.of("area","capacity"))for(int bad:List.of(0,-1))call(post("/api/halls"),admin,Map.of("name","Invalid","museumId",f.museum(),field,bad),400);
        call(post("/api/halls"),admin,Map.of("name","Invalid","museumId",Long.MAX_VALUE),404);
        assertThat(call(get("/api/halls/"+f.hall()),null,null,200).path("museumId").asLong()).isEqualTo(f.museum());
        call(post("/api/exhibition-terms"),admin,termBody(f.exhibition(),0),400);
        var badDates=new HashMap<>(termBody(f.exhibition(),2));badDates.put("end",now.minusSeconds(7200));call(post("/api/exhibition-terms"),admin,badDates,400);
        badDates.put("end",now.plusSeconds(999999));call(post("/api/exhibition-terms"),admin,badDates,409);
        call(post(prices(f)),admin,price("ODRASLI",now.minusSeconds(86400),null,5),201);
        for(int i=0;i<2;i++)call(post("/api/cashier/tickets"),cashier,Map.of("termId",f.term(),"category","ODRASLI","paymentMethod","GOTOVINA"),201);
        call(put("/api/exhibition-terms/"+f.term()),admin,termBody(f.exhibition(),1),409);
        var update=new HashMap<>(termBody(f.exhibition(),3));update.put("end",now.plusSeconds(8000));call(put("/api/exhibition-terms/"+f.term()),admin,update,409);
        JsonNode updated=call(put("/api/exhibition-terms/"+f.term()),admin,termBody(f.exhibition(),3),200);
        assertThat(updated.path("status").asText()).isEqualTo("OTVOREN");
    }
    @Test void pricePeriodsUseImmutableCompositeKeyAndHalfOpenNonOverlappingIntervals() throws Exception {
        String admin=tokenFor("ADMINISTRATOR");Fixture f=fixture(admin);Instant from=now.minusSeconds(3600),end=now.plusSeconds(3600);
        call(post(prices(f)),admin,price("ODRASLI",from,end,0),201);
        call(post(prices(f)),admin,price("ODRASLI",end,null,10),201);
        call(post(prices(f)),admin,price("STUDENT",from,null,5),201);
        call(post(prices(f)),admin,price("ODRASLI",now,null,8),409);
        call(post(prices(f)),admin,price("ODRASLI",from,end,8),409);
        call(post(prices(f)),admin,price("OTHER",end,from,8),400);
        call(post(prices(f)),admin,price("OTHER",from,end,-1),400);
        call(put(prices(f)).param("category","ODRASLI").param("validFrom",from.toString()),admin,Map.of("price",2,"validTo",end.plusSeconds(1)),409);
        assertThat(call(get(prices(f)),admin,null,200)).hasSize(3);
        assertThat(call(get("/api/prices").param("exhibitionId",""+f.exhibition()),null,null,200).toString()).doesNotContain("INTERNAL-PRICE-NOTE");
    }
    @Test void concurrentPriceCreationCannotOverlap() throws Exception {
        String admin=tokenFor("ADMINISTRATOR");Fixture f=fixture(admin);CountDownLatch ready=new CountDownLatch(2),go=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)){
            List<Future<Integer>> jobs=new ArrayList<>();
            for(int i=0;i<2;i++){final int shift=i;jobs.add(pool.submit(()->{ready.countDown();go.await();return mvc.perform(post(prices(f)).header("Authorization","Bearer "+admin).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(price("ONE",now.plusSeconds(shift),null,5)))).andReturn().getResponse().getStatus();}));}
            assertThat(ready.await(5,TimeUnit.SECONDS)).isTrue();go.countDown();
            assertThat(List.of(jobs.get(0).get(20,TimeUnit.SECONDS),jobs.get(1).get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(201,409);
        }
    }
    @Test void rolesAndPublicVisibilityFollowExistingPolicy() throws Exception {
        String admin=tokenFor("ADMINISTRATOR"),curator=tokenFor("CURATOR");Fixture f=fixture(admin);
        for(String role:Arrays.asList(null,"VISITOR","CASHIER","GUIDE")){
            String token=role==null?null:tokenFor(role);int denied=role==null?401:403;
            for(String path:List.of("/api/museums","/api/halls","/api/exhibitions","/api/exhibition-terms",prices(f)))call(post(path),token,Map.of(),denied);
            call(get(prices(f)),token,null,denied);
            call(put(prices(f)).param("category","x").param("validFrom",now.toString()),token,Map.of("price",1),denied);
        }
        call(post("/api/museums"),curator,Map.of(),403);call(post("/api/halls"),curator,Map.of(),403);
        call(post(prices(f)),curator,price("CURATOR",now,null,4),201);call(get(prices(f)),curator,null,200);
        var update=new HashMap<>(exhibitionBody(f.hall()));update.put("onlineVisible",false);
        call(put("/api/exhibitions/"+f.exhibition()),curator,update,200);
        call(get("/api/exhibitions/"+f.exhibition()),null,null,404);call(get("/api/exhibition-terms/"+f.term()),null,null,404);
        call(get("/api/exhibitions/"+f.exhibition()),curator,null,200);
    }
}
