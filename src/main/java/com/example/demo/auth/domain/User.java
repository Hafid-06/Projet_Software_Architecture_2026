package com.example.demo.auth.domain;

import jakarta.persistence.*;

/**
 * Représente un utilisateur inscrit.
 * verified=false tant que l'e-mail n'a pas été confirmé.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    /** Mot de passe hashé (BCrypt) */
    @Column(nullable = false)
    private String passwordHash;

    /** false à l'inscription, true après clic sur le lien de vérification */
    @Column(nullable = false)
    private boolean verified = false;

    public User() {}

    public User(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }

    // --- Getters / Setters ---

    public Long getId() { return id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
}