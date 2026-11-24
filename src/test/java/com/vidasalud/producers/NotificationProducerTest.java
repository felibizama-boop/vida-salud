package com.vidasalud.producers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidasalud.dto.NotificationRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationProducerTest {

    private static final String DIRECT = "vidasalud.direct.exchange";
    private static final String TOPIC = "vidasalud.topic.exchange";

    @Mock
    private RabbitTemplate rabbitTemplate;

    private NotificationProducer producer;

    @BeforeEach
    void setUp() {
        producer = new NotificationProducer(rabbitTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(producer, "directExchange", DIRECT);
        ReflectionTestUtils.setField(producer, "topicExchange", TOPIC);
        ReflectionTestUtils.setField(producer, "notificacionesRoutingKey", "notificaciones.key");
        ReflectionTestUtils.setField(producer, "ticketsRoutingKey", "tickets.key");
    }

    @Test
    void enrutaNotificacionPorExchangeDirecto() {
        producer.publish(new NotificationRequestDTO("NOTIFICACION", "paciente@vidasalud.cl", "Recordatorio", false));

        verify(rabbitTemplate).convertAndSend(eq(DIRECT), eq("notificaciones.key"), anyString());
    }

    @Test
    void enrutaTicketPorExchangeDirecto() {
        producer.publish(new NotificationRequestDTO("TICKET", "mesa@vidasalud.cl", "Soporte", false));

        verify(rabbitTemplate).convertAndSend(eq(DIRECT), eq("tickets.key"), anyString());
    }

    @Test
    void enrutaDocumentoPorExchangeTopic() {
        producer.publish(new NotificationRequestDTO("DOCUMENTO", "archivo@vidasalud.cl", "Receta", false));

        verify(rabbitTemplate).convertAndSend(eq(TOPIC), eq("documents.alta"), anyString());
    }
}
