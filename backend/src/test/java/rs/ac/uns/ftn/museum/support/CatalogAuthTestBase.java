package rs.ac.uns.ftn.museum.support;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import rs.ac.uns.ftn.museum.people.security.TokenService;

/** Real signed JWT and database roles, no mock security bypass. */
public abstract class CatalogAuthTestBase extends PostgresTestBase {
    @Autowired private JdbcTemplate authJdbc;
    @Autowired private PasswordEncoder authPasswords;
    @Autowired private TokenService tokens;
    @Autowired private PlatformTransactionManager authTransactions;

    protected String tokenFor(String role) {
        long id=new TransactionTemplate(authTransactions).execute(status -> {
            long account=authJdbc.queryForObject("insert into korisnicki_nalog(email,lozinka_hash,aktivan,datum_registracije) values (?,?,true,now()) returning id",
                    Long.class, UUID.randomUUID()+"@example.test", authPasswords.encode(UUID.randomUUID().toString()));
            authJdbc.update("insert into nalog_uloga(nalog_id,uloga_id,datum_dodele) select ?,id,now() from uloga where naziv=?",account,role);
            return account;
        });
        return tokens.issue(id).accessToken();
    }
}
