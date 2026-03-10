package com.example.demo.auth.controller;

import com.example.demo.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Endpoint pour l'inscription d'un utilisateur
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest body) {
        if (body.email == null || body.email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "email requis"));
        }
        try {
            authService.register(body.email.trim().toLowerCase());
            return ResponseEntity.status(201)
                    .body(Map.of("status", "REGISTERED",
                                 "message", "Un e-mail de vérification a été envoyé"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
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

    public static class RegisterRequest {
        public String email;
    }
}