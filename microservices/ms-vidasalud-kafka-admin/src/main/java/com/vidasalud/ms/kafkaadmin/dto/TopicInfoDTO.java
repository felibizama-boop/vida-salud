package com.vidasalud.ms.kafkaadmin.dto;

import java.util.Map;

/**
 * DTO de salida con la información de un tópico.
 */
public record TopicInfoDTO(
        String name,
        int partitions,
        int replicationFactor,
        boolean internal,
        Map<String, String> topicConfigs,
        Long retentionMs,
        String cleanupPolicy
) {
}
