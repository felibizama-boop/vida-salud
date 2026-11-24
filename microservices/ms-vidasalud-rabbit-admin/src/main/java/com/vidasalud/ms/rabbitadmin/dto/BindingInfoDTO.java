package com.vidasalud.ms.rabbitadmin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * DTO de información de un binding, mapeado desde la Management API de RabbitMQ.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BindingInfoDTO(

        String source,

        String destination,

        @JsonProperty("destination_type")
        String destinationType,

        @JsonProperty("routing_key")
        String routingKey,

        String vhost,

        @JsonProperty("arguments")
        Map<String, Object> arguments
) {
}
