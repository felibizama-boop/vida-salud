package com.vidasalud.producers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidasalud.dto.NotificationRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Productor RabbitMQ de VidaSalud.
 * <p>
 * Enruta el mensaje a la cola correspondiente según el tipo solicitado:
 * <ul>
 *   <li>{@code NOTIFICACION} -> exchange directo, routing key de notificaciones.</li>
 *   <li>{@code TICKET}       -> exchange directo, routing key de tickets.</li>
 *   <li>{@code DOCUMENTO}    -> exchange topic, routing key {@code documents.alta}.</li>
 * </ul>
 * El payload se publica como JSON para que los consumidores lo registren y, si
 * corresponde, lo deriven a su DLQ.
 */
@Service
public class NotificationProducer {

    private static final Logger log = LoggerFactory.getLogger(NotificationProducer.class);

    /** Routing key concreta que satisface el binding del exchange topic ({@code documents.#}). */
    private static final String DOCUMENTOS_ROUTING_KEY = "documents.alta";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${vidasalud.rabbitmq.exchanges.direct}")
    private String directExchange;

    @Value("${vidasalud.rabbitmq.exchanges.topic}")
    private String topicExchange;

    @Value("${vidasalud.rabbitmq.queues.notificaciones.routing-key}")
    private String notificacionesRoutingKey;

    @Value("${vidasalud.rabbitmq.queues.tickets.routing-key}")
    private String ticketsRoutingKey;

    public NotificationProducer(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publica la solicitud en el exchange y routing key que correspondan a su tipo.
     *
     * @param dto solicitud de mensaje a publicar
     */
    public void publish(NotificationRequestDTO dto) {
        String exchange;
        String routingKey;

        switch (dto.getType().toUpperCase()) {
            case "TICKET" -> {
                exchange = directExchange;
                routingKey = ticketsRoutingKey;
            }
            case "DOCUMENTO" -> {
                exchange = topicExchange;
                routingKey = DOCUMENTOS_ROUTING_KEY;
            }
            default -> {
                exchange = directExchange;
                routingKey = notificacionesRoutingKey;
            }
        }

        String payload = toJson(dto);
        rabbitTemplate.convertAndSend(exchange, routingKey, payload);
        log.info("[NotificationProducer] Mensaje publicado (exchange='{}', routingKey='{}'): {}",
                exchange, routingKey, payload);
    }

    private String toJson(NotificationRequestDTO dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar la solicitud de notificación", e);
        }
    }
}
