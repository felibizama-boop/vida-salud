package com.vidasalud.controllers;

import com.vidasalud.dto.NotificationRequestDTO;
import com.vidasalud.producers.NotificationProducer;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST para publicar mensajes en la topología RabbitMQ.
 * <p>
 * Permite demostrar el flujo completo: publicación -> consumo con ack manual ->
 * derivación a la Dead Letter Queue cuando el consumidor falla
 * ({@code simulateFailure = true}).
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationProducer notificationProducer;

    public NotificationController(NotificationProducer notificationProducer) {
        this.notificationProducer = notificationProducer;
    }

    @PostMapping
    public ResponseEntity<String> publish(@Valid @RequestBody NotificationRequestDTO request) {
        log.info("[NotificationController] Publicando mensaje tipo '{}' para '{}'", request.getType(), request.getRecipient());
        notificationProducer.publish(request);
        return new ResponseEntity<>(
                "Mensaje publicado en RabbitMQ (tipo " + request.getType()
                        + (request.isSimulateFailure() ? ", fallo simulado -> se espera DLQ" : "") + ")",
                HttpStatus.ACCEPTED);
    }
}
