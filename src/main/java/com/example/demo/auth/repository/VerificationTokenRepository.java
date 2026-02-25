package com.example.demo.auth.repository;

import com.example.demo.auth.domain.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, String> {
    // findById(tokenId) est fourni par JpaRepository
}
