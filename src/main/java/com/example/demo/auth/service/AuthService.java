package com.example.demo.auth.service;

import com.example.demo.auth.domain.*;
import com.example.demo.auth.repository.AuthorityRepository;
import com.example.demo.auth.repository.CredentialRepository;
import com.example.demo.auth.repository.IdentityRepository;
import com.example.demo.auth.repository.TokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    private final IdentityRepository identities;
    private final AuthorityRepository authorities;
    private final CredentialRepository credentials;
    private final TokenRepository tokens;

    public AuthService(
            IdentityRepository identities,
            AuthorityRepository authorities,
            CredentialRepository credentials,
            TokenRepository tokens
    ) {
        this.identities = identities;
        this.authorities = authorities;
        this.credentials = credentials;
        this.tokens = tokens;
    }

    @Transactional
    public boolean register(String email, String credentialValue, AuthMethod method) {
        if (identities.findByEmail(email).isPresent()) return false;
        Identity id = identities.save(new Identity(email, email));
        Authority auth = authorities.findByMethod(method).orElseGet(() -> authorities.save(new Authority(method)));
        credentials.save(new Credential(credentialValue, id, auth));
        return true;
    }

    @Transactional
    public String login(String email, String credentialValue, AuthMethod method) {
        Optional<Identity> idOpt = identities.findByEmail(email);
        if (idOpt.isEmpty()) return null;
        Identity id = idOpt.get();
        Optional<Authority> authOpt = authorities.findByMethod(method);
        if (authOpt.isEmpty() || !authOpt.get().isEnabled()) return null;
        Authority auth = authOpt.get();
        Optional<Credential> credOpt = credentials.findByIdentityAndAuthority(id, auth);
        if (credOpt.isEmpty()) return null;
        Credential cred = credOpt.get();
        if (!cred.getValue().equals(credentialValue)) return null;
        String tokenValue = UUID.randomUUID().toString();
        Token token = new Token(tokenValue, id, auth, Instant.now().plus(1, ChronoUnit.HOURS));
        tokens.save(token);
        return tokenValue;
    }

    public Token verify(String tokenValue) {
        Optional<Token> tokenOpt = tokens.findByValue(tokenValue);
        if (tokenOpt.isEmpty()) return null;
        Token token = tokenOpt.get();
        if (token.isRevoked()) return null;
        if (token.getExpiresAt().isBefore(Instant.now())) return null;
        return token;
    }

    @Transactional
    public boolean logout(String tokenValue) {
        Optional<Token> tokenOpt = tokens.findByValue(tokenValue);
        if (tokenOpt.isEmpty()) return false;
        Token token = tokenOpt.get();
        token.setRevoked(true);
        tokens.save(token);
        return true;
    }
}
