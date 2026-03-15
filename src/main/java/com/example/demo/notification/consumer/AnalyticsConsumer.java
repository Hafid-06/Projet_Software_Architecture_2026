package com.example.demo.notification.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

// Consumer RabbitMQ qui écoute la queue "analytics.email-verified"
// Quand un email est vérifié avec succès, AuthService publie un EmailVerifiedEvent
// Ce consumer le reçoit et met à jour un compteur (simule un service d'analytics)
@Component
public class AnalyticsConsumer {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsConsumer.class);

    // Compteur thread-safe (AtomicInteger) pour compter le nombre total d'emails vérifiés
    // AtomicInteger est utilisé car plusieurs threads peuvent appeler onEmailVerified() en parallèle
    // (si plusieurs messages arrivent en même temps sur la queue)
    private final AtomicInteger counter = new AtomicInteger(0);

    /**
     * Méthode appelée AUTOMATIQUEMENT par Spring quand un message arrive
     * dans la queue RabbitMQ "analytics.email-verified".
     *
     * Le message contient : eventId, userId, occurredAt
     * → On incrémente le compteur et on log le total
     *
     * C'est un exemple simple de consumer analytics :
     * dans un vrai projet, on pourrait stocker les stats en BDD,
     * envoyer des métriques à Prometheus, etc.
     */
    @RabbitListener(queues = "${app.mq.queue.emailVerified}")
    public void onEmailVerified(Map<String, Object> event) {
        // Extraction des champs depuis le JSON
        String eventId = (String) event.get("eventId");
        String userId  = (String) event.get("userId");

        // Incrémente le compteur de manière thread-safe et récupère la nouvelle valeur
        int total = counter.incrementAndGet();

        log.info("[ANALYTICS] EmailVerified reçu eventId={} userId={}", eventId, userId);
        log.info("[ANALYTICS] Total emails vérifiés : {}", total);
    }
}