package rs.ac.uns.ftn.museum.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@Profile("local")
public class LocalWebConfig {
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${museum.web.allowed-origins:http://localhost:4200,http://127.0.0.1:4200}") String origins) {
        CorsConfiguration config=new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(s->!s.isBlank()).toList());
        config.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("Authorization","Content-Type","X-Museum-Language"));
        config.setExposedHeaders(Arrays.asList("Content-Type"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource();source.registerCorsConfiguration("/api/**",config);return source;
    }
}
