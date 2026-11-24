package com.vidasalud.ms.kafkaadmin.dto;

import jakarta.validation.constraints.Min;

import java.util.Map;

/**
 * DTO de entrada para actualizar la configuración de un tópico existente.
 * Todos los campos son opcionales: solo se aplican los que vengan informados.
 */
public record TopicConfigUpdateDTO(

        @Min(value = 1, message = "El número de particiones debe ser mayor o igual a 1")
        Integer partitions,

        String cleanupPolicy,

        Long retentionMs,

        @Min(value = 1, message = "El número de réplicas in-sync debe ser mayor o igual a 1")
        Integer minInsyncReplicas,

        Map<String, String> extraConfigs
) {
}
