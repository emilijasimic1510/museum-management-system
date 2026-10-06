package rs.ac.uns.ftn.museum.notification;

import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import rs.ac.uns.ftn.museum.journey.domain.Ticket;
import rs.ac.uns.ftn.museum.journey.domain.Visit;
import rs.ac.uns.ftn.museum.people.domain.Account;
import rs.ac.uns.ftn.museum.people.domain.EventRegistration;

/** Snapshots are built inside the transaction; SMTP runs only after a successful commit. */
@Service
public class EmailService {
    private static final Logger log=LoggerFactory.getLogger(EmailService.class);
    private final ObjectProvider<JavaMailSender> senders;
    private final boolean enabled;
    private final String from;

    public EmailService(ObjectProvider<JavaMailSender> senders,
            @Value("${museum.mail.enabled:false}") boolean enabled,
            @Value("${museum.mail.from:museum@localhost}") String from) {
        this.senders=senders;this.enabled=enabled;this.from=from;
    }

    public void sendRegistrationConfirmation(Account account) {
        schedule("registration",()->EmailTemplates.registration(account.getEmail(),language()));
    }
    public void sendTicketPurchaseConfirmation(Ticket ticket) {
        if(ticket.getVisitor()==null)return; // Anonymous cashier sales have no recipient.
        schedule("ticket-purchase",()->EmailTemplates.ticket(ticket.getVisitor().getAccount().getEmail(),
                language(),ticket.getId(),ticket.getTerm().getExhibition().getName(),
                ticket.getTerm().getStart(),ticket.getTerm().getEnd(),ticket.getPrice().getId().getCategory(),
                ticket.getPaidPrice(),ticket.getStatus().name()));
    }
    public void sendEventRegistrationConfirmation(EventRegistration registration) {
        event(registration,false);
    }
    public void sendEventCancellationConfirmation(EventRegistration registration) {
        event(registration,true);
    }
    private void event(EventRegistration r,boolean cancelled) {
        schedule(cancelled?"event-cancellation":"event-registration",()->EmailTemplates.event(
                r.getVisitor().getAccount().getEmail(),language(),r.getEvent().getName(),
                r.getEvent().getStart(),r.getEvent().getEnd(),r.getEvent().getExhibition().getName(),cancelled));
    }
    public void sendVisitCompletionThankYou(Visit visit) {
        if(visit.getTicket().getVisitor()==null)return;
        schedule("visit-completion",()->EmailTemplates.visit(visit.getTicket().getVisitor().getAccount().getEmail(),
                language(),visit.getTicket().getTerm().getExhibition().getName()));
    }

    private String language() {
        // Explicit UI preference only: browser/system locale is not the user's selected UI language.
        var attributes=RequestContextHolder.getRequestAttributes();
        return attributes instanceof ServletRequestAttributes request
                ? request.getRequest().getHeader("X-Museum-Language") : null;
    }
    private void schedule(String event,Supplier<EmailTemplates.Message> factory) {
        if(!enabled)return;
        try {
            if(!TransactionSynchronizationManager.isActualTransactionActive()
                    || !TransactionSynchronizationManager.isSynchronizationActive()) {
                log.warn("Email skipped: event={}, reason=no-active-transaction",event);
                return;
            }
            var message=factory.get(); // Immutable strings only; no lazy entity access after commit.
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { deliver(event,message); }
            });
        } catch(Exception failure) { failed(event,failure); }
    }
    private void deliver(String event,EmailTemplates.Message message) {
        try {
            var sender=senders.getObject();
            var mime=sender.createMimeMessage();
            var helper=new MimeMessageHelper(mime,true,"UTF-8");
            helper.setFrom(from);helper.setTo(message.to());helper.setSubject(message.subject());
            helper.setText(message.text(),message.html());
            sender.send(mime);
        } catch(Exception failure) { failed(event,failure); }
    }
    private void failed(String event,Exception failure) {
        // SMTP exception messages can contain recipients, credentials or message contents.
        log.warn("Email notification failed: event={}, errorType={}",event,failure.getClass().getSimpleName());
    }
}
