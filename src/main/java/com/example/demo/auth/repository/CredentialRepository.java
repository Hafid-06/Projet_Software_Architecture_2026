package com.example.demo.auth.repository;

import com.example.demo.auth.domain.Authority;
import com.example.demo.auth.domain.Credential;
import com.example.demo.auth.domain.Identity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CredentialRepository extends JpaRepository<Credential, Long> {
    Optional<Credential> findByIdentityAndAuthority(Identity identity, Authority authority);
}
