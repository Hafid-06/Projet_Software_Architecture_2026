package com.example.demo.notification.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AnalyticsConsumer {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsConsumer.class);

    // Compteur thread-safe des emails vérifiés
    private final AtomicInteger counter = new AtomicInteger(0);

    @RabbitListener(queues = "${app.mq.queue.emailVerified}")
    public void onEmailVerified(Map<String, Object> event) {
        String eventId = (String) event.get("eventId");
        String userId  = (String) event.get("userId");

        int total = counter.incrementAndGet();

        log.info("[ANALYTICS] EmailVerified reçu eventId={} userId={}", eventId, userId);
        log.info("[ANALYTICS] Total emails vérifiés : {}", total);
    }
}