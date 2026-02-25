package com.example.demo.auth.service;

import com.example.demo.auth.domain.User;
import com.example.demo.auth.domain.VerificationToken;
import com.example.demo.auth.event.UserRegisteredEvent;
import com.example.demo.auth.repository.UserRepository;
import com.example.demo.auth.repository.VerificationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private final UserRepository users;
    private final VerificationTokenRepository tokens;
    private final RabbitTemplate rabbit;

    @Value("${app.mq.exchange}")
    private String exchange;

    @Value("${app.mq.rk.userRegistered}")
    private String rkUserRegistered;

    @Value("${app.token.expiry-minutes:30}")
    private int expiryMinutes;

    public AuthService(UserRepository users,
                       VerificationTokenRepository tokens,
                       RabbitTemplate rabbit) {
        this.users  = users;
        this.tokens = tokens;
        this.rabbit = rabbit;
    }

    // ----------------------------------------------------------------
    // POST /register
    // ----------------------------------------------------------------

    /**
     * Inscription :
     *  1. Crée l'utilisateur (verified=false)
     *  2. Génère un token UUID, stocke son HASH en base
     *  3. Publie UserRegistered dans RabbitMQ avec le token EN CLAIR
     *     (uniquement dans le message, jamais en base)
     */
    @Transactional
    public void register(String email) {
        // Vérification doublon
        if (users.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email déjà utilisé : " + email);
        }

        // 1. Créer l'utilisateur
        User user = users.save(new User(email));
        log.info("[AUTH] Utilisateur créé id={} email={}", user.getId(), email);

        // 2. Générer le token secret + son hash
        String tokenClear = UUID.randomUUID().toString();   // secret, jamais stocké
        String tokenHash  = ENCODER.encode(tokenClear);     // seul le hash va en base
        String tokenId    = UUID.randomUUID().toString();   // identifiant public (dans l'URL)
        Instant expiresAt = Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES);

        tokens.save(new VerificationToken(tokenId, user, tokenHash, expiresAt));
        log.info("[AUTH] Token créé tokenId={} expiresAt={}", tokenId, expiresAt);

        // 3. Publier l'événement UserRegistered
        UserRegisteredEvent event = new UserRegisteredEvent(
                UUID.randomUUID().toString(),       // eventId unique
                String.valueOf(user.getId()),        // userId
                email,
                tokenId,
                tokenClear,                         // inclus dans l'événement pour le flux TP
                Instant.now().toString()
        );

        rabbit.convertAndSend(exchange, rkUserRegistered, event,
                msg -> {
                    msg.getMessageProperties().setHeader("x-correlation-id", event.getEventId());
                    msg.getMessageProperties().setHeader("x-schema-version", 1);
                    return msg;
                });

        log.info("[AUTH] Événement UserRegistered publié eventId={}", event.getEventId());
    }

    // ----------------------------------------------------------------
    // GET /verify
    // ----------------------------------------------------------------

    /**
     * Vérification du lien cliqué par l'utilisateur :
     *  1. Retrouve le token par tokenId
     *  2. Vérifie l'expiration
     *  3. Compare BCrypt(tokenReçu) == tokenHash stocké
     *  4. Marque l'utilisateur comme vérifié
     *  5. Supprime le token (usage unique / one-shot)
     */
    @Transactional
    public void verify(String tokenId, String tokenClear) {
        VerificationToken vt = tokens.findById(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Token introuvable"));

        // Vérification expiration
        if (Instant.now().isAfter(vt.getExpiresAt())) {
            tokens.delete(vt); // nettoyage
            throw new IllegalArgumentException("Token expiré");
        }

        // Vérification du secret par comparaison BCrypt
        if (!ENCODER.matches(tokenClear, vt.getTokenHash())) {
            throw new IllegalArgumentException("Token invalide");
        }

        // Marquer l'utilisateur comme vérifié
        User user = vt.getUser();
        user.setVerified(true);
        users.save(user);
        log.info("[AUTH] Email vérifié pour userId={} email={}", user.getId(), user.getEmail());

        // Supprimer le token (usage unique)
        tokens.delete(vt);
        log.info("[AUTH] Token supprimé tokenId={}", tokenId);
    }
}
