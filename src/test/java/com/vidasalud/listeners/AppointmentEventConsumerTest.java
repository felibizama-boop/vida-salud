package com.vidasalud.listeners;

import com.vidasalud.dto.event.AppointmentEventDTO;
import com.vidasalud.producers.AuditEventProducer;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentEventConsumerTest {

    @Mock
    private Validator validator;

    @Mock
    private AuditEventProducer auditEventProducer;

    @Mock
    private Acknowledgment acknowledgment;

    private AppointmentEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new AppointmentEventConsumer(validator, auditEventProducer);
        ReflectionTestUtils.setField(consumer, "groupId", "vidasalud-appointments-group");
    }

    private AppointmentEventDTO eventoValido() {
        return new AppointmentEventDTO("apt-1", "pac-1", "pro-1", "CREATED", "AGENDADA",
                LocalDateTime.now().plusDays(1));
    }

    private ConsumerRecord<String, Object> registro(AppointmentEventDTO event) {
        return new ConsumerRecord<>("appointments.events", 0, 15L, event.getAppointmentId(), event);
    }

    @Test
    void procesaEventoValidoEnviaAckYGeneraAuditoria() {
        when(validator.validate(any(AppointmentEventDTO.class))).thenReturn(Collections.emptySet());
        AppointmentEventDTO event = eventoValido();

        consumer.consume(event, registro(event), acknowledgment);

        verify(acknowledgment).acknowledge();
        verify(auditEventProducer).publish(any());
    }

    @Test
    void payloadInvalidoLanzaExcepcionYNoConfirma() {
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("appointmentId");
        @SuppressWarnings("unchecked")
        ConstraintViolation<AppointmentEventDTO> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("obligatorio");
        when(validator.validate(any(AppointmentEventDTO.class))).thenReturn(Set.of(violation));

        AppointmentEventDTO event = eventoValido();

        assertThatThrownBy(() -> consumer.consume(event, registro(event), acknowledgment))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payload inválido");

        verify(acknowledgment, never()).acknowledge();
        verify(auditEventProducer, never()).publish(any());
    }

    @Test
    void errorDeNegocioPropagaExcepcionParaReintentoYDLT() {
        when(validator.validate(any(AppointmentEventDTO.class))).thenReturn(Collections.emptySet());
        AppointmentEventDTO event = eventoValido();
        event.setStatus("ERROR_SIMULADO");

        assertThatThrownBy(() -> consumer.consume(event, registro(event), acknowledgment))
                .isInstanceOf(IllegalStateException.class);

        verify(acknowledgment, never()).acknowledge();
    }
}
