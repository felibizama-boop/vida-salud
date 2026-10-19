package com.vidasalud.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración avanzada de RabbitMQ para VidaSalud.
 * Define la topología de red: Exchanges, Queues, DLQs y Bindings.
 */
@Configuration
public class RabbitMQConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQConfig.class);

    @Value("${vidasalud.rabbitmq.exchanges.direct}")
    private String directExchangeName;

    @Value("${vidasalud.rabbitmq.exchanges.topic}")
    private String topicExchangeName;

    @Value("${vidasalud.rabbitmq.queues.notificaciones.name}")
    private String notificacionesQueueName;

    @Value("${vidasalud.rabbitmq.queues.notificaciones.dlq}")
    private String notificacionesDlqName;

    @Value("${vidasalud.rabbitmq.queues.notificaciones.routing-key}")
    private String notificacionesRoutingKey;

    @Value("${vidasalud.rabbitmq.queues.tickets.name}")
    private String ticketsQueueName;

    @Value("${vidasalud.rabbitmq.queues.tickets.dlq}")
    private String ticketsDlqName;

    @Value("${vidasalud.rabbitmq.queues.tickets.routing-key}")
    private String ticketsRoutingKey;

    @Value("${vidasalud.rabbitmq.queues.documentos.name}")
    private String documentosQueueName;

    @Value("${vidasalud.rabbitmq.queues.documentos.dlq}")
    private String documentosDlqName;

    @Value("${vidasalud.rabbitmq.queues.documentos.routing-key}")
    private String documentosRoutingKey;

    // --- Exchanges ---

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(directExchangeName);
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(topicExchangeName);
    }

    // --- Dead Letter Queues (DLQ) ---

    @Bean
    public Queue notificacionesDlq() {
        return QueueBuilder.durable(notificacionesDlqName).build();
    }

    @Bean
    public Queue ticketsDlq() {
        return QueueBuilder.durable(ticketsDlqName).build();
    }

    @Bean
    public Queue documentosDlq() {
        return QueueBuilder.durable(documentosDlqName).build();
    }

    // --- Colas Principales con configuración de DLQ ---

    @Bean
    public Queue notificacionesQueue() {
        return QueueBuilder.durable(notificacionesQueueName)
                .withArgument("x-dead-letter-exchange", directExchangeName)
                .withArgument("x-dead-letter-routing-key", notificacionesDlqName)
                .build();
    }

    @Bean
    public Queue ticketsQueue() {
        return QueueBuilder.durable(ticketsQueueName)
                .withArgument("x-dead-letter-exchange", directExchangeName)
                .withArgument("x-dead-letter-routing-key", ticketsDlqName)
                .build();
    }

    @Bean
    public Queue documentosQueue() {
        return QueueBuilder.durable(documentosQueueName)
                .withArgument("x-dead-letter-exchange", topicExchangeName)
                .withArgument("x-dead-letter-routing-key", documentosDlqName)
                .build();
    }

    // --- Bindings ---

    @Bean
    public Binding bindingNotificaciones(Queue notificacionesQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(notificacionesQueue).to(directExchange).with(notificacionesRoutingKey);
    }

    @Bean
    public Binding bindingTickets(Queue ticketsQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(ticketsQueue).to(directExchange).with(ticketsRoutingKey);
    }

    @Bean
    public Binding bindingDocumentos(Queue documentosQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(documentosQueue).to(topicExchange).with(documentosRoutingKey);
    }

    // --- Administración y Arranque ---

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }

    @Bean
    public ApplicationRunner initializeInfrastructure(RabbitAdmin rabbitAdmin) {
        return args -> {
            log.info("[RabbitMQConfig] Forzando declaración inmediata de infraestructura...");
            rabbitAdmin.initialize();
            log.info("[RabbitMQConfig] Infraestructura declarada exitosamente.");
        };
    }
}
