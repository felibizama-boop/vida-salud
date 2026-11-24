package com.vidasalud.ms.kafkaadmin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

/**
 * DTO de entrada para la creación de un tópico.
 */
public record TopicRequestDTO(

        @NotBlank(message = "El nombre del tópico es obligatorio")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre del tópico contiene caracteres inválidos")
        String name,

        @NotNull(message = "El número de particiones es obligatorio")
        @Min(value = 1, message = "El número de particiones debe ser mayor o igual a 1")
        Integer partitions,

        @NotNull(message = "El factor de replicación es obligatorio")
        @Min(value = 1, message = "El factor de replicación debe ser mayor o igual a 1")
        Short replicationFactor,

        String cleanupPolicy,

        Long retentionMs,

        Integer minInsyncReplicas,

        Map<String, String> extraConfigs
) {

    public TopicRequestDTO {
        if (cleanupPolicy == null) {
            cleanupPolicy = "delete";
        }
        if (retentionMs == null) {
            retentionMs = 604800000L;
        }
        if (minInsyncReplicas == null) {
            minInsyncReplicas = 1;
        }
    }
}
