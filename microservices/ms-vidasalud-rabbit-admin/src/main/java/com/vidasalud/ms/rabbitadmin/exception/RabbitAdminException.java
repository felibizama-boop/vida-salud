package com.vidasalud.ms.rabbitadmin.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción de negocio para errores del administrador de RabbitMQ.
 * Transporta el {@link HttpStatus} que debe devolverse al cliente.
 */
public class RabbitAdminException extends RuntimeException {

    private final HttpStatus status;

    public RabbitAdminException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public RabbitAdminException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
