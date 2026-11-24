package com.vidasalud.producers;

import com.vidasalud.dto.event.AppointmentEventDTO;
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
 * Productor del tópico Kafka {@code appointments.events}.
 * <p>
 * Publica eventos de cita con clave igual a {@code appointmentId}, garantizando que
 * todos los eventos de una misma cita caigan en la misma partición (orden por clave).
 */
@Service
public class AppointmentEventProducer {

    private static final Logger log = LoggerFactory.getLogger(AppointmentEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topicName;

    public AppointmentEventProducer(KafkaTemplate<String, Object> kafkaTemplate,
                                    @Value("${vidasalud.kafka.topics.appointments.name}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    /**
     * Publica un evento de cita en Kafka de forma asíncrona.
     *
     * @param event evento a publicar (se completan {@code eventId} y {@code occurredAt} si faltan)
     */
    public void publish(AppointmentEventDTO event) {
        if (event.getEventId() == null || event.getEventId().isBlank()) {
            event.setEventId(UUID.randomUUID().toString());
        }
        if (event.getOccurredAt() == null) {
            event.setOccurredAt(LocalDateTime.now());
        }
        if (event.getSource() == null) {
            event.setSource("api-rest");
        }

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topicName, event.getAppointmentId(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[AppointmentEventProducer] Error publicando evento '{}' en '{}': {}",
                        event.getEventId(), topicName, ex.getMessage());
            } else if (result != null) {
                log.info("[AppointmentEventProducer] Evento '{}' publicado en '{}' (partición={}, offset={})",
                        event.getEventId(), topicName,
                        result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }
        });
    }
}
