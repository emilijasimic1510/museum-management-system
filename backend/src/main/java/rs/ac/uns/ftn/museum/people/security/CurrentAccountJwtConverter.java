package rs.ac.uns.ftn.museum.people.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import rs.ac.uns.ftn.museum.people.domain.Account;
import rs.ac.uns.ftn.museum.people.repository.*;

@Component
public class CurrentAccountJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final AccountRepository accounts;
    private final AccountRoleRepository roles;
    public CurrentAccountJwtConverter(AccountRepository accounts, AccountRoleRepository roles) { this.accounts=accounts; this.roles=roles; }
    @Override @Transactional(readOnly=true)
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Long id;
        try { id=Long.valueOf(jwt.getSubject()); } catch (RuntimeException ex) { throw new BadCredentialsException("Invalid token"); }
        Account account=accounts.findById(id).orElseThrow(() -> new BadCredentialsException("Invalid token"));
        if (!Boolean.TRUE.equals(account.getActive())) throw new BadCredentialsException("Invalid token");
        var authorities=roles.roleNames(id).stream().map(r -> new SimpleGrantedAuthority("ROLE_"+r.name())).toList();
        return new JwtAuthenticationToken(jwt,authorities,id.toString());
    }
}
