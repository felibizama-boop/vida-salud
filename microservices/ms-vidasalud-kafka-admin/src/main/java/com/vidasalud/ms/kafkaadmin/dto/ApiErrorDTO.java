package com.vidasalud.ms.kafkaadmin.dto;

import java.time.Instant;
import java.util.Map;

/**
 * DTO estándar para respuestas de error de la API.
 */
public record ApiErrorDTO(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> details
) {
}
