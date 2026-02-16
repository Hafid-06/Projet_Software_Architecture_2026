package com.example.demo.auth.service;

import com.example.demo.auth.domain.Authority;
import com.example.demo.auth.domain.Credential;
import com.example.demo.auth.domain.Identity;
import com.example.demo.auth.repository.CredentialRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CredentialService {
    private final CredentialRepository repo;

    public CredentialService(CredentialRepository repo) {
        this.repo = repo;
    }

    public List<Credential> list() { return repo.findAll(); }
    public Optional<Credential> get(Long id) { return repo.findById(id); }
    public Optional<Credential> getByIdentityAndAuthority(Identity identity, Authority authority) {
        return repo.findByIdentityAndAuthority(identity, authority);
    }
    public Credential create(Credential credential) { return repo.save(credential); }
    public Optional<Credential> update(Long id, Credential data) {
        return repo.findById(id).map(existing -> {
            existing.setValue(data.getValue());
            existing.setIdentity(data.getIdentity());
            existing.setAuthority(data.getAuthority());
            return repo.save(existing);
        });
    }
    public void delete(Long id) { repo.deleteById(id); }
}
