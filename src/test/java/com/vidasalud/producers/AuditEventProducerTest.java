package com.vidasalud.producers;

import com.vidasalud.dto.event.AuditEventDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditEventProducerTest {

    private static final String TOPIC = "audit.timeline";

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private AuditEventProducer producer;

    @BeforeEach
    void setUp() {
        producer = new AuditEventProducer(kafkaTemplate, TOPIC);
        CompletableFuture<SendResult<String, Object>> completed = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(completed);
    }

    @Test
    void publicaEventoDeAuditoriaConClaveDeEntidad() {
        AuditEventDTO event = new AuditEventDTO("APPOINTMENT", "apt-999", "CANCELLED", "user@vidasalud.cl", "motivo=test");

        producer.publish(event);

        verify(kafkaTemplate).send(eq(TOPIC), eq("apt-999"), eq(event));
        assertThat(event.getEventId()).isNotBlank();
        assertThat(event.getOccurredAt()).isNotNull();
    }
}
