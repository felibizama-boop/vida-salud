package com.vidasalud.ms.rabbitadmin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * DTO de información de un exchange, mapeado desde la Management API de RabbitMQ.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExchangeInfoDTO(

        String name,

        String vhost,

        String type,

        Boolean durable,

        @JsonProperty("auto_delete")
        Boolean autoDelete,

        Boolean internal,

        @JsonProperty("arguments")
        Map<String, Object> arguments
) {
}
