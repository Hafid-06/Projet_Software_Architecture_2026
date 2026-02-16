package com.example.demo.auth.service;

import com.example.demo.auth.domain.Token;
import com.example.demo.auth.repository.TokenRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class TokenService {
    private final TokenRepository repo;

    public TokenService(TokenRepository repo) {
        this.repo = repo;
    }

    public List<Token> list() { return repo.findAll(); }
    public Optional<Token> get(Long id) { return repo.findById(id); }
    public Optional<Token> getByValue(String value) { return repo.findByValue(value); }
    public boolean isValid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return repo.findByValue(value)
                .filter(token -> !token.isRevoked())
                .filter(token -> token.getExpiresAt() != null && token.getExpiresAt().isAfter(Instant.now()))
                .isPresent();
    }
    public Token create(Token token) { return repo.save(token); }
    public Optional<Token> update(Long id, Token data) {
        return repo.findById(id).map(existing -> {
            existing.setValue(data.getValue());
            existing.setIdentity(data.getIdentity());
            existing.setAuthority(data.getAuthority());
            existing.setIssuedAt(data.getIssuedAt());
            existing.setExpiresAt(data.getExpiresAt());
            existing.setRevoked(data.isRevoked());
            return repo.save(existing);
        });
    }
    public void delete(Long id) { repo.deleteById(id); }
}
