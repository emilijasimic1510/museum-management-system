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
public class RoleService {
    private final RoleRepository roles;
    public RoleService(RoleRepository roles) { this.roles=roles; }
    @Transactional(readOnly=true) public List<RoleResponse> list() { return roles.findAll(Sort.by("id")).stream().map(PeopleMapper::role).toList(); }
    @Transactional(readOnly=true) public RoleResponse get(Long id) { return PeopleMapper.role(require(id)); }
    public RoleResponse create(RoleRequest r) {
        if (roles.findByName(r.name()).isPresent()) throw new ConflictException("Role already exists");
        Role role=new Role();role.setName(r.name());role.setDescription(r.description());return PeopleMapper.role(roles.save(role));
    }
    public RoleResponse update(Long id,RoleRequest r) {
        Role role=require(id);if (role.getName()!=r.name()) throw new ConflictException("Role identity cannot be renamed");
        role.setDescription(r.description());return PeopleMapper.role(role);
    }
    public void delete(Long id) { roles.delete(require(id));roles.flush(); }
    private Role require(Long id) { return roles.findById(id).orElseThrow(() -> new NotFoundException("Role not found")); }
}
