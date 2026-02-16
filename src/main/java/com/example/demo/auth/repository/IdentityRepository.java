package com.example.demo.auth.repository;

import com.example.demo.auth.domain.Identity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdentityRepository extends JpaRepository<Identity, Long> {
    Optional<Identity> findByEmail(String email);
}
