package com.example.demo.auth.controller;

import com.example.demo.auth.repository.UserRepository;
import com.example.demo.auth.repository.VerificationTokenRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class UserController {

    private final UserRepository users;
    private final VerificationTokenRepository tokens;

    public UserController(UserRepository users, VerificationTokenRepository tokens) {
        this.users = users;
        this.tokens = tokens;
    }

    @GetMapping("/users")
    public List<Map<String, Object>> getUsers() {
        return users.findAll().stream()
            .map(u -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", u.getId());
                map.put("email", u.getEmail());
                map.put("verified", u.isVerified());
                return map;
            })
            .collect(Collectors.toList());
    }
//Nombre de tokens en attente de vérification
    @GetMapping("/pending-count")
    public Map<String, Object> getPendingCount() {
        Map<String, Object> map = new HashMap<>();
        map.put("count", tokens.count());
        return map;
    }
}