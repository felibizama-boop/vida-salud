package com.vidasalud.listeners;

import com.vidasalud.dto.event.AppointmentEventDTO;
import com.vidasalud.dto.event.AuditEventDTO;
import com.vidasalud.producers.AuditEventProducer;
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
 * Consumidor Kafka del tópico {@code appointments.events}.
 * <p>
 * Forma parte del {@code consumer group} de citas, con ack manual y validación de payload.
 * Tras procesar un evento de cita, emite un evento de auditoría en {@code audit.timeline}.
 * <ul>
 *   <li>Payload inválido -> {@link IllegalArgumentException} (no reintentable, va directo a la DLT).</li>
 *   <li>Error transitorio -> reintentos con backoff y, agotados, va a la DLT.</li>
 * </ul>
 */
@Component
public class AppointmentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AppointmentEventConsumer.class);

    /** Estado de negocio artificial que fuerza el fallo para demostrar reintentos + DLT. */
    private static final String FORCE_FAILURE_STATUS = "ERROR_SIMULADO";

    private final Validator validator;
    private final AuditEventProducer auditEventProducer;

    @Value("${vidasalud.kafka.consumer-groups.appointments}")
    private String groupId;

    public AppointmentEventConsumer(Validator validator, AuditEventProducer auditEventProducer) {
        this.validator = validator;
        this.auditEventProducer = auditEventProducer;
    }

    @KafkaListener(
            topics = "${vidasalud.kafka.topics.appointments.name}",
            groupId = "${vidasalud.kafka.consumer-groups.appointments}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(AppointmentEventDTO event, ConsumerRecord<String, Object> record, Acknowledgment ack) {
        log.info("[AppointmentEventConsumer] group='{}' tópico='{}' partición={} offset={} clave='{}' -> {}",
                groupId, record.topic(), record.partition(), record.offset(), record.key(), event);

        validate(event);

        try {
            process(event);
            ack.acknowledge();
            log.info("[AppointmentEventConsumer] Evento '{}' procesado correctamente. ACK manual enviado (partición={}, offset={}).",
                    event.getEventId(), record.partition(), record.offset());
        } catch (Exception e) {
            log.error("[AppointmentEventConsumer] Error procesando evento '{}' (status='{}'): {}",
                    event.getEventId(), event.getStatus(), e.getMessage());
            throw e;
        }
    }

    /**
     * Lógica de negocio del consumo de citas (simulada).
     */
    private void process(AppointmentEventDTO event) {
        if (FORCE_FAILURE_STATUS.equalsIgnoreCase(event.getStatus())) {
            throw new IllegalStateException("Fallo transitorio simulado para el evento " + event.getEventId());
        }
        log.info("[AppointmentEventConsumer] Aplicando cambio de estado '{}' a la cita {}.",
                event.getEventType(), event.getAppointmentId());

        // Encadena la traza de auditoría en el tópico audit.timeline.
        AuditEventDTO audit = new AuditEventDTO(
                "APPOINTMENT",
                event.getAppointmentId(),
                event.getEventType(),
                event.getSource() != null ? event.getSource() : "appointments-consumer",
                "status=" + event.getStatus()
        );
        auditEventProducer.publish(audit);
    }

    /**
     * Valida el payload recibido aplicando las restricciones Jakarta del DTO.
     *
     * @param event evento a validar
     * @throws IllegalArgumentException si el payload no cumple las restricciones
     */
    private void validate(AppointmentEventDTO event) {
        Set<ConstraintViolation<AppointmentEventDTO>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            String detail = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
            log.warn("[AppointmentEventConsumer] Payload inválido en el tópico de citas -> {}", detail);
            throw new IllegalArgumentException("Payload inválido: " + detail);
        }
    }
}
