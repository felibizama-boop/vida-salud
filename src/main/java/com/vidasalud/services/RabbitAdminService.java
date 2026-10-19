package com.vidasalud.services;

import com.vidasalud.dto.QueueRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servicio para la gestión dinámica de infraestructura RabbitMQ.
 */
@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);
    private final AmqpAdmin amqpAdmin;

    @Autowired
    public RabbitAdminService(AmqpAdmin amqpAdmin) {
        this.amqpAdmin = amqpAdmin;
    }

    /**
     * Crea una cola y su binding opcional.
     */
    public void createQueue(QueueRequestDTO dto) {
        log.info("[RabbitAdminService] Declarando cola: {}", dto.getQueueName());
        Queue queue = new Queue(dto.getQueueName(), true, false, false);
        amqpAdmin.declareQueue(queue);

        if (dto.getExchangeName() != null && !dto.getExchangeName().isEmpty()) {
            log.info("[RabbitAdminService] Declarando binding con exchange: {}", dto.getExchangeName());
            DirectExchange exchange = new DirectExchange(dto.getExchangeName());
            amqpAdmin.declareExchange(exchange);
            
            Binding binding = BindingBuilder.bind(queue)
                    .to(exchange)
                    .with(dto.getRoutingKey() != null ? dto.getRoutingKey() : "");
            
            amqpAdmin.declareBinding(binding);
        }
    }

    /**
     * Elimina una cola por su nombre.
     */
    public void deleteQueue(String queueName) {
        log.info("[RabbitAdminService] Eliminando cola: {}", queueName);
        amqpAdmin.deleteQueue(queueName);
    }
}
