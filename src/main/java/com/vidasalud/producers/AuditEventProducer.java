package com.vidasalud.producers;

import com.vidasalud.dto.event.AuditEventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Productor del tópico Kafka {@code audit.timeline}.
 * <p>
 * Publica eventos de auditoría con clave igual a {@code entityId}. Al ser un tópico
 * compactado ({@code cleanup.policy=compact,delete}), conserva el último estado por clave.
 */
@Service
public class AuditEventProducer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topicName;

    public AuditEventProducer(KafkaTemplate<String, Object> kafkaTemplate,
                              @Value("${vidasalud.kafka.topics.audit.name}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    /**
     * Publica un evento de auditoría en Kafka de forma asíncrona.
     *
     * @param event evento a publicar (se completan {@code eventId} y {@code occurredAt} si faltan)
     */
    public void publish(AuditEventDTO event) {
        if (event.getEventId() == null || event.getEventId().isBlank()) {
            event.setEventId(UUID.randomUUID().toString());
        }
        if (event.getOccurredAt() == null) {
            event.setOccurredAt(LocalDateTime.now());
        }

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topicName, event.getEntityId(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[AuditEventProducer] Error publicando evento '{}' en '{}': {}",
                        event.getEventId(), topicName, ex.getMessage());
            } else if (result != null) {
                log.info("[AuditEventProducer] Evento '{}' publicado en '{}' (partición={}, offset={})",
                        event.getEventId(), topicName,
                        result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }
        });
    }
}
