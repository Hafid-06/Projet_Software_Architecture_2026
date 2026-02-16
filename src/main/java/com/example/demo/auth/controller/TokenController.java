package com.example.demo.auth.controller;

import com.example.demo.auth.domain.Authority;
import com.example.demo.auth.domain.Identity;
import com.example.demo.auth.domain.Token;
import com.example.demo.auth.repository.AuthorityRepository;
import com.example.demo.auth.repository.IdentityRepository;
import com.example.demo.auth.service.TokenService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tokens")
public class TokenController {
    private final TokenService service;
    private final IdentityRepository identityRepository;
    private final AuthorityRepository authorityRepository;

    public TokenController(
            TokenService service,
            IdentityRepository identityRepository,
            AuthorityRepository authorityRepository
    ) {
        this.service = service;
        this.identityRepository = identityRepository;
        this.authorityRepository = authorityRepository;
    }

    @GetMapping
    public List<Token> list() { return service.list(); }

    @GetMapping("/{id}")
    public ResponseEntity<Token> get(@PathVariable Long id) {
        return service.get(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam String value) {
        if (value == null || value.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        boolean valid = service.isValid(value);
        return ResponseEntity.ok(Map.of("valid", valid));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody TokenRequest request) {
        if (request == null || request.identityId == null || request.authorityId == null
                || request.value == null || request.value.isBlank() || request.expiresAt == null) {
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

        Token token = new Token();
        token.setValue(request.value);
        token.setIdentity(identity);
        token.setAuthority(authority);
        token.setIssuedAt(request.issuedAt != null ? request.issuedAt : Instant.now());
        token.setExpiresAt(request.expiresAt);
        token.setRevoked(request.revoked != null && request.revoked);

        try {
            Token created = service.create(token);
            return ResponseEntity.created(URI.create("/tokens/" + created.getId())).body(created);
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(409).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody TokenRequest request) {
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

        return service.get(id).map(existing -> {
            Token data = new Token();
            data.setValue(request.value);
            data.setIdentity(identity);
            data.setAuthority(authority);
            data.setIssuedAt(request.issuedAt != null ? request.issuedAt : existing.getIssuedAt());
            data.setExpiresAt(request.expiresAt != null ? request.expiresAt : existing.getExpiresAt());
            data.setRevoked(request.revoked != null ? request.revoked : existing.isRevoked());
            try {
                return service.update(id, data).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
            } catch (DataIntegrityViolationException ex) {
                return ResponseEntity.status(409).build();
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private static class TokenRequest {
        public Long identityId;
        public Long authorityId;
        public String value;
        public Instant issuedAt;
        public Instant expiresAt;
        public Boolean revoked;
    }
}
