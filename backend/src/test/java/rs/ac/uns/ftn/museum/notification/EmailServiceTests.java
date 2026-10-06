package rs.ac.uns.ftn.museum.notification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Properties;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import rs.ac.uns.ftn.museum.people.domain.Account;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailServiceTests {
    @AfterEach void clearTransaction() {
        if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }
    @Test void serbianAndEnglishTemplatesCoverAllEventsAndEscapeHtml() {
        var now=Instant.parse("2026-10-05T10:00:00Z");
        for(String language:new String[]{"sr","en"}) {
            var messages=java.util.List.of(EmailTemplates.registration("user@example.test",language),
                    EmailTemplates.ticket("user@example.test",language,23L,"<script>& Gallery",now,now.plusSeconds(3600),"STUDENT",new BigDecimal("12.50"),"KUPLJENA"),
                    EmailTemplates.event("user@example.test",language,"<script>& Tour",now,now.plusSeconds(3600),"Gallery",false),
                    EmailTemplates.event("user@example.test",language,"Tour",now,now.plusSeconds(3600),"Gallery",true),
                    EmailTemplates.visit("user@example.test",language,"Gallery"));
            assertThat(messages).allSatisfy(m->{
                assertThat(m.subject()).isNotBlank();assertThat(m.text()).isNotBlank();
                assertThat(m.html()).contains("<html>").doesNotContain("<script>");
            });
            assertThat(messages.get(1).text()).contains("12.50","23","STUDENT","KUPLJENA","UTC");
            assertThat(messages.get(1).html()).contains("&lt;script&gt;&amp;");
            assertThat(messages.get(0).text()).contains(language.equals("en")?"Welcome":"Dobro došli");
            assertThat(messages.get(3).text()).contains(language.equals("en")?"cancelled":"otkazana");
        }
    }
    @Test void unknownOrMissingLanguageFallsBackToSerbian() {
        for(String language:new String[]{null,"","de","fr","enough"})
            assertThat(EmailTemplates.registration("user@example.test",language).subject()).contains("Dobro došli");
        assertThat(EmailTemplates.registration("user@example.test","en-GB").subject()).contains("Welcome");
    }
    @Test void disabledModeNeverResolvesTransportOrReadsNotificationData() {
        ObjectProvider<JavaMailSender> provider=mock(ObjectProvider.class);
        new EmailService(provider,false,"museum@example.test").sendRegistrationConfirmation(null);
        verifyNoInteractions(provider);
    }
    @Test void smtpIsDeferredUntilCommitAndFailureCannotEscape() {
        ObjectProvider<JavaMailSender> provider=mock(ObjectProvider.class);
        JavaMailSender sender=mock(JavaMailSender.class);
        when(provider.getObject()).thenReturn(sender);
        when(sender.createMimeMessage()).thenAnswer(i->new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailSendException("simulated SMTP outage")).when(sender).send(any(MimeMessage.class));
        begin();new EmailService(provider,true,"museum@example.test").sendRegistrationConfirmation(account());
        verifyNoInteractions(sender);
        assertThatCode(()->TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit))
                .doesNotThrowAnyException();
        verify(sender).send(any(MimeMessage.class));
    }
    @Test void rollbackNeverSendsNotification() {
        ObjectProvider<JavaMailSender> provider=mock(ObjectProvider.class);
        begin();new EmailService(provider,true,"museum@example.test").sendRegistrationConfirmation(account());
        TransactionSynchronizationManager.getSynchronizations().forEach(s->s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verifyNoInteractions(provider);
    }
    @Test void noTransactionNeverSendsPrematureSuccess() {
        ObjectProvider<JavaMailSender> provider=mock(ObjectProvider.class);
        new EmailService(provider,true,"museum@example.test").sendRegistrationConfirmation(account());
        verifyNoInteractions(provider);
    }
    private void begin() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }
    private Account account() {var account=new Account();account.setEmail("user@example.test");return account;}
}
