package com.vidasalud.ms.kafkaadmin.exception;

/**
 * Excepción lanzada cuando falla una operación administrativa contra Kafka.
 */
public class KafkaAdminOperationException extends RuntimeException {

    public KafkaAdminOperationException(String message) {
        super(message);
    }

    public KafkaAdminOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
