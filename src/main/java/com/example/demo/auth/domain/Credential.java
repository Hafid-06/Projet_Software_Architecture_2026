package com.example.demo.auth.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "credentials",
       uniqueConstraints = @UniqueConstraint(columnNames = {"identity_id", "authority_id"}))
public class Credential {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Simple string value (quoi)
    @Column(name = "credential_value", nullable = false)
    private String value;

    @ManyToOne(optional = false)
    @JoinColumn(name = "identity_id")
    @com.fasterxml.jackson.annotation.JsonBackReference
    private Identity identity;

    @ManyToOne(optional = false)
    @JoinColumn(name = "authority_id")
    private Authority authority;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Credential() {}

    public Credential(String value, Identity identity, Authority authority) {
        this.value = value;
        this.identity = identity;
        this.authority = authority;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public Identity getIdentity() { return identity; }
    public void setIdentity(Identity identity) { this.identity = identity; }

    public Authority getAuthority() { return authority; }
    public void setAuthority(Authority authority) { this.authority = authority; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
