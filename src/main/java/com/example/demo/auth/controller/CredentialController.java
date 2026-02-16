package com.example.demo.auth.controller;

import com.example.demo.auth.domain.Authority;
import com.example.demo.auth.domain.Credential;
import com.example.demo.auth.domain.Identity;
import com.example.demo.auth.repository.AuthorityRepository;
import com.example.demo.auth.repository.IdentityRepository;
import com.example.demo.auth.service.CredentialService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/credentials")
public class CredentialController {
    private final CredentialService service;
    private final IdentityRepository identityRepository;
    private final AuthorityRepository authorityRepository;

    public CredentialController(
            CredentialService service,
            IdentityRepository identityRepository,
            AuthorityRepository authorityRepository
    ) {
        this.service = service;
        this.identityRepository = identityRepository;
        this.authorityRepository = authorityRepository;
    }

    @GetMapping
    public List<Credential> list() { return service.list(); }

    @GetMapping("/{id}")
    public ResponseEntity<Credential> get(@PathVariable Long id) {
        return service.get(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CredentialRequest request) {
        if (request == null || request.identityId == null || request.authorityId == null
                || request.value == null || request.value.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Identity identity = identityRepository.findById(request.identityId).orElse(null);
        if (identity == null) {
            return ResponseEntity.notFound().build();
        }
        Authority authority = authorityRepository.findById(request.authorityId).orElse(null);
        if (authority == null) {
            return ResponseEntity.notFound().build();
        }

        Credential credential = new Credential();
        credential.setValue(request.value);
        credential.setIdentity(identity);
        credential.setAuthority(authority);

        try {
            Credential created = service.create(credential);
            return ResponseEntity.created(URI.create("/credentials/" + created.getId())).body(created);
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(409).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody CredentialRequest request) {
        if (request == null || request.identityId == null || request.authorityId == null
                || request.value == null || request.value.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Identity identity = identityRepository.findById(request.identityId).orElse(null);
        if (identity == null) {
            return ResponseEntity.notFound().build();
        }
        Authority authority = authorityRepository.findById(request.authorityId).orElse(null);
        if (authority == null) {
            return ResponseEntity.notFound().build();
        }

        Credential data = new Credential();
        data.setValue(request.value);
        data.setIdentity(identity);
        data.setAuthority(authority);

        try {
            return service.update(id, data).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(409).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private static class CredentialRequest {
        public Long identityId;
        public Long authorityId;
        public String value;
    }
}
