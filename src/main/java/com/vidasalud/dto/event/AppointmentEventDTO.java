package com.vidasalud.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

/**
 * Evento de dominio publicado en el tópico Kafka {@code appointments.events}.
 * <p>
 * Representa un cambio de estado en una cita médica (agendamiento, reprogramación,
 * cancelación, finalización). Se serializa como JSON en el productor y se valida
 * tanto en el borde REST como en el consumidor.
 */
public class AppointmentEventDTO {

    /** Identificador único del evento (si no se informa, el productor lo genera). */
    private String eventId;

    @NotBlank(message = "El identificador de la cita es obligatorio")
    private String appointmentId;

    @NotBlank(message = "El identificador del paciente es obligatorio")
    private String patientId;

    @NotBlank(message = "El identificador del profesional es obligatorio")
    private String practitionerId;

    @NotBlank(message = "El tipo de evento es obligatorio")
    @Pattern(regexp = "CREATED|UPDATED|RESCHEDULED|CANCELLED|COMPLETED",
            message = "El tipo de evento debe ser CREATED, UPDATED, RESCHEDULED, CANCELLED o COMPLETED")
    private String eventType;

    @NotBlank(message = "El estado de la cita es obligatorio")
    private String status;

    @NotNull(message = "La fecha/hora agendada es obligatoria")
    private LocalDateTime scheduledAt;

    /** Marca temporal en la que ocurrió el evento (si no se informa, el productor la genera). */
    private LocalDateTime occurredAt;

    /** Origen del evento (por ejemplo {@code agenda-web} o {@code mobile}). */
    private String source;

    public AppointmentEventDTO() {
    }

    public AppointmentEventDTO(String appointmentId, String patientId, String practitionerId,
                               String eventType, String status, LocalDateTime scheduledAt) {
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.practitionerId = practitionerId;
        this.eventType = eventType;
        this.status = status;
        this.scheduledAt = scheduledAt;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getPractitionerId() {
        return practitionerId;
    }

    public void setPractitionerId(String practitionerId) {
        this.practitionerId = practitionerId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return "AppointmentEventDTO{eventId='" + eventId + "', appointmentId='" + appointmentId
                + "', eventType='" + eventType + "', status='" + status + "', occurredAt=" + occurredAt + "}";
    }
}
