package com.vidasalud.ms.rabbitadmin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * DTO de información de una cola, mapeado desde la Management API de RabbitMQ.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record QueueInfoDTO(

        String name,

        String vhost,

        @JsonProperty("messages")
        Long messages,

        Long consumers,

        Boolean durable,

        @JsonProperty("auto_delete")
        Boolean autoDelete,

        @JsonProperty("arguments")
        Map<String, Object> arguments
) {
}
