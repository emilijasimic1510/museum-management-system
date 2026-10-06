package rs.ac.uns.ftn.museum.people.service;

import java.util.*;
import java.time.*;
import org.springframework.stereotype.Service;
import rs.ac.uns.ftn.museum.notification.EmailService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;
import rs.ac.uns.ftn.museum.people.domain.*;
import rs.ac.uns.ftn.museum.people.dto.*;
import rs.ac.uns.ftn.museum.people.repository.*;
import rs.ac.uns.ftn.museum.people.mapper.PeopleMapper;
import rs.ac.uns.ftn.museum.people.security.*;
import rs.ac.uns.ftn.museum.catalog.exception.*;
import rs.ac.uns.ftn.museum.catalog.repository.ExhibitionRepository;
import org.springframework.security.access.AccessDeniedException;
@Service
@Transactional
public class EventService {
    private final EmailService email;
    private final EventRepository events;
    private final ExhibitionRepository exhibitions;
    private final GuideRepository guides;
    private final VisitorRepository visitors;
    private final EventRegistrationRepository registrations;
    private final Clock clock;
    public EventService(EventRepository events,ExhibitionRepository exhibitions,GuideRepository guides,VisitorRepository visitors,EventRegistrationRepository registrations,Clock clock,EmailService email) {
        this.email=email;this.events=events;this.exhibitions=exhibitions;this.guides=guides;this.visitors=visitors;this.registrations=registrations;this.clock=clock;
    }
    @Transactional(readOnly=true) public List<EventResponse> list() { return events.findAll(Sort.by("id")).stream().map(this::response).toList(); }
    @Transactional(readOnly=true) public EventResponse get(Long id) { return response(require(id)); }
    public EventResponse create(EventRequest r) { Event e=new Event();apply(e,r);canManage(e);return response(events.save(e)); }
    public EventResponse update(Long id,EventRequest r) {
        Event e=lock(id);canManage(e);long count=count(id);
        if(r.capacity()<count)throw new ConflictException("Capacity is below occupied places");
        apply(e,r);canManage(e);refreshCapacityStatus(e);return response(e);
    }
    public void delete(Long id) { Event e=lock(id);canManage(e);events.delete(e);events.flush(); }
    public RegistrationResponse register(Long eventId) {
        Event e=lock(eventId);Visitor v=currentVisitor();
        EventRegistrationId id=new EventRegistrationId(v.getId(),eventId);
        EventRegistration r=registrations.findById(id).orElse(null);
        if (r!=null && r.getStatus()!=RegistrationStatus.OTKAZAN) throw new ConflictException("Already registered");
        ensureRegistrationOpen(e,count(eventId));
        if(r==null) r=new EventRegistration();r.setId(id);r.setVisitor(v);r.setEvent(e);r.setRegisteredAt(clock.instant());r.setStatus(RegistrationStatus.PRIJAVLJEN);
        registrations.saveAndFlush(r);refreshCapacityStatus(e);
        email.sendEventRegistrationConfirmation(r);return PeopleMapper.registration(r);
    }
    public void cancel(Long eventId) {
        Event e=lock(eventId);Visitor v=currentVisitor();
        EventRegistration r=registrations.findById(new EventRegistrationId(v.getId(),eventId)).orElseThrow(() -> new NotFoundException("Registration not found"));
        if(r.getStatus()==RegistrationStatus.PRISUSTVOVAO)throw new ConflictException("Attendance history cannot be cancelled by visitor");
        boolean changed=r.getStatus()!=RegistrationStatus.OTKAZAN;
        r.setStatus(RegistrationStatus.OTKAZAN);registrations.flush();refreshCapacityStatus(e);
        if(changed)email.sendEventCancellationConfirmation(r);
    }
    @Transactional(readOnly=true) public List<ManagedRegistrationResponse> registrations(Long id) {
        Event e=require(id);canManage(e);return registrations.findByEventId(id).stream()
                .map(r -> new ManagedRegistrationResponse(r.getVisitor().getId(),id,r.getRegisteredAt(),
                        r.getStatus(),r.getVisitor().getFirstName(),r.getVisitor().getLastName())).toList();
    }
    public RegistrationResponse updateRegistration(Long eventId,Long visitorId,RegistrationUpdateRequest request) {
        Event e=lock(eventId);canManage(e);
        EventRegistration r=registrations.findById(new EventRegistrationId(visitorId,eventId)).orElseThrow(() -> new NotFoundException("Registration not found"));
        if(r.getStatus()==RegistrationStatus.OTKAZAN && request.status()!=RegistrationStatus.OTKAZAN) ensureRegistrationOpen(e,count(eventId));
        boolean cancelled=r.getStatus()!=RegistrationStatus.OTKAZAN && request.status()==RegistrationStatus.OTKAZAN;
        boolean restored=r.getStatus()==RegistrationStatus.OTKAZAN && request.status()==RegistrationStatus.PRIJAVLJEN;
        r.setStatus(request.status());registrations.flush();refreshCapacityStatus(e);
        if(cancelled)email.sendEventCancellationConfirmation(r);
        if(restored)email.sendEventRegistrationConfirmation(r);
        return PeopleMapper.registration(r);
    }
    public static void ensureOpen(Event e,long occupied) {
        if(e.getStatus()!=EventStatus.PLANIRAN || occupied>=e.getCapacity())throw new ConflictException("Event is closed or full");
    }
    private void ensureRegistrationOpen(Event e,long occupied) {
        ensureOpen(e,occupied);
        if(!e.getStart().isAfter(clock.instant()))throw new ConflictException("Registration closes when the event starts");
    }
    private void refreshCapacityStatus(Event e) {
        if(e.getStatus()==EventStatus.PLANIRAN || e.getStatus()==EventStatus.POPUNJEN) e.setStatus(count(e.getId())>=e.getCapacity()?EventStatus.POPUNJEN:EventStatus.PLANIRAN);
    }
    private void canManage(Event e) {
        if(CurrentUser.hasRole("GUIDE"))return;
        throw new AccessDeniedException("Guide role required to manage events");
    }
    private void apply(Event e,EventRequest r) {
        if(!r.end().isAfter(r.start()))throw new InvalidRequestException("Event end must be after start");
        e.setExhibition(exhibitions.findById(r.exhibitionId()).orElseThrow(() -> new NotFoundException("Exhibition not found")));
        e.setGuide(r.guideId()==null?null:guides.findById(r.guideId()).orElseThrow(() -> new NotFoundException("Guide not found")));
        e.setName(r.name());e.setType(r.type());e.setStart(r.start());e.setEnd(r.end());e.setCapacity(r.capacity());e.setLanguage(r.language());e.setStatus(r.status());
    }
    private long count(Long id) { return id==null?0:registrations.countByEventIdAndStatusNot(id,RegistrationStatus.OTKAZAN); }
    private EventResponse response(Event e) { return PeopleMapper.event(e,count(e.getId())); }
    private Event require(Long id) { return events.findById(id).orElseThrow(() -> new NotFoundException("Event not found")); }
    private Event lock(Long id) { return events.lockById(id).orElseThrow(() -> new NotFoundException("Event not found")); }
    private Visitor currentVisitor() { return visitors.findByAccountId(CurrentUser.id()).orElseThrow(() -> new NotFoundException("Visitor profile not found")); }
}
