package com.example.demo.notification.consumer;

import com.example.demo.notification.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consommateur RabbitMQ.
 *
 * Écoute la file "notification.user-registered".
 * Lorsqu'un événement UserRegistered arrive, il envoie l'e-mail de vérification.
 *
 * En cas d'exception non catchée :
 *   Le message est nack'd sans requeue
 *   Il part automatiquement en DLQ (notification.user-registered.dlq)
 */
@Component
public class UserRegisteredConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserRegisteredConsumer.class);

    private final EmailService emailService;

    public UserRegisteredConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = "${app.mq.queue.userRegistered}")
    public void onUserRegistered(Map<String, Object> event) {
        // Extraction des champs de l'événement
        String eventId    = (String) event.get("eventId");
        String email      = (String) event.get("email");
        String tokenId    = (String) event.get("tokenId");
        String tokenClear = (String) event.get("tokenClear");

        log.info("[NOTIFICATION] Événement reçu eventId={} email={}", eventId, email);

        // Validation minimale avant traitement
        if (email == null || tokenId == null || tokenClear == null) {
            log.error("[NOTIFICATION] Événement mal formé eventId={} → envoi en DLQ", eventId);
            // Lancer une exception provoque le nack → DLQ
            throw new IllegalArgumentException("Événement UserRegistered incomplet : " + event);
        }

        // Envoi de l'e-mail (via MailHog en local)
        emailService.sendVerificationEmail(email, tokenId, tokenClear);

        log.info("[NOTIFICATION] Traitement terminé pour eventId={}", eventId);
    }
}
