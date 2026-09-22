package rs.ac.uns.ftn.museum.config;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import rs.ac.uns.ftn.museum.people.security.*;

@Configuration
@Profile("!local")
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder decoder, CurrentAccountJwtConverter converter) throws Exception {
        return ApiSecurity.configure(http,decoder,converter,false);
    }
}
