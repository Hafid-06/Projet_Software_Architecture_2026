package com.example.demo.auth.service;

import com.example.demo.auth.domain.Authority;
import com.example.demo.auth.domain.AuthMethod;
import com.example.demo.auth.repository.AuthorityRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AuthorityService {
    private final AuthorityRepository repo;

    public AuthorityService(AuthorityRepository repo) {
        this.repo = repo;
    }

    public List<Authority> list() { return repo.findAll(); }
    public Optional<Authority> get(Long id) { return repo.findById(id); }
    public Optional<Authority> getByMethod(AuthMethod method) { return repo.findByMethod(method); }
    public Authority create(Authority authority) { return repo.save(authority); }
    public Optional<Authority> update(Long id, Authority data) {
        return repo.findById(id).map(existing -> {
            existing.setMethod(data.getMethod());
            existing.setEnabled(data.isEnabled());
            return repo.save(existing);
        });
    }
    public void delete(Long id) { repo.deleteById(id); }
}
