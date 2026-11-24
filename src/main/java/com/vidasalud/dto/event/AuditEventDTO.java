package com.vidasalud.dto.event;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/**
 * Evento de auditoría publicado en el tópico Kafka {@code audit.timeline}.
 * <p>
 * Se utiliza un tópico con {@code cleanup.policy=compact,delete} porque representa una
 * línea de tiempo de auditoría donde interesa conservar el último estado por clave
 * ({@code entityId}) además de aplicar retención por tiempo.
 */
public class AuditEventDTO {

    /** Identificador único del evento (si no se informa, el productor lo genera). */
    private String eventId;

    @NotBlank(message = "El tipo de entidad auditada es obligatorio")
    private String entityType;

    @NotBlank(message = "El identificador de la entidad auditada es obligatorio")
    private String entityId;

    @NotBlank(message = "La acción auditada es obligatoria")
    private String action;

    @NotBlank(message = "El usuario que realiza la acción es obligatorio")
    private String performedBy;

    /** Detalle libre del cambio (JSON, descripción, etc.). */
    private String details;

    /** Marca temporal del evento (si no se informa, el productor la genera). */
    private LocalDateTime occurredAt;

    public AuditEventDTO() {
    }

    public AuditEventDTO(String entityType, String entityId, String action, String performedBy, String details) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.performedBy = performedBy;
        this.details = details;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    @Override
    public String toString() {
        return "AuditEventDTO{eventId='" + eventId + "', entityType='" + entityType + "', entityId='" + entityId
                + "', action='" + action + "', performedBy='" + performedBy + "', occurredAt=" + occurredAt + "}";
    }
}
