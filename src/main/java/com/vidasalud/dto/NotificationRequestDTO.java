package com.vidasalud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Solicitud para publicar un mensaje en la topología RabbitMQ existente.
 * <p>
 * El campo {@code type} determina el exchange y la routing key utilizados
 * (notificaciones y tickets vía exchange directo, documentos vía exchange topic).
 * El campo {@code simulateFailure} permite demostrar el enrutamiento a la DLQ.
 */
public class NotificationRequestDTO {

    @NotBlank(message = "El tipo de mensaje es obligatorio")
    @Pattern(regexp = "NOTIFICACION|TICKET|DOCUMENTO",
            message = "El tipo debe ser NOTIFICACION, TICKET o DOCUMENTO")
    private String type;

    @NotBlank(message = "El destinatario es obligatorio")
    private String recipient;

    @NotBlank(message = "El mensaje es obligatorio")
    private String message;

    /** Si es {@code true}, el consumidor simulará un error para enrutar el mensaje a la DLQ. */
    private boolean simulateFailure;

    public NotificationRequestDTO() {
    }

    public NotificationRequestDTO(String type, String recipient, String message, boolean simulateFailure) {
        this.type = type;
        this.recipient = recipient;
        this.message = message;
        this.simulateFailure = simulateFailure;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSimulateFailure() {
        return simulateFailure;
    }

    public void setSimulateFailure(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }
}
