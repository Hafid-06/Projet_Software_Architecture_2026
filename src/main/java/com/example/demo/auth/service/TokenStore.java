package com.example.demo.auth.service;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stockage en mémoire des tokens de session actifs.
 * Utilisé par NGINX auth_request pour valider les requêtes.
 */
@Component
public class TokenStore {

    private final Set<String> activeTokens = ConcurrentHashMap.newKeySet();

    public void store(String token) {
        activeTokens.add(token);
    }

    public boolean isValid(String token) {
        return activeTokens.contains(token);
    }

    public void revoke(String token) {
        activeTokens.remove(token);
    }
}
