package com.example.demo.auth.service;

import com.example.demo.auth.domain.User;
import com.example.demo.auth.domain.VerificationToken;
import com.example.demo.auth.event.EmailVerifiedEvent;
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

    @Value("${app.mq.rk.emailVerified}")
    private String rkEmailVerified;

    @Value("${app.token.expiry-minutes:30}")
    private int expiryMinutes;

    public AuthService(UserRepository users,
                       VerificationTokenRepository tokens,
                       RabbitTemplate rabbit) {
        this.users  = users;
        this.tokens = tokens;
        this.rabbit = rabbit;
    }

    @Transactional
    public void register(String email) {
        if (users.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email déjà utilisé : " + email);
        }

        User user = users.save(new User(email));
        log.info("[AUTH] Utilisateur créé id={} email={}", user.getId(), email);

        String tokenClear = UUID.randomUUID().toString();
        String tokenHash  = ENCODER.encode(tokenClear);
        String tokenId    = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES);

        tokens.save(new VerificationToken(tokenId, user, tokenHash, expiresAt));
        log.info("[AUTH] Token créé tokenId={} expiresAt={}", tokenId, expiresAt);

        UserRegisteredEvent event = new UserRegisteredEvent(
                UUID.randomUUID().toString(),
                String.valueOf(user.getId()),
                email,
                tokenId,
                tokenClear,
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

    @Transactional
    public VerifyResult verify(String tokenId, String tokenClear) {

        VerificationToken vt = tokens.findById(tokenId).orElse(null);

        if (vt == null) {
            log.info("[AUTH] Token introuvable tokenId={} → idempotence", tokenId);
            return VerifyResult.ALREADY_VERIFIED;
        }

        if (Instant.now().isAfter(vt.getExpiresAt())) {
            tokens.delete(vt);
            log.warn("[AUTH] Token expiré tokenId={}", tokenId);
            return VerifyResult.EXPIRED;
        }

        if (!ENCODER.matches(tokenClear, vt.getTokenHash())) {
            log.warn("[AUTH] Token invalide tokenId={}", tokenId);
            return VerifyResult.INVALID;
        }

        User user = vt.getUser();

        if (user.isVerified()) {
            tokens.delete(vt);
            log.info("[AUTH] Déjà vérifié userId={}", user.getId());
            return VerifyResult.ALREADY_VERIFIED;
        }

        user.setVerified(true);
        users.save(user);
        log.info("[AUTH] Email vérifié userId={} email={}", user.getId(), user.getEmail());

        tokens.delete(vt);
        log.info("[AUTH] Token supprimé tokenId={}", tokenId);

        // Publier EmailVerified
        EmailVerifiedEvent verifiedEvent = new EmailVerifiedEvent(
                UUID.randomUUID().toString(),
                String.valueOf(user.getId()),
                Instant.now().toString()
        );

        rabbit.convertAndSend(exchange, rkEmailVerified, verifiedEvent,
                msg -> {
                    msg.getMessageProperties().setHeader("x-correlation-id", verifiedEvent.getEventId());
                    msg.getMessageProperties().setHeader("x-schema-version", 1);
                    return msg;
                });

        log.info("[AUTH] Événement EmailVerified publié eventId={}", verifiedEvent.getEventId());

        return VerifyResult.SUCCESS;
    }

    public enum VerifyResult {
        SUCCESS,
        ALREADY_VERIFIED,
        EXPIRED,
        INVALID
    }
}