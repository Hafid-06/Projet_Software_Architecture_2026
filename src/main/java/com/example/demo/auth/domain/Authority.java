package com.example.demo.auth.domain;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "authorities")
public class Authority {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthMethod method;

    @Column(nullable = false)
    private boolean enabled = true;

    @OneToMany(mappedBy = "authority", cascade = CascadeType.PERSIST, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonManagedReference
    private List<Credential> credentials = new ArrayList<>();

    @OneToMany(mappedBy = "authority", cascade = CascadeType.PERSIST, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonManagedReference
    private List<Token> tokens = new ArrayList<>();

    public Authority() {}

    public Authority(AuthMethod method) {
        this.method = method;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AuthMethod getMethod() { return method; }
    public void setMethod(AuthMethod method) { this.method = method; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public List<Credential> getCredentials() { return credentials; }
    public void setCredentials(List<Credential> credentials) { this.credentials = credentials; }

    public List<Token> getTokens() { return tokens; }
    public void setTokens(List<Token> tokens) { this.tokens = tokens; }
}
