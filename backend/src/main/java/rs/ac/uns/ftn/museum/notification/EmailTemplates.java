package rs.ac.uns.ftn.museum.notification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import org.springframework.web.util.HtmlUtils;

/** Small backend-owned templates. Dates explicitly use UTC; no invented currency. */
final class EmailTemplates {
    private EmailTemplates() {}
    record Message(String to,String subject,String text,String html) {}
    static boolean english(String language) {
        if(language==null)return false;
        String value=language.strip().toLowerCase(Locale.ROOT);
        return value.equals("en") || value.startsWith("en-");
    }
    static Message registration(String to,String language) {
        boolean en=english(language);
        return message(to,en?"Welcome — registration confirmed":"Dobro došli — potvrda registracije",
                en?"Welcome to the museum! Your account has been created. You can now sign in."
                  :"Dobro došli u muzej! Vaš nalog je uspešno kreiran. Sada se možete prijaviti.");
    }
    static Message ticket(String to,String language,Long id,String exhibition,Instant start,Instant end,
            String category,BigDecimal price,String status) {
        boolean en=english(language);
        return message(to,en?"Ticket purchase confirmed":"Potvrda kupovine ulaznice",
                (en?"Exhibition: ":"Izložba: ")+exhibition+"\n"+
                (en?"Term (UTC): ":"Termin (UTC): ")+start+" — "+end+"\n"+
                (en?"Category: ":"Kategorija: ")+category+"\n"+
                (en?"Paid price: ":"Plaćena cena: ")+price.toPlainString()+"\n"+
                (en?"Status: Purchased":"Status: Kupljena")+" ("+status+")\n"+
                (en?"Ticket identifier: ":"Identifikator ulaznice: ")+id+"\n"+
                (en?"View your ticket and QR code in the Tickets section after signing in."
                   :"Ulaznicu i QR kod možete pogledati u odeljku Ulaznice nakon prijave."));
    }
    static Message event(String to,String language,String name,Instant start,Instant end,String exhibition,boolean cancelled) {
        boolean en=english(language);
        return message(to,cancelled?(en?"Event cancellation confirmed":"Potvrda otkazivanja prijave")
                    :(en?"Event registration confirmed":"Potvrda prijave na događaj"),
                (en?"Event: ":"Događaj: ")+name+"\n"+
                (en?"Time (UTC): ":"Vreme (UTC): ")+start+" — "+end+"\n"+
                (en?"Exhibition: ":"Izložba: ")+exhibition+"\n"+
                (cancelled?(en?"Your registration has been cancelled.":"Vaša prijava je otkazana.")
                          :(en?"Registration status: Registered.":"Status prijave: Prijavljen.")));
    }
    static Message visit(String to,String language,String exhibition) {
        boolean en=english(language);
        return message(to,en?"Thank you for visiting":"Hvala na poseti",
                (en?"Thank you for visiting the exhibition: ":"Hvala što ste posetili izložbu: ")+exhibition+".\n"+
                (en?"Your visit is complete. You can rate the exhibition from your visit details in the Visits section."
                   :"Vaša poseta je završena. Izložbu možete oceniti na detalju posete u odeljku Posete."));
    }
    private static Message message(String to,String subject,String text) {
        return new Message(to,subject,text,"<!doctype html><html><body><h1>"+HtmlUtils.htmlEscape(subject)+
                "</h1><p>"+HtmlUtils.htmlEscape(text).replace("\n","<br>")+"</p></body></html>");
    }
}
