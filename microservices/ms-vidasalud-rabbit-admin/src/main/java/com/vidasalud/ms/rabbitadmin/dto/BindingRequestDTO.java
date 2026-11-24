package com.vidasalud.ms.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de solicitud para la creación o eliminación de un binding.
 */
public record BindingRequestDTO(

        @NotBlank(message = "El nombre de la cola es obligatorio")
        String queueName,

        @NotBlank(message = "El nombre del exchange es obligatorio")
        String exchangeName,

        @NotNull(message = "La routing key es obligatoria (puede ser una cadena vacía)")
        String routingKey
) {
}
