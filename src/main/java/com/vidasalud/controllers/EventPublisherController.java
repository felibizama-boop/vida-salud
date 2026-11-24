package com.vidasalud.controllers;

import com.vidasalud.dto.event.AppointmentEventDTO;
import com.vidasalud.dto.event.AuditEventDTO;
import com.vidasalud.producers.AppointmentEventProducer;
import com.vidasalud.producers.AuditEventProducer;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST para publicar eventos en los tópicos de Kafka.
 * <p>
 * Los endpoints quedan protegidos por {@code SecurityConfig} (JWT obligatorio bajo {@code /api/v1/**}).
 */
@RestController
@RequestMapping("/api/v1/events")
public class EventPublisherController {

    private static final Logger log = LoggerFactory.getLogger(EventPublisherController.class);

    private final AppointmentEventProducer appointmentEventProducer;
    private final AuditEventProducer auditEventProducer;

    public EventPublisherController(AppointmentEventProducer appointmentEventProducer,
                                    AuditEventProducer auditEventProducer) {
        this.appointmentEventProducer = appointmentEventProducer;
        this.auditEventProducer = auditEventProducer;
    }

    /**
     * Publica un evento de cita en {@code appointments.events}.
     */
    @PostMapping("/appointments")
    public ResponseEntity<String> publishAppointment(@Valid @RequestBody AppointmentEventDTO event) {
        log.info("[EventPublisherController] Solicitud de publicación de evento de cita: {}", event.getAppointmentId());
        appointmentEventProducer.publish(event);
        return new ResponseEntity<>("Evento de cita enviado al tópico appointments.events", HttpStatus.ACCEPTED);
    }

    /**
     * Publica un evento de auditoría en {@code audit.timeline}.
     */
    @PostMapping("/audit")
    public ResponseEntity<String> publishAudit(@Valid @RequestBody AuditEventDTO event) {
        log.info("[EventPublisherController] Solicitud de publicación de evento de auditoría: {}", event.getEntityId());
        auditEventProducer.publish(event);
        return new ResponseEntity<>("Evento de auditoría enviado al tópico audit.timeline", HttpStatus.ACCEPTED);
    }
}
