package rs.ac.uns.ftn.museum.journey.service;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import java.security.SecureRandom;
import org.springframework.stereotype.Service;
import rs.ac.uns.ftn.museum.notification.EmailService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import rs.ac.uns.ftn.museum.catalog.domain.*;
import rs.ac.uns.ftn.museum.catalog.exception.*;
import rs.ac.uns.ftn.museum.people.domain.Visitor;
import rs.ac.uns.ftn.museum.people.repository.VisitorRepository;
import rs.ac.uns.ftn.museum.people.security.CurrentUser;
import rs.ac.uns.ftn.museum.journey.domain.*;
import rs.ac.uns.ftn.museum.journey.repository.*;
import rs.ac.uns.ftn.museum.journey.mapper.JourneyMapper;
import static rs.ac.uns.ftn.museum.journey.dto.JourneyDtos.*;

@Service
@Transactional
public class SalesService {
    private final EmailService email;
    private final EntityManager em;
    private final TicketRepository tickets;
    private final PaymentRepository payments;
    private final VisitRepository visits;
    private final VisitorRepository visitors;
    private final DemoPaymentGateway gateway;
    private final Clock clock;
    private static final SecureRandom RANDOM = new SecureRandom();
    public SalesService(EntityManager em, TicketRepository tickets, PaymentRepository payments, VisitRepository visits,
            VisitorRepository visitors, DemoPaymentGateway gateway, Clock clock, EmailService email) {
        this.email=email;this.em=em;this.tickets=tickets;this.payments=payments;this.visits=visits;this.visitors=visitors;this.gateway=gateway;this.clock=clock;
    }
    public TicketResponse purchase(PurchaseRequest request, boolean cashier) {
        Visitor visitor;
        if (cashier) visitor=request.visitorId()==null?null:visitors.findById(request.visitorId()).orElseThrow(()->new NotFoundException("Visitor not found"));
        else {
            visitor=visitors.findByAccountId(CurrentUser.id()).orElseThrow(()->new NotFoundException("Visitor profile not found"));
            if(request.visitorId()!=null && !visitor.getId().equals(request.visitorId())) throw new AccessDeniedException("Cannot buy for another profile");
            if(request.paymentMethod()!=PaymentMethod.KARTICA) throw new InvalidRequestException("Online purchase requires card payment");
        }
        ExhibitionTerm term=lockTerm(request.termId());
        ensureSale(term);
        Ticket ticket=new Ticket();ticket.setVisitor(visitor);ticket.setTerm(term);
        ticket.setStatus(TicketStatus.OTKAZANA);ticket.setQrToken(token());
        snapshot(ticket,request.category());
        tickets.saveAndFlush(ticket);
        attempt(ticket,request.paymentMethod(),request.demoDecline());
        return JourneyMapper.ticket(ticket);
    }
    public TicketResponse retry(Long id, RetryPaymentRequest request) {
        Ticket ticket=lockTicket(id); ownerOrCashier(ticket);
        if(ticket.getStatus()!=TicketStatus.OTKAZANA || visits.existsByTicketId(id)) throw new ConflictException("Only an unused cancelled ticket may be paid again");
        if(!CurrentUser.hasRole("CASHIER") && request.paymentMethod()!=PaymentMethod.KARTICA) throw new InvalidRequestException("Online purchase requires card payment");
        ensureSale(ticket.getTerm());snapshot(ticket,ticket.getPrice().getId().getCategory());
        attempt(ticket,request.paymentMethod(),request.demoDecline());
        return JourneyMapper.ticket(ticket);
    }
    public TicketResponse cancel(Long id) {
        Ticket ticket=lockTicket(id);ownerOrCashier(ticket);
        if(ticket.getStatus()==TicketStatus.OTKAZANA) return JourneyMapper.ticket(ticket);
        if(ticket.getStatus()!=TicketStatus.KUPLJENA || visits.existsByTicketId(id)
                || !clock.instant().isBefore(ticket.getValidUntil()) || !clock.instant().isBefore(ticket.getTerm().getEnd()))
            throw new ConflictException("Used or expired tickets cannot be cancelled");
        gateway.refund();
        for(Payment payment:payments.findByTicketIdOrderById(id))
            if(payment.getStatus()==PaymentStatus.USPESNA) payment.setStatus(PaymentStatus.REFUNDIRANA);
        ticket.setStatus(TicketStatus.OTKAZANA);
        em.flush();refreshTerm(ticket.getTerm());
        return JourneyMapper.ticket(ticket);
    }
    public VisitResponse validate(ValidateTicketRequest request) {
        Ticket found=tickets.findByQrToken(request.qrToken()).orElseThrow(()->new NotFoundException("Ticket not found"));
        Ticket ticket=lockTicket(found.getId());
        Instant now=clock.instant();
        if(!ticket.getTerm().getId().equals(request.termId())) throw new ConflictException("Ticket is for a different term");
        ExhibitionTerm term=ticket.getTerm();
        if(ticket.getStatus()!=TicketStatus.KUPLJENA || visits.existsByTicketId(ticket.getId())
                || now.isBefore(term.getStart()) || !now.isBefore(term.getEnd()) || !now.isBefore(ticket.getValidUntil())
                || term.getStatus()==TermStatus.OTKAZAN || term.getStatus()==TermStatus.ZAVRSEN
                || term.getExhibition().getStatus()==ExhibitionStatus.OTKAZANA
                || !payments.existsByTicketIdAndStatus(ticket.getId(),PaymentStatus.USPESNA))
            throw new ConflictException("Ticket is not valid for entry");
        Visit visit=new Visit();visit.setTicket(ticket);visit.setEnteredAt(now);visit.setStatus(VisitStatus.AKTIVNA);
        visits.saveAndFlush(visit);
        ticket.setStatus(TicketStatus.AKTIVNA);
        return JourneyMapper.visit(visit);
    }
    @Transactional(readOnly=true) public List<TicketResponse> mine() {
        return tickets.findByVisitorAccountIdOrderByIdDesc(CurrentUser.id()).stream().map(JourneyMapper::ticket).toList();
    }
    @Transactional(readOnly=true) public TicketResponse get(Long id) { Ticket ticket=require(id);ownerOrCashier(ticket);return JourneyMapper.ticket(ticket); }
    @Transactional(readOnly=true) public List<PaymentResponse> payments(Long id) {
        ownerOrCashier(require(id));return payments.findByTicketIdOrderById(id).stream().map(JourneyMapper::payment).toList();
    }
    @Transactional(readOnly=true) public List<PriceResponse> prices(Long exhibitionId) {
        if(em.find(Exhibition.class,exhibitionId)==null) throw new NotFoundException("Exhibition not found");
        Instant now=clock.instant();
        return em.createQuery("select p from ExhibitionPrice p where p.exhibition.id=:id and p.id.validFrom<=:now and (p.validTo is null or p.validTo>:now) order by p.id.validFrom desc",ExhibitionPrice.class)
                .setParameter("id",exhibitionId).setParameter("now",now).getResultList().stream()
                .map(p->new PriceResponse(exhibitionId,p.getId().getValidFrom(),p.getValidTo(),p.getId().getCategory(),p.getPrice())).toList();
    }
    private void snapshot(Ticket ticket,String category) {
        Instant now=clock.instant();
        List<ExhibitionPrice> prices=em.createQuery("select p from ExhibitionPrice p where p.exhibition.id=:id and p.id.category=:category and p.id.validFrom<=:now and (p.validTo is null or p.validTo>:now) order by p.id.validFrom desc",ExhibitionPrice.class)
                .setParameter("id",ticket.getTerm().getExhibition().getId()).setParameter("category",category).setParameter("now",now)
                .setMaxResults(1).setLockMode(LockModeType.PESSIMISTIC_READ).getResultList();
        if(prices.isEmpty()) throw new ConflictException("No price is valid for this category at purchase time");
        ticket.setPrice(prices.getFirst());ticket.setPaidPrice(prices.getFirst().getPrice());
        ticket.setPurchasedAt(now);ticket.setValidUntil(ticket.getTerm().getEnd());
    }
    private void attempt(Ticket ticket,PaymentMethod method,boolean decline) {
        Payment payment=new Payment();payment.setTicket(ticket);payment.setAmount(ticket.getPaidPrice());
        payment.setCreatedAt(clock.instant());payment.setMethod(method);payment.setStatus(PaymentStatus.POKRENUTA);
        payments.saveAndFlush(payment);
        var result=gateway.charge(decline);payment.setStatus(result.status());payment.setExternalReference(result.reference());
        ticket.setStatus(result.status()==PaymentStatus.USPESNA?TicketStatus.KUPLJENA:TicketStatus.OTKAZANA);
        em.flush();refreshTerm(ticket.getTerm());
        if(ticket.getStatus()==TicketStatus.KUPLJENA)email.sendTicketPurchaseConfirmation(ticket);
    }
    private void ensureSale(ExhibitionTerm term) {
        if(term.getStatus()!=TermStatus.OTVOREN || !clock.instant().isBefore(term.getEnd())
                || term.getExhibition().getStatus()==ExhibitionStatus.OTKAZANA
                || term.getExhibition().getStatus()==ExhibitionStatus.ZAVRSENA
                || tickets.countByTermIdAndStatusNot(term.getId(),TicketStatus.OTKAZANA)>=term.getCapacity())
            throw new ConflictException("Term is closed, expired or full");
    }
    private void refreshTerm(ExhibitionTerm term) {
        if(term.getStatus()==TermStatus.OTVOREN || term.getStatus()==TermStatus.POPUNJEN)
            term.setStatus(tickets.countByTermIdAndStatusNot(term.getId(),TicketStatus.OTKAZANA)>=term.getCapacity()?TermStatus.POPUNJEN:TermStatus.OTVOREN);
    }
    private ExhibitionTerm lockTerm(Long id) {
        ExhibitionTerm term=em.find(ExhibitionTerm.class,id,LockModeType.PESSIMISTIC_WRITE);
        if(term==null) throw new NotFoundException("Term not found");
        em.refresh(term);return term;
    }
    private Ticket lockTicket(Long id) {
        Ticket ticket=require(id);lockTerm(ticket.getTerm().getId());em.refresh(ticket,LockModeType.PESSIMISTIC_WRITE);return ticket;
    }
    private Ticket require(Long id) { return tickets.findById(id).orElseThrow(()->new NotFoundException("Ticket not found")); }
    private void ownerOrCashier(Ticket ticket) {
        if(CurrentUser.hasRole("CASHIER"))return;
        if(ticket.getVisitor()==null || !ticket.getVisitor().getAccount().getId().equals(CurrentUser.id())) throw new AccessDeniedException("Ticket belongs to another visitor");
    }
    private static String token() { byte[] bytes=new byte[32];RANDOM.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
}
