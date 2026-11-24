package com.vidasalud.ms.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

/**
 * DTO de solicitud para la creación de una cola.
 */
public record QueueRequestDTO(

        @NotBlank(message = "El nombre de la cola es obligatorio")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
                message = "El nombre de la cola solo puede contener letras, números, puntos, guiones y guiones bajos")
        String name,

        @Pattern(regexp = "^[a-zA-Z0-9._-]*$",
                message = "El nombre del exchange de dead-letter solo puede contener letras, números, puntos, guiones y guiones bajos")
        String deadLetterExchange,

        @Pattern(regexp = "^[a-zA-Z0-9._-]*$",
                message = "La routing key de dead-letter solo puede contener letras, números, puntos, guiones y guiones bajos")
        String deadLetterRoutingKey,

        Boolean durable,

        Boolean exclusive,

        Boolean autoDelete,

        Map<String, Object> arguments
) {

    public QueueRequestDTO {
        if (durable == null) {
            durable = true;
        }
        if (exclusive == null) {
            exclusive = false;
        }
        if (autoDelete == null) {
            autoDelete = false;
        }
    }
}
