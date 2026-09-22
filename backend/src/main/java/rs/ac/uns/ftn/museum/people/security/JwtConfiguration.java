package rs.ac.uns.ftn.museum.people.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

@Configuration
public class JwtConfiguration {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean Clock clock() { return Clock.systemUTC(); }
    @Bean SecretKey jwtKey(Environment environment) {
        String value=environment.getProperty("JWT_SECRET");
        if (value==null || value.getBytes(StandardCharsets.UTF_8).length<32) {
            throw new IllegalStateException("JWT_SECRET must be supplied and contain at least 32 UTF-8 bytes");
        }
        return new SecretKeySpec(value.getBytes(StandardCharsets.UTF_8),"HmacSHA256");
    }
    @Bean JwtEncoder jwtEncoder(SecretKey key) { return new NimbusJwtEncoder(new ImmutableSecret<>(key)); }
    @Bean JwtDecoder jwtDecoder(SecretKey key) {
        NimbusJwtDecoder decoder=NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer("museum-backend"),
                jwt -> jwt.getExpiresAt()==null || jwt.getSubject()==null
                    ? OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token")) : OAuth2TokenValidatorResult.success()));
        return decoder;
    }
}
