package com.example.demo.auth.controller;

import com.example.demo.auth.domain.Authority;
import com.example.demo.auth.domain.AuthMethod;
import com.example.demo.auth.service.AuthorityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/authorities")
public class AuthorityController {
    private final AuthorityService service;

    public AuthorityController(AuthorityService service) {
        this.service = service;
    }

    @GetMapping
    public List<Authority> list() { return service.list(); }

    @GetMapping("/{id}")
    public ResponseEntity<Authority> get(@PathVariable Long id) {
        return service.get(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-method/{method}")
    public ResponseEntity<Authority> getByMethod(@PathVariable AuthMethod method) {
        return service.getByMethod(method).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Authority> create(@RequestBody Authority authority) {
        Authority created = service.create(authority);
        return ResponseEntity.created(URI.create("/authorities/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Authority> update(@PathVariable Long id, @RequestBody Authority authority) {
        return service.update(id, authority).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
