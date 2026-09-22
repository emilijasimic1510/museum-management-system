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
import org.springframework.jdbc.core.JdbcTemplate;
import rs.ac.uns.ftn.museum.catalog.repository.MuseumRepository;
@Service
@Transactional
public class EmployeeService {
    private final EmployeeRepository employees;
    private final AccountRepository accounts;
    private final MuseumRepository museums;
    private final JdbcTemplate jdbc;
    public EmployeeService(EmployeeRepository employees,AccountRepository accounts,MuseumRepository museums,JdbcTemplate jdbc) { this.employees=employees;this.accounts=accounts;this.museums=museums;this.jdbc=jdbc; }
    @Transactional(readOnly=true) public List<EmployeeResponse> list() { return employees.findAll(Sort.by("id")).stream().map(PeopleMapper::employee).toList(); }
    @Transactional(readOnly=true) public EmployeeResponse get(Long id) { return PeopleMapper.employee(require(id)); }
    public EmployeeResponse create(EmployeeRequest r) {
        hierarchyLock();Employee e=switch(r.type()) { case CURATOR -> new Curator();case GUIDE -> new Guide();case CASHIER -> new Cashier();case MANAGER -> new Manager();case ADMINISTRATOR -> new Administrator(); };
        e.setAccount(accounts.findById(r.accountId()).orElseThrow(() -> new NotFoundException("Account not found")));
        if (employees.findByAccountId(r.accountId()).isPresent()) throw new ConflictException("Account already has employee profile");
        apply(e,r);return PeopleMapper.employee(employees.save(e));
    }
    public EmployeeResponse update(Long id,EmployeeRequest r) {
        hierarchyLock();Employee e=require(id);
        if (e.getType()!=r.type() || !e.getAccount().getId().equals(r.accountId())) throw new ConflictException("Employee subtype and account are immutable");
        apply(e,r);return PeopleMapper.employee(e);
    }
    public void delete(Long id) { hierarchyLock();employees.delete(require(id));employees.flush(); }
    private void hierarchyLock() { jdbc.execute("select pg_advisory_xact_lock(920022)"); }
    private void apply(Employee e,EmployeeRequest r) {
        if ((r.type()!=EmployeeType.CURATOR && r.specialization()!=null) || (r.type()!=EmployeeType.GUIDE && r.primaryLanguage()!=null)
                || (r.type()!=EmployeeType.CASHIER && r.counterLabel()!=null) || (r.type()!=EmployeeType.MANAGER && r.reportingLevel()!=null)
                || (r.type()!=EmployeeType.ADMINISTRATOR && r.administrationLevel()!=null)) throw new InvalidRequestException("Only selected subtype attributes may be supplied");
        e.setMuseum(museums.findById(r.museumId()).orElseThrow(() -> new NotFoundException("Museum not found")));
        Employee supervisor=r.supervisorId()==null?null:require(r.supervisorId());
        Set<Long> seen=new HashSet<>();Employee cursor=supervisor;
        while(cursor!=null) {
            if (Objects.equals(cursor.getId(),e.getId()) || !seen.add(cursor.getId())) throw new ConflictException("Supervisor hierarchy cannot contain cycles");
            cursor=cursor.getSupervisor();
        }
        e.setSupervisor(supervisor);e.setFirstName(r.firstName());e.setLastName(r.lastName());e.setPhone(r.phone());e.setHiredOn(r.hiredOn());
        if(e instanceof Curator c)c.setSpecialization(r.specialization());if(e instanceof Guide g)g.setPrimaryLanguage(r.primaryLanguage());
        if(e instanceof Cashier c)c.setCounterLabel(r.counterLabel());if(e instanceof Manager m)m.setReportingLevel(r.reportingLevel());
        if(e instanceof Administrator a)a.setAdministrationLevel(r.administrationLevel());
    }
    private Employee require(Long id) { return employees.findById(id).orElseThrow(() -> new NotFoundException("Employee not found")); }
}
