package com.vidasalud.ms.rabbitadmin.domain;

/**
 * Tipos de exchange soportados por RabbitMQ.
 * El tipo {@code DIRECT} se usa también para el exchange de DLX ("dead").
 */
public enum ExchangeType {
    DIRECT,
    TOPIC,
    FANOUT,
    HEADERS
}
