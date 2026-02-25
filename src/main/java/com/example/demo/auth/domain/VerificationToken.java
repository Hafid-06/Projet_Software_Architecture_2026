package com.example.demo.auth.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Token de vérification d'e-mail.
 *
 * on ne stocke jamais le token en clair.
 * tokenHash = BCrypt(tokenClear) -> comme pour les mots de passe.
 * tokenId   = identifiant public (UUID) transmis dans l'URL.
 *
 * Lors de la vérification :
 *   BCrypt.matches(tokenReçu, tokenHash) -> true/false
 */
@Entity
@Table(name = "verification_tokens")
public class VerificationToken {

    /** Identifiant public transmis dans le lien (?tokenId=...) */
    @Id
    private String tokenId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    /** Hash BCrypt du token secret (jamais le secret lui-même) */
    @Column(nullable = false)
    private String tokenHash;

    /** Date d'expiration (30 min par défaut, configurable) */
    @Column(nullable = false)
    private Instant expiresAt;

    public VerificationToken() {}

    public VerificationToken(String tokenId, User user, String tokenHash, Instant expiresAt) {
        this.tokenId   = tokenId;
        this.user      = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    // --- Getters / Setters ---

    public String getTokenId() { return tokenId; }

    public User getUser() { return user; }

    public String getTokenHash() { return tokenHash; }

    public Instant getExpiresAt() { return expiresAt; }
}
