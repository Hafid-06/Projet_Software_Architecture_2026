package com.example.demo.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${app.mq.exchange}")
    private String exchange;

    @Value("${app.mq.queue.userRegistered}")
    private String queueName;

    @Value("${app.mq.rk.userRegistered}")
    private String routingKey;

    @Bean
    public TopicExchange notificationAuthExchange() {
        return ExchangeBuilder.topicExchange(exchange).durable(true).build();
    }

    @Bean
    public FanoutExchange notificationDlxExchange() {
        return ExchangeBuilder.fanoutExchange(exchange + ".dlx").durable(true).build();
    }

    @Bean
    public Queue notificationDlq() {
        return QueueBuilder.durable(queueName + ".dlq").build();
    }

    @Bean
    public Binding notificationDlqBinding() {
        return BindingBuilder.bind(notificationDlq()).to(notificationDlxExchange());
    }

    @Bean
    public Queue userRegisteredQueue() {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", exchange + ".dlx")
                .build();
    }

    @Bean
    public Binding userRegisteredBinding() {
        return BindingBuilder
                .bind(userRegisteredQueue())
                .to(notificationAuthExchange())
                .with(routingKey);
    }

    @Bean
    public Jackson2JsonMessageConverter notificationJsonConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(notificationJsonConverter());
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}