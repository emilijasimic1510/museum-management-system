package rs.ac.uns.ftn.museum;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import rs.ac.uns.ftn.museum.support.PostgresTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "museum.automation.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles({"test", "local"})
class OpenApiIntegrationTests extends PostgresTestBase {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mappings;

    @Test
    void swaggerAndSpecArePublicAndDescribeEveryApiOperation() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs/swagger-config")).andExpect(status().isOk());
        JsonNode spec = mapper.readTree(mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        mappings.getHandlerMethods().forEach((mapping, handler) -> mapping.getPatternValues().stream()
                .filter(path -> path.startsWith("/api/"))
                .forEach(path -> mapping.getMethodsCondition().getMethods().forEach(method -> {
                    // Spring MVC keeps regex constraints in its mapping, while SpringDoc
                    // emits the OpenAPI placeholder without the constraint.
                    String documentedPath = path.replaceAll("\\{([^}:]+):[^}]+}", "{$1}");
                    assertThat(spec.path("paths").path(documentedPath).has(method.name().toLowerCase()))
                            .as("%s %s is documented", method, documentedPath).isTrue();
                })));
        assertThat(spec.at("/components/securitySchemes/bearerAuth/scheme").asText()).isEqualTo("bearer");
        assertThat(spec.path("paths").path("/api/auth/login").path("post").path("security").isEmpty()).isTrue();
        assertThat(spec.path("paths").path("/api/exhibits").path("get").path("security").isEmpty()).isTrue();
        // An absent operation override inherits the global bearer requirement.
        assertThat(spec.path("paths").path("/api/exhibits").path("post").has("security")).isFalse();
        assertThat(spec.path("paths").path("/api/museums").path("post").has("security")).isFalse();
    }

    @Test
    void swaggerDoesNotRemoveBusinessAuthentication() throws Exception {
        for (String path : new String[]{"/api/accounts/me", "/api/tickets/me", "/api/visits/me", "/api/admin/accounts"})
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
    }
}
