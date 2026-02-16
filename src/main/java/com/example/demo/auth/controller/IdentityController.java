package com.example.demo.auth.controller;

import com.example.demo.auth.domain.Identity;
import com.example.demo.auth.service.IdentityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/identities")
public class IdentityController {
    private final IdentityService service;

    public IdentityController(IdentityService service) {
        this.service = service;
    }

    @GetMapping
    public List<Identity> list() { return service.list(); }

    @GetMapping("/{id}")
    public ResponseEntity<Identity> get(@PathVariable Long id) {
        return service.get(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Identity> create(@RequestBody Identity identity) {
        if (identity == null || identity.getEmail() == null || identity.getEmail().isBlank() || identity.getName() == null || identity.getName().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        // prevent duplicate email -> 409
        if (service.getByEmail(identity.getEmail()).isPresent()) {
            return ResponseEntity.status(409).build();
        }
        Identity created = service.create(identity);
        return ResponseEntity.created(URI.create("/identities/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Identity> update(@PathVariable Long id, @RequestBody Identity identity) {
        if (identity == null || identity.getEmail() == null || identity.getEmail().isBlank() || identity.getName() == null || identity.getName().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return service.update(id, identity).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
