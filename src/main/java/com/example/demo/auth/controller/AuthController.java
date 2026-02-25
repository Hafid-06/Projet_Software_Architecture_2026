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

    // ----------------------------------------------------------------
    // POST /register
    // Body JSON : { "email": "alice@example.com" }
    // ----------------------------------------------------------------
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

    // ----------------------------------------------------------------
    // GET /verify?tokenId=xxx&t=yyy
    // tokenId : identifiant public du token
    // t       : valeur secrète du token (en clair, reçue dans l'URL)
    // ----------------------------------------------------------------
    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam String tokenId,
                                    @RequestParam String t) {
        try {
            authService.verify(tokenId, t);
            return ResponseEntity.ok(Map.of("status", "VERIFIED",
                                            "message", "E-mail vérifié avec succès !"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // DTO interne pour la requête d'inscription
    public static class RegisterRequest {
        public String email;
    }
}