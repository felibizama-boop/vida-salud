package com.vidasalud.ms.rabbitadmin.dto;

import com.vidasalud.ms.rabbitadmin.domain.ExchangeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * DTO de solicitud para la creación de un exchange.
 */
public record ExchangeRequestDTO(

        @NotBlank(message = "El nombre del exchange es obligatorio")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
                message = "El nombre del exchange solo puede contener letras, números, puntos, guiones y guiones bajos")
        String name,

        @NotNull(message = "El tipo de exchange es obligatorio")
        ExchangeType type,

        Boolean durable,

        Boolean autoDelete
) {

    public ExchangeRequestDTO {
        if (durable == null) {
            durable = true;
        }
        if (autoDelete == null) {
            autoDelete = false;
        }
    }
}
