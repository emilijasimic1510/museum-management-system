package rs.ac.uns.ftn.museum.journey.service;

import java.util.UUID;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import rs.ac.uns.ftn.museum.journey.domain.PaymentStatus;

@Component
public class DemoPaymentGateway {
    private final Environment environment;
    public DemoPaymentGateway(Environment environment) { this.environment=environment; }
    public record Result(PaymentStatus status, String reference) {}
    public Result charge(boolean decline) {
        requireDemo();
        return new Result(decline ? PaymentStatus.NEUSPESNA : PaymentStatus.USPESNA, "DEMO-"+UUID.randomUUID());
    }
    public void refund() { requireDemo(); }
    private void requireDemo() {
        if (!environment.matchesProfiles("local","test"))
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"A production payment adapter has not been configured");
    }
}
