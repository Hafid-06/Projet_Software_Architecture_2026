package com.example.demo.auth.repository;

import com.example.demo.auth.domain.Authority;
import com.example.demo.auth.domain.AuthMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthorityRepository extends JpaRepository<Authority, Long> {
    Optional<Authority> findByMethod(AuthMethod method);
}
