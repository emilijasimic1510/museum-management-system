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
public class InterestService {
    private final InterestRepository interests;
    public InterestService(InterestRepository interests) { this.interests=interests; }
    @Transactional(readOnly=true) public List<InterestResponse> list() { return interests.findAll(Sort.by("id")).stream().map(PeopleMapper::interest).toList(); }
    @Transactional(readOnly=true) public InterestResponse get(Long id) { return PeopleMapper.interest(require(id)); }
    public InterestResponse create(InterestRequest r) { Interest i=new Interest();i.setName(r.name());i.setDescription(r.description());return PeopleMapper.interest(interests.save(i)); }
    public InterestResponse update(Long id,InterestRequest r) { Interest i=require(id);i.setName(r.name());i.setDescription(r.description());return PeopleMapper.interest(i); }
    public void delete(Long id) { interests.delete(require(id));interests.flush(); }
    private Interest require(Long id) { return interests.findById(id).orElseThrow(() -> new NotFoundException("Interest not found")); }
}
