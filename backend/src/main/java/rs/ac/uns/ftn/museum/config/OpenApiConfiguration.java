package rs.ac.uns.ftn.museum.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Set;

@Configuration
@Profile("local")
public class OpenApiConfiguration {
    @Bean
    OpenAPI museumOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Museum API").version("v1")
                        .description("Email je identifikator za prijavu. Prijavite se preko /api/auth/login, "
                                + "zatim unesite dobijeni JWT u Authorize. Pristup zavisi od uloge naloga."))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    OpenApiCustomizer publicLocalOperations() {
        Set<String> publicGets = Set.of("/api/health", "/api/prices", "/api/events", "/api/events/{id}",
                "/api/interests", "/api/interests/{id}");
        return api -> api.getPaths().forEach((path, item) -> {
            if (publicGets.contains(path) && item.getGet() != null) item.getGet().setSecurity(List.of());
            if ((path.equals("/api/auth/login") || path.equals("/api/auth/register")) && item.getPost() != null)
                item.getPost().setSecurity(List.of());
            // These catalog endpoints are already public in the existing local security profile.
            if (path.matches("/api/(museums|halls|exhibitions|exhibition-terms)(/[^/]+)?"))
                item.readOperations().forEach(operation -> operation.setSecurity(List.of()));
        });
    }
}
