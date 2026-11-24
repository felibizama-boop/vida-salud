package com.vidasalud.listeners;

import com.vidasalud.dto.event.AuditEventDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Consumidor Kafka del tópico {@code audit.timeline}.
 * <p>
 * Pertenece al {@code consumer group} de auditoría. Su concurrencia (3) coincide con
 * las particiones del tópico, lo que permite el balanceo entre instancias del grupo.
 */
@Component
public class AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);

    private final Validator validator;

    @Value("${vidasalud.kafka.consumer-groups.audit}")
    private String groupId;

    public AuditEventConsumer(Validator validator) {
        this.validator = validator;
    }

    @KafkaListener(
            topics = "${vidasalud.kafka.topics.audit.name}",
            groupId = "${vidasalud.kafka.consumer-groups.audit}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(AuditEventDTO event, ConsumerRecord<String, Object> record, Acknowledgment ack) {
        log.info("[AuditEventConsumer] group='{}' tópico='{}' partición={} offset={} clave='{}' -> {}",
                groupId, record.topic(), record.partition(), record.offset(), record.key(), event);

        validate(event);

        try {
            log.info("[AuditEventConsumer] Registrando traza de auditoría: entidad={}:{}, acción={}, usuario={}.",
                    event.getEntityType(), event.getEntityId(), event.getAction(), event.getPerformedBy());
            ack.acknowledge();
            log.info("[AuditEventConsumer] Evento de auditoría '{}' confirmado (partición={}, offset={}).",
                    event.getEventId(), record.partition(), record.offset());
        } catch (Exception e) {
            log.error("[AuditEventConsumer] Error procesando auditoría '{}': {}", event.getEventId(), e.getMessage());
            throw e;
        }
    }

    private void validate(AuditEventDTO event) {
        Set<ConstraintViolation<AuditEventDTO>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            String detail = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
            log.warn("[AuditEventConsumer] Payload inválido en el tópico de auditoría -> {}", detail);
            throw new IllegalArgumentException("Payload inválido: " + detail);
        }
    }
}
