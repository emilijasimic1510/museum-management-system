package rs.ac.uns.ftn.museum.people.security;

import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

public final class ApiSecurity {
    private ApiSecurity() {}
    public static SecurityFilterChain configure(HttpSecurity http, JwtDecoder decoder, CurrentAccountJwtConverter converter, boolean local) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(c -> c.disable())
                .authorizeHttpRequests(auth -> {
                    if (local) auth.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll();
                    auth.requestMatchers(HttpMethod.GET,"/api/health").permitAll();
                    auth.requestMatchers(HttpMethod.GET,"/api/media/exhibits/*","/api/media/exhibitions/*").permitAll();
                    if(local) {
                        auth.requestMatchers("/api/local-demo/board").hasAnyRole("CASHIER","CURATOR","GUIDE","ADMINISTRATOR");
                        auth.requestMatchers("/api/local-demo/visits/**").hasRole("VISITOR");
                        auth.requestMatchers("/api/local-demo/enabled").authenticated();
                    }
                    auth.requestMatchers("/api/local-demo/**").denyAll();
                    auth.requestMatchers(HttpMethod.POST,"/api/auth/register","/api/auth/login").permitAll();
                    auth.requestMatchers(HttpMethod.GET,"/api/prices").permitAll();
                    auth.requestMatchers("/api/curator/media/**","/api/curator/participations/*/images","/api/curator/participations/*/images/*").hasAnyRole("CURATOR","ADMINISTRATOR");
                    auth.requestMatchers("/api/curator/**").hasRole("CURATOR");
                    auth.requestMatchers("/api/manager/**").hasRole("MANAGER");
                    auth.requestMatchers("/api/cashier/**").hasRole("CASHIER");
                    auth.requestMatchers("/api/tickets/me").hasRole("VISITOR");
                    auth.requestMatchers(HttpMethod.POST,"/api/tickets").hasRole("VISITOR");
                    auth.requestMatchers("/api/tickets/**").hasAnyRole("VISITOR","CASHIER");
                    auth.requestMatchers("/api/visits/**","/api/recommendations/**").hasRole("VISITOR");
                    auth.requestMatchers(HttpMethod.GET,
                            "/api/museums", "/api/museums/*", "/api/halls", "/api/halls/*",
                            "/api/exhibitions", "/api/exhibitions/*", "/api/exhibition-terms", "/api/exhibition-terms/*",
                            "/api/exhibits", "/api/exhibits/*", "/api/authors", "/api/authors/*",
                            "/api/exhibits/*/authorships", "/api/exhibits/*/participations",
                            "/api/exhibitions/*/participations").permitAll();
                    auth.requestMatchers("/api/exhibits/**", "/api/authors/**", "/api/exhibit-participations/**",
                            "/api/digital-content/**").hasAnyRole("CURATOR", "ADMINISTRATOR");
                    auth.requestMatchers("/api/admin/**","/api/employees/**").hasRole("ADMINISTRATOR");
                    auth.requestMatchers("/api/accounts/me").authenticated();
                    auth.requestMatchers(HttpMethod.GET,"/api/guides/me").hasRole("GUIDE");
                    auth.requestMatchers("/api/guides/**").denyAll();
                    auth.requestMatchers("/api/visitors/me","/api/visitors/me/**").hasRole("VISITOR");
                    auth.requestMatchers("/api/visitors/**").hasRole("ADMINISTRATOR");
                    auth.requestMatchers(HttpMethod.POST,"/api/events/*/registrations").hasRole("VISITOR");
                    auth.requestMatchers(HttpMethod.DELETE,"/api/events/*/registrations/me").hasRole("VISITOR");
                    auth.requestMatchers("/api/events/*/registrations","/api/events/*/registrations/*").hasRole("GUIDE");
                    auth.requestMatchers(HttpMethod.GET,"/api/events","/api/events/*","/api/interests","/api/interests/*").permitAll();
                    auth.requestMatchers("/api/events/**").hasRole("GUIDE");
                    auth.requestMatchers("/api/interests/**").hasAnyRole("CURATOR","ADMINISTRATOR");
                    auth.requestMatchers("/api/exhibitions/**","/api/exhibition-terms/**").hasAnyRole("CURATOR","ADMINISTRATOR");
                    auth.requestMatchers("/api/museums/**","/api/halls/**").hasRole("ADMINISTRATOR");
                    auth.anyRequest().authenticated();
                })
                .oauth2ResourceServer(o -> o.jwt(j -> j.decoder(decoder).jwtAuthenticationConverter(converter)))
                .exceptionHandling(e -> e.authenticationEntryPoint((req,res,ex) -> res.sendError(401))
                        .accessDeniedHandler((req,res,ex) -> res.sendError(403)))
                .build();
    }
}
