package com.example.demo.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${app.mq.exchange}")
    private String exchange;

    @Value("${app.mq.queue.userRegistered}")
    private String queueUserRegistered;

    @Value("${app.mq.queue.emailVerified}")
    private String queueEmailVerified;

    @Value("${app.mq.rk.userRegistered}")
    private String rkUserRegistered;

    @Value("${app.mq.rk.emailVerified}")
    private String rkEmailVerified;

    // Exchange principal topic
    @Bean
    public TopicExchange authExchange() {
        return ExchangeBuilder.topicExchange(exchange).durable(true).build();
    }

    // Dead Letter Exchange
    @Bean
    public FanoutExchange dlxExchange() {
        return ExchangeBuilder.fanoutExchange(exchange + ".dlx").durable(true).build();
    }

    // Dead Letter Queue
    @Bean
    public Queue dlq() {
        return QueueBuilder.durable(exchange + ".dlq").build();
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(dlq()).to(dlxExchange());
    }

    // File notification.user-registered avec DLX
    @Bean
    public Queue userRegisteredQueue() {
        return QueueBuilder.durable(queueUserRegistered)
                .withArgument("x-dead-letter-exchange", exchange + ".dlx")
                .build();
    }

    @Bean
    public Binding userRegisteredBinding() {
        return BindingBuilder.bind(userRegisteredQueue())
                .to(authExchange())
                .with(rkUserRegistered);
    }

    // File analytics.email-verified
    @Bean
    public Queue emailVerifiedQueue() {
        return QueueBuilder.durable(queueEmailVerified).build();
    }

    @Bean
    public Binding emailVerifiedBinding() {
        return BindingBuilder.bind(emailVerifiedQueue())
                .to(authExchange())
                .with(rkEmailVerified);
    }

    // Sérialisation JSON
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}