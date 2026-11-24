package com.vidasalud.listeners;

import com.vidasalud.dto.event.AuditEventDTO;
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
class AuditEventConsumerTest {

    @Mock
    private Validator validator;

    @Mock
    private Acknowledgment acknowledgment;

    private AuditEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new AuditEventConsumer(validator);
        ReflectionTestUtils.setField(consumer, "groupId", "vidasalud-audit-group");
    }

    private AuditEventDTO eventoValido() {
        return new AuditEventDTO("APPOINTMENT", "apt-1", "CREATED", "user@vidasalud.cl", "detalle");
    }

    private ConsumerRecord<String, Object> registro(AuditEventDTO event) {
        return new ConsumerRecord<>("audit.timeline", 2, 42L, event.getEntityId(), event);
    }

    @Test
    void procesaAuditoriaValidaYEnviaAck() {
        when(validator.validate(any(AuditEventDTO.class))).thenReturn(Collections.emptySet());
        AuditEventDTO event = eventoValido();

        consumer.consume(event, registro(event), acknowledgment);

        verify(acknowledgment).acknowledge();
    }

    @Test
    void payloadInvalidoNoConfirmaElOffset() {
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("action");
        @SuppressWarnings("unchecked")
        ConstraintViolation<AuditEventDTO> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("obligatorio");
        when(validator.validate(any(AuditEventDTO.class))).thenReturn(Set.of(violation));

        AuditEventDTO event = eventoValido();
        event.setOccurredAt(LocalDateTime.now());

        assertThatThrownBy(() -> consumer.consume(event, registro(event), acknowledgment))
                .isInstanceOf(IllegalArgumentException.class);

        verify(acknowledgment, never()).acknowledge();
    }
}
