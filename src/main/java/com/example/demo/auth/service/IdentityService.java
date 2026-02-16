package com.example.demo.auth.service;

import com.example.demo.auth.domain.Identity;
import com.example.demo.auth.repository.IdentityRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class IdentityService {
    private final IdentityRepository repo;

    public IdentityService(IdentityRepository repo) {
        this.repo = repo;
    }

    public List<Identity> list() { return repo.findAll(); }
    public Optional<Identity> get(Long id) { return repo.findById(id); }
    public Optional<Identity> getByEmail(String email) { return repo.findByEmail(email); }
    public Identity create(Identity identity) { return repo.save(identity); }
    public Optional<Identity> update(Long id, Identity data) {
        return repo.findById(id).map(existing -> {
            existing.setEmail(data.getEmail());
            existing.setName(data.getName());
            return repo.save(existing);
        });
    }
    public void delete(Long id) { repo.deleteById(id); }
}
