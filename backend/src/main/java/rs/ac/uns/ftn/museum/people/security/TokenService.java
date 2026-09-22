package rs.ac.uns.ftn.museum.people.security;

import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import rs.ac.uns.ftn.museum.people.dto.TokenResponse;

@Service
public class TokenService {
    private final JwtEncoder encoder;
    private final Clock clock;
    public TokenService(JwtEncoder encoder, Clock clock) { this.encoder=encoder; this.clock=clock; }
    public TokenResponse issue(Long accountId) {
        Instant now=clock.instant();
        JwtClaimsSet claims=JwtClaimsSet.builder().issuer("museum-backend").subject(accountId.toString()).issuedAt(now).expiresAt(now.plusSeconds(1800)).build();
        String token=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
        return new TokenResponse(token,"Bearer",1800);
    }
}
