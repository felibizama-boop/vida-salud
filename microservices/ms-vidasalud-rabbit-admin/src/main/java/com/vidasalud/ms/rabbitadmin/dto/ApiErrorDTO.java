package com.vidasalud.ms.rabbitadmin.dto;

import java.time.Instant;
import java.util.Map;

/**
 * DTO de error global devuelto por la API.
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
