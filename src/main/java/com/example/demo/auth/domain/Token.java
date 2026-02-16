package com.example.demo.auth.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tokens")
public class Token {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_value", nullable = false, unique = true)
    private String value; // preuve

    @ManyToOne(optional = false)
    @JoinColumn(name = "identity_id")
    @com.fasterxml.jackson.annotation.JsonBackReference
    private Identity identity;

    @ManyToOne(optional = false)
    @JoinColumn(name = "authority_id")
    @com.fasterxml.jackson.annotation.JsonBackReference
    private Authority authority;

    @Column(nullable = false)
    private Instant issuedAt = Instant.now();

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    public Token() {}

    public Token(String value, Identity identity, Authority authority, Instant expiresAt) {
        this.value = value;
        this.identity = identity;
        this.authority = authority;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public Identity getIdentity() { return identity; }
    public void setIdentity(Identity identity) { this.identity = identity; }

    public Authority getAuthority() { return authority; }
    public void setAuthority(Authority authority) { this.authority = authority; }

    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
}
