package rs.ac.uns.ftn.museum.people.mapper;

import rs.ac.uns.ftn.museum.people.domain.*;
import rs.ac.uns.ftn.museum.people.dto.*;

public final class PeopleMapper {
    private PeopleMapper() {}
    public static VisitorResponse visitor(Visitor v) { return new VisitorResponse(v.getId(),v.getAccount().getId(),v.getFirstName(),v.getLastName(),v.getPhone(),v.getRecommendationsConsent()); }
    public static RoleResponse role(Role r) { return new RoleResponse(r.getId(),r.getName(),r.getDescription()); }
    public static InterestResponse interest(Interest i) { return new InterestResponse(i.getId(),i.getName(),i.getDescription()); }
    public static EmployeeResponse employee(Employee e) {
        return new EmployeeResponse(e.getId(),e.getAccount().getId(),e.getMuseum().getId(),e.getFirstName(),e.getLastName(),e.getPhone(),e.getHiredOn(),e.getSupervisor()==null?null:e.getSupervisor().getId(),e.getType(),
                e instanceof Curator c?c.getSpecialization():null, e instanceof Guide g?g.getPrimaryLanguage():null,
                e instanceof Cashier c?c.getCounterLabel():null, e instanceof Manager m?m.getReportingLevel():null,
                e instanceof Administrator a?a.getAdministrationLevel():null);
    }
    public static EventResponse event(Event e,long count) { return new EventResponse(e.getId(),e.getExhibition().getId(),e.getGuide()==null?null:e.getGuide().getId(),e.getName(),e.getType(),e.getStart(),e.getEnd(),e.getCapacity(),e.getLanguage(),e.getStatus(),count); }
    public static RegistrationResponse registration(EventRegistration r) { return new RegistrationResponse(r.getVisitor().getId(),r.getEvent().getId(),r.getRegisteredAt(),r.getStatus()); }
}
