package rs.ac.uns.ftn.museum.people.service;

import java.util.*;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;
import rs.ac.uns.ftn.museum.people.domain.*;
import rs.ac.uns.ftn.museum.people.dto.*;
import rs.ac.uns.ftn.museum.people.repository.*;
import rs.ac.uns.ftn.museum.people.mapper.PeopleMapper;
import rs.ac.uns.ftn.museum.people.security.*;
import rs.ac.uns.ftn.museum.catalog.exception.*;

@Service
@Transactional
public class VisitorService {
    private final VisitorRepository visitors;
    private final AccountRepository accounts;
    private final InterestRepository interests;
    private final VisitorInterestRepository links;
    private final EventRegistrationRepository registrations;
    public VisitorService(VisitorRepository visitors, AccountRepository accounts, InterestRepository interests, VisitorInterestRepository links, EventRegistrationRepository registrations) {
        this.visitors=visitors;this.accounts=accounts;this.interests=interests;this.links=links;this.registrations=registrations;
    }
    @Transactional(readOnly=true) public List<VisitorResponse> list() { return visitors.findAll(Sort.by("id")).stream().map(PeopleMapper::visitor).toList(); }
    @Transactional(readOnly=true) public VisitorResponse get(Long id) { return PeopleMapper.visitor(require(id)); }
    @Transactional(readOnly=true) public VisitorResponse me() { return PeopleMapper.visitor(current()); }
    public VisitorResponse create(VisitorRequest r) {
        var account=accounts.findById(r.accountId()).orElseThrow(() -> new NotFoundException("Account not found"));
        if (visitors.findByAccountId(r.accountId()).isPresent()) throw new ConflictException("Account already has visitor profile");
        Visitor v=new Visitor();v.setAccount(account);apply(v,r.firstName(),r.lastName(),r.phone(),r.recommendationsConsent());return PeopleMapper.visitor(visitors.save(v));
    }
    public VisitorResponse update(Long id,VisitorRequest r) {
        Visitor v=require(id);if (!v.getAccount().getId().equals(r.accountId())) throw new ConflictException("Profile account cannot be changed");
        apply(v,r.firstName(),r.lastName(),r.phone(),r.recommendationsConsent());return PeopleMapper.visitor(v);
    }
    public VisitorResponse updateMe(VisitorProfileRequest r) {
        Visitor v=current();apply(v,r.firstName(),r.lastName(),r.phone(),r.recommendationsConsent());return PeopleMapper.visitor(v);
    }
    public void delete(Long id) { visitors.delete(require(id));visitors.flush(); }
    @Transactional(readOnly=true) public List<VisitorInterestResponse> myInterests() {
        return links.findByVisitorId(current().getId()).stream().map(l -> new VisitorInterestResponse(l.getInterest().getId(),l.getInterest().getName(),l.getLevel())).toList();
    }
    public VisitorInterestResponse setInterest(Long interestId,InterestLevelRequest r) {
        Visitor v=current();Interest i=interests.findById(interestId).orElseThrow(() -> new NotFoundException("Interest not found"));
        VisitorInterestId id=new VisitorInterestId(v.getId(),interestId);
        VisitorInterest link=links.findById(id).orElseGet(VisitorInterest::new);link.setId(id);link.setVisitor(v);link.setInterest(i);link.setLevel(r.level());links.save(link);
        return new VisitorInterestResponse(i.getId(),i.getName(),link.getLevel());
    }
    public void removeInterest(Long interestId) {
        var link=links.findById(new VisitorInterestId(current().getId(),interestId)).orElseThrow(() -> new NotFoundException("Interest selection not found"));links.delete(link);
    }
    @Transactional(readOnly=true) public List<RegistrationResponse> myRegistrations() { return registrations.findByVisitorId(current().getId()).stream().map(PeopleMapper::registration).toList(); }
    private Visitor current() { return visitors.findByAccountId(CurrentUser.id()).orElseThrow(() -> new NotFoundException("Visitor profile not found")); }
    private Visitor require(Long id) { return visitors.findById(id).orElseThrow(() -> new NotFoundException("Visitor not found")); }
    private void apply(Visitor v,String first,String last,String phone,Boolean consent) { v.setFirstName(first);v.setLastName(last);v.setPhone(phone);v.setRecommendationsConsent(consent); }
}
