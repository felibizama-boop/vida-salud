package com.vidasalud.ms.rabbitadmin.exception;

import com.vidasalud.ms.rabbitadmin.dto.ApiErrorDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejo global de errores del microservicio.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDTO> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                     ServletWebRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "Errores de validación en la petición", request, details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorDTO> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                     ServletWebRequest request) {
        log.warn("Petición ilegible: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "El cuerpo de la petición es inválido o no se pudo leer",
                request, null);
    }

    @ExceptionHandler(RabbitAdminException.class)
    public ResponseEntity<ApiErrorDTO> handleRabbitAdmin(RabbitAdminException ex, ServletWebRequest request) {
        log.warn("Error de RabbitAdmin: {}", ex.getMessage());
        return build(ex.getStatus(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<ApiErrorDTO> handleAmqp(AmqpException ex, ServletWebRequest request) {
        log.warn("Error de AMQP: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDTO> handleGeneric(Exception ex, ServletWebRequest request) {
        log.error("Error inesperado", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", request, null);
    }

    private ResponseEntity<ApiErrorDTO> build(HttpStatus status, String message, ServletWebRequest request,
                                              Map<String, String> details) {
        ApiErrorDTO error = new ApiErrorDTO(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequest().getRequestURI(),
                details);
        return ResponseEntity.status(status).body(error);
    }
}
