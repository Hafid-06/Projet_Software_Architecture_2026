package com.example.demo.notification.consumer;

import com.example.demo.notification.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

// Consumer RabbitMQ qui écoute la queue "notification.user-registered"
// Quand un utilisateur s'inscrit, AuthService publie un événement sur RabbitMQ
// Ce consumer le reçoit et déclenche l'envoi de l'email de vérification
@Component
public class UserRegisteredConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserRegisteredConsumer.class);

    // Injection du service d'envoi d'email
    private final EmailService emailService;

    // Flag pour simuler une erreur (utile pour tester la DLQ)
    // Valeur par défaut = false, configurable dans application.properties
    @Value("${app.notification.simulate-error:false}")
    private boolean simulateError;

    public UserRegisteredConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Méthode appelée AUTOMATIQUEMENT par Spring quand un message arrive
     * dans la queue RabbitMQ "notification.user-registered".
     *
     * Le message JSON est désérialisé en Map<String, Object> par Jackson.
     *
     * Flux :
     * 1. Extrait les champs de l'événement (eventId, email, tokenId, tokenClear)
     * 2. Si simulateError=true → lève une exception → le message part en DLQ (Dead Letter Queue)
     * 3. Si l'événement est incomplet (champs null) → exception → DLQ
     * 4. Sinon → appelle emailService.sendVerificationEmail() → envoie l'email via MailHog
     *
     * En cas d'exception, RabbitMQ réessaie puis envoie le message en DLQ
     * (configurée dans RabbitConfig avec x-dead-letter-exchange)
     */
    @RabbitListener(queues = "${app.mq.queue.userRegistered}")
    public void onUserRegistered(Map<String, Object> event) {
        // Extraction des champs depuis le JSON de l'événement
        String eventId    = (String) event.get("eventId");
        String email      = (String) event.get("email");
        String tokenId    = (String) event.get("tokenId");
        String tokenClear = (String) event.get("tokenClear");

        log.info("[NOTIFICATION] Événement reçu eventId={} email={}", eventId, email);

        // Simulation d'erreur pour tester la DLQ (Dead Letter Queue)
        // Si on met app.notification.simulate-error=true dans la config,
        // le consumer lève une exception → le message va en DLQ au lieu d'être perdu
        if (simulateError) {
            log.error("[NOTIFICATION] Erreur simulée → message envoyé en DLQ eventId={}", eventId);
            throw new RuntimeException("Erreur simulée pour test DLQ");
        }

        // Validation : si l'événement est mal formé, on le rejette → DLQ
        if (email == null || tokenId == null || tokenClear == null) {
            log.error("[NOTIFICATION] Événement mal formé → DLQ eventId={}", eventId);
            throw new IllegalArgumentException("Événement incomplet : " + event);
        }

        // Tout est OK → on envoie l'email de vérification via MailHog
        emailService.sendVerificationEmail(email, tokenId, tokenClear);
        log.info("[NOTIFICATION] Traitement terminé eventId={}", eventId);
    }
}