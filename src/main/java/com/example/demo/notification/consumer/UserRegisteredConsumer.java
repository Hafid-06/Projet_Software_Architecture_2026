package com.example.demo.notification.consumer;

import com.example.demo.notification.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UserRegisteredConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserRegisteredConsumer.class);

    private final EmailService emailService;

    @Value("${app.notification.simulate-error:false}")
    private boolean simulateError;

    public UserRegisteredConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = "${app.mq.queue.userRegistered}")
    public void onUserRegistered(Map<String, Object> event) {
        String eventId    = (String) event.get("eventId");
        String email      = (String) event.get("email");
        String tokenId    = (String) event.get("tokenId");
        String tokenClear = (String) event.get("tokenClear");

        log.info("[NOTIFICATION] Événement reçu eventId={} email={}", eventId, email);

        // Simulation d'erreur pour tester la DLQ
        if (simulateError) {
            log.error("[NOTIFICATION] Erreur simulée → message envoyé en DLQ eventId={}", eventId);
            throw new RuntimeException("Erreur simulée pour test DLQ");
        }

        if (email == null || tokenId == null || tokenClear == null) {
            log.error("[NOTIFICATION] Événement mal formé → DLQ eventId={}", eventId);
            throw new IllegalArgumentException("Événement incomplet : " + event);
        }

        emailService.sendVerificationEmail(email, tokenId, tokenClear);
        log.info("[NOTIFICATION] Traitement terminé eventId={}", eventId);
    }
}