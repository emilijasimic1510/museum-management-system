package rs.ac.uns.ftn.museum.people.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.museum.people.dto.*;
import rs.ac.uns.ftn.museum.people.service.*;

@RestController
@RequestMapping("/api/admin/roles")
public class RoleController {
    private final RoleService service;
    public RoleController(RoleService service) { this.service = service; }
    @GetMapping public List<RoleResponse> list() { return service.list(); }
    @GetMapping("/{id}") public RoleResponse get(@PathVariable Long id) { return service.get(id); }
    @PostMapping public ResponseEntity<RoleResponse> create(@Valid @RequestBody RoleRequest request) {
        var result = service.create(request);
        return ResponseEntity.created(URI.create("/api/admin/roles/" + result.id())).body(result);
    }
    @PutMapping("/{id}") public RoleResponse update(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        return service.update(id, request);
    }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
