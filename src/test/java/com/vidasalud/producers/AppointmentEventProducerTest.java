package com.vidasalud.producers;

import com.vidasalud.dto.event.AppointmentEventDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentEventProducerTest {

    private static final String TOPIC = "appointments.events";

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private AppointmentEventProducer producer;

    @BeforeEach
    void setUp() {
        producer = new AppointmentEventProducer(kafkaTemplate, TOPIC);
        CompletableFuture<SendResult<String, Object>> completed = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(completed);
    }

    @Test
    void publicaEventoConClaveDeCitaYGeneraIdentificadoresAusentes() {
        AppointmentEventDTO event = new AppointmentEventDTO(
                "apt-123", "pac-1", "pro-1", "CREATED", "AGENDADA",
                LocalDateTime.now().plusDays(2));

        producer.publish(event);

        verify(kafkaTemplate).send(eq(TOPIC), eq("apt-123"), eq(event));
        assertThat(event.getEventId()).isNotBlank();
        assertThat(event.getOccurredAt()).isNotNull();
        assertThat(event.getSource()).isEqualTo("api-rest");
    }
}
