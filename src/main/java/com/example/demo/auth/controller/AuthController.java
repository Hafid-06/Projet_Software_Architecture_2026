package com.example.demo.auth.controller;

import com.example.demo.auth.domain.AuthMethod;
import com.example.demo.auth.domain.Token;
import com.example.demo.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    public static class RegisterRequest {
        public String email;      // identity (who)
        public String credential; // credential (what)
        public AuthMethod method = AuthMethod.PASSWORD; // authority (how)
    }

    public static class LoginRequest {
        public String email;
        public String credential;
        public AuthMethod method = AuthMethod.PASSWORD;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest body) {
        if (body == null || body.email == null || body.email.isBlank() || body.credential == null || body.credential.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "email and credential are required"));
        }
        boolean ok = auth.register(body.email.trim(), body.credential, body.method);
        if (!ok) return ResponseEntity.status(409).body(Map.of("error", "email already registered"));
        return ResponseEntity.ok(Map.of("status", "REGISTERED", "id", body.email.trim(), "method", body.method.name()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest body) {
        if (body == null || body.email == null || body.email.isBlank() || body.credential == null || body.credential.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "email and credential are required"));
        }
        String tokenVal = auth.login(body.email.trim(), body.credential, body.method);
        if (tokenVal == null) return ResponseEntity.status(401).body(Map.of("error", "invalid credentials"));
        Token info = auth.verify(tokenVal);
        return ResponseEntity.ok(Map.of(
                "token", tokenVal,
                "id", info.getIdentity().getEmail(),
                "method", info.getAuthority().getMethod().name(),
                "issuedAt", info.getIssuedAt().toString()
        ));
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam String token) {
        Token info = auth.verify(token);
        if (info == null) return ResponseEntity.status(401).body(Map.of("error", "invalid token"));
        return ResponseEntity.ok(Map.of(
                "token", info.getValue(),
                "id", info.getIdentity().getEmail(),
                "method", info.getAuthority().getMethod().name(),
                "issuedAt", info.getIssuedAt().toString(),
                "expiresAt", info.getExpiresAt().toString()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestParam String token) {
        boolean ok = auth.logout(token);
        return ResponseEntity.ok(Map.of("revoked", ok));
    }
}
