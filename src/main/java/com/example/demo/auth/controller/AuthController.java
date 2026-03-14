package com.example.demo.auth.controller;

import com.example.demo.auth.service.AuthService;
import com.example.demo.auth.service.TokenStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.regex.Pattern;

@RestController
public class AuthController {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final AuthService authService;
    private final TokenStore tokenStore;

    public AuthController(AuthService authService, TokenStore tokenStore) {
        this.authService = authService;
        this.tokenStore = tokenStore;
    }

    // Endpoint pour l'inscription d'un utilisateur
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest body) {
        if (body.email == null || body.email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "email requis"));
        }
        if (!EMAIL_PATTERN.matcher(body.email.trim()).matches()) {
            return ResponseEntity.badRequest().body(Map.of("error", "email invalide"));
        }
        if (body.password == null || body.password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "password requis"));
        }
        try {
            authService.register(body.email.trim().toLowerCase(), body.password);
            return ResponseEntity.status(201)
                    .body(Map.of("status", "REGISTERED",
                                 "message", "Un e-mail de vérification a été envoyé"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    // Endpoint pour la connexion avec JWT
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest body) {
        if (body.email == null || body.email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "email requis"));
        }
        if (!EMAIL_PATTERN.matcher(body.email.trim()).matches()) {
            return ResponseEntity.badRequest().body(Map.of("error", "email invalide"));
        }
        if (body.password == null || body.password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "password requis"));
        }
        AuthService.LoginResult result = authService.login(body.email.trim().toLowerCase(), body.password);
        return switch (result.status()) {
            case SUCCESS -> ResponseEntity.ok(
                    Map.of("status", "AUTHENTICATED",
                           "token", result.token(),
                           "message", "Connexion réussie"));
            case NOT_VERIFIED -> ResponseEntity.status(403)
                    .body(Map.of("error", "E-mail non vérifié. Vérifiez votre boîte mail."));
            case INVALID_CREDENTIALS -> ResponseEntity.status(401)
                    .body(Map.of("error", "Email ou mot de passe incorrect"));
        };
    }

    // Endpoint pour vérifier un e-mail
    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam String tokenId,
                                    @RequestParam String t) {
        AuthService.VerifyResult result = authService.verify(tokenId, t);
        return switch (result) {
            case SUCCESS -> ResponseEntity.ok(
                    Map.of("status", "VERIFIED",
                           "message", "E-mail vérifié avec succès !"));
            case ALREADY_VERIFIED -> ResponseEntity.ok(
                    Map.of("status", "ALREADY_VERIFIED",
                           "message", "E-mail déjà vérifié, aucune action nécessaire."));
            case EXPIRED -> ResponseEntity.badRequest()
                    .body(Map.of("error", "Token expiré"));
            case INVALID -> ResponseEntity.badRequest()
                    .body(Map.of("error", "Token invalide"));
        };
    }

    /**
     * Endpoint interne appelé par NGINX auth_request.
     * Vérifie le header Authorization: Bearer <token>
     * Retourne 200 si valide, 401 sinon.
     */
    @GetMapping("/auth/validate")
    public ResponseEntity<?> validate(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Token manquant"));
        }
        String token = authHeader.substring(7);
        if (!tokenStore.isValid(token)) {
            return ResponseEntity.status(401).body(Map.of("error", "Token invalide"));
        }
        return ResponseEntity.ok(Map.of("status", "VALID"));
    }

    public static class RegisterRequest {
        public String email;
        public String password;
    }

    public static class LoginRequest {
        public String email;
        public String password;
    }
}