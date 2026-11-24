package com.vidasalud.ms.kafkaadmin.dto;

import java.util.List;

/**
 * DTO de salida con la información de un consumer group.
 */
public record ConsumerGroupInfoDTO(
        String groupId,
        String state,
        boolean simpleConsumerGroup,
        int memberCount,
        List<MemberInfoDTO> members,
        List<String> topics,
        Long totalLag
) {

    /**
     * Información de un miembro (consumidor) dentro de un consumer group.
     */
    public record MemberInfoDTO(
            String memberId,
            String clientId,
            String host,
            List<String> assignment
    ) {
    }
}
