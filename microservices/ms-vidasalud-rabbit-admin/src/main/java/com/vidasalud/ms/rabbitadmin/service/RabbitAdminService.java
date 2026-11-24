package com.vidasalud.ms.rabbitadmin.service;

import com.vidasalud.ms.rabbitadmin.dto.BindingInfoDTO;
import com.vidasalud.ms.rabbitadmin.dto.BindingRequestDTO;
import com.vidasalud.ms.rabbitadmin.dto.ExchangeInfoDTO;
import com.vidasalud.ms.rabbitadmin.dto.ExchangeRequestDTO;
import com.vidasalud.ms.rabbitadmin.dto.QueueInfoDTO;
import com.vidasalud.ms.rabbitadmin.dto.QueueRequestDTO;
import com.vidasalud.ms.rabbitadmin.exception.RabbitAdminException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio administrador de infraestructura RabbitMQ.
 * Encapsula las operaciones de creación/eliminación de colas, exchanges y bindings.
 */
@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);

    private final AmqpAdmin amqpAdmin;
    private final RabbitManagementClient managementClient;

    public RabbitAdminService(AmqpAdmin amqpAdmin, RabbitManagementClient managementClient) {
        this.amqpAdmin = amqpAdmin;
        this.managementClient = managementClient;
    }

    // ---------------------------------------------------------------------
    // Colas
    // ---------------------------------------------------------------------

    public void createQueue(QueueRequestDTO dto) {
        log.info("[RabbitAdminService] Creando cola: {}", dto.name());
        try {
            QueueBuilder builder = Boolean.TRUE.equals(dto.durable())
                    ? QueueBuilder.durable(dto.name())
                    : QueueBuilder.nonDurable(dto.name());

            if (Boolean.TRUE.equals(dto.exclusive())) {
                builder.exclusive();
            }
            if (Boolean.TRUE.equals(dto.autoDelete())) {
                builder.autoDelete();
            }
            if (dto.deadLetterExchange() != null && !dto.deadLetterExchange().isBlank()) {
                builder.deadLetterExchange(dto.deadLetterExchange());
            }
            if (dto.deadLetterRoutingKey() != null && !dto.deadLetterRoutingKey().isBlank()) {
                builder.deadLetterRoutingKey(dto.deadLetterRoutingKey());
            }
            if (dto.arguments() != null && !dto.arguments().isEmpty()) {
                builder.withArguments(dto.arguments());
            }

            Queue queue = builder.build();
            amqpAdmin.declareQueue(queue);
        } catch (AmqpException ex) {
            throw new RabbitAdminException("Error al crear la cola '" + dto.name() + "'", HttpStatus.CONFLICT, ex);
        }
    }

    public void deleteQueue(String name) {
        log.info("[RabbitAdminService] Eliminando cola: {}", name);
        try {
            amqpAdmin.deleteQueue(name);
        } catch (AmqpException ex) {
            throw new RabbitAdminException("Error al eliminar la cola '" + name + "'", HttpStatus.CONFLICT, ex);
        }
    }

    public void purgeQueue(String name) {
        log.info("[RabbitAdminService] Purgando cola: {}", name);
        try {
            amqpAdmin.purgeQueue(name, false);
        } catch (AmqpException ex) {
            throw new RabbitAdminException("Error al purgar la cola '" + name + "'", HttpStatus.CONFLICT, ex);
        }
    }

    public List<QueueInfoDTO> listQueues() {
        log.info("[RabbitAdminService] Listando colas");
        return managementClient.listQueues();
    }

    // ---------------------------------------------------------------------
    // Exchanges
    // ---------------------------------------------------------------------

    public void createExchange(ExchangeRequestDTO dto) {
        log.info("[RabbitAdminService] Creando exchange '{}' de tipo {}", dto.name(), dto.type());
        try {
            ExchangeBuilder builder = switch (dto.type()) {
                case DIRECT -> ExchangeBuilder.directExchange(dto.name());
                case TOPIC -> ExchangeBuilder.topicExchange(dto.name());
                case FANOUT -> ExchangeBuilder.fanoutExchange(dto.name());
                case HEADERS -> ExchangeBuilder.headersExchange(dto.name());
            };
            builder.durable(Boolean.TRUE.equals(dto.durable()));
            if (Boolean.TRUE.equals(dto.autoDelete())) {
                builder.autoDelete();
            }
            Exchange exchange = builder.build();
            amqpAdmin.declareExchange(exchange);
        } catch (AmqpException ex) {
            throw new RabbitAdminException("Error al crear el exchange '" + dto.name() + "'", HttpStatus.CONFLICT, ex);
        }
    }

    public void deleteExchange(String name) {
        log.info("[RabbitAdminService] Eliminando exchange: {}", name);
        try {
            amqpAdmin.deleteExchange(name);
        } catch (AmqpException ex) {
            throw new RabbitAdminException("Error al eliminar el exchange '" + name + "'", HttpStatus.CONFLICT, ex);
        }
    }

    public List<ExchangeInfoDTO> listExchanges() {
        log.info("[RabbitAdminService] Listando exchanges");
        return managementClient.listExchanges();
    }

    // ---------------------------------------------------------------------
    // Bindings
    // ---------------------------------------------------------------------

    public void createBinding(BindingRequestDTO dto) {
        log.info("[RabbitAdminService] Creando binding cola '{}' -> exchange '{}' con routing key '{}'",
                dto.queueName(), dto.exchangeName(), dto.routingKey());
        try {
            Binding binding = new Binding(dto.queueName(), Binding.DestinationType.QUEUE,
                    dto.exchangeName(), dto.routingKey(), null);
            amqpAdmin.declareBinding(binding);
        } catch (AmqpException ex) {
            throw new RabbitAdminException("Error al crear el binding entre '" + dto.exchangeName()
                    + "' y '" + dto.queueName() + "'", HttpStatus.CONFLICT, ex);
        }
    }

    public void deleteBinding(BindingRequestDTO dto) {
        log.info("[RabbitAdminService] Eliminando binding cola '{}' -> exchange '{}' con routing key '{}'",
                dto.queueName(), dto.exchangeName(), dto.routingKey());
        try {
            Binding binding = new Binding(dto.queueName(), Binding.DestinationType.QUEUE,
                    dto.exchangeName(), dto.routingKey(), null);
            amqpAdmin.removeBinding(binding);
        } catch (AmqpException ex) {
            throw new RabbitAdminException("Error al eliminar el binding entre '" + dto.exchangeName()
                    + "' y '" + dto.queueName() + "'", HttpStatus.CONFLICT, ex);
        }
    }

    public List<BindingInfoDTO> listBindings() {
        log.info("[RabbitAdminService] Listando bindings");
        return managementClient.listBindings();
    }
}
