package com.vidasalud.ms.rabbitadmin.service;

import com.vidasalud.ms.rabbitadmin.domain.ExchangeType;
import com.vidasalud.ms.rabbitadmin.dto.ExchangeRequestDTO;
import com.vidasalud.ms.rabbitadmin.dto.QueueRequestDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests unitarios de {@link RabbitAdminService} con Mockito. No levanta contexto de Spring.
 */
@ExtendWith(MockitoExtension.class)
class RabbitAdminServiceTest {

    @Mock
    private AmqpAdmin amqpAdmin;

    @Mock
    private RabbitManagementClient managementClient;

    @InjectMocks
    private RabbitAdminService rabbitAdminService;

    @Test
    void createQueueLlamaDeclareQueueUnaVez() {
        QueueRequestDTO dto = new QueueRequestDTO("mi-cola", null, null, true, false, false, null);

        rabbitAdminService.createQueue(dto);

        verify(amqpAdmin, times(1)).declareQueue(any(Queue.class));
    }

    @Test
    void deleteQueueLlamaDeleteQueueConElNombre() {
        rabbitAdminService.deleteQueue("mi-cola");

        verify(amqpAdmin, times(1)).deleteQueue("mi-cola");
    }

    @Test
    void createExchangeDirectoLlamaDeclareExchange() {
        ExchangeRequestDTO dto = new ExchangeRequestDTO("mi-exchange", ExchangeType.DIRECT, true, false);

        rabbitAdminService.createExchange(dto);

        verify(amqpAdmin, times(1)).declareExchange(any(Exchange.class));
    }
}
