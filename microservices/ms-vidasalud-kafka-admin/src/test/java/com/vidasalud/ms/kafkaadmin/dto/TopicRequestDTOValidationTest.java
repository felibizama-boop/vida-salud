package com.vidasalud.ms.kafkaadmin.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopicRequestDTOValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validDtoHasNoViolations() {
        TopicRequestDTO dto = new TopicRequestDTO("appointments.events", 3, (short) 3, null, null, null, null);
        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void compactConstructorAppliesDefaults() {
        TopicRequestDTO dto = new TopicRequestDTO("t", 1, (short) 1, null, null, null, null);
        assertEquals("delete", dto.cleanupPolicy());
        assertEquals(604800000L, dto.retentionMs());
        assertEquals(1, dto.minInsyncReplicas());
    }

    @Test
    void invalidNameIsRejected() {
        TopicRequestDTO dto = new TopicRequestDTO("bad name!", 3, (short) 3, null, null, null, null);
        Set<ConstraintViolation<TopicRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    void nullPartitionsIsRejected() {
        TopicRequestDTO dto = new TopicRequestDTO("t", null, (short) 3, null, null, null, null);
        assertTrue(validator.validate(dto).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("partitions")));
    }

    @Test
    void zeroPartitionsIsRejected() {
        TopicRequestDTO dto = new TopicRequestDTO("t", 0, (short) 3, null, null, null, null);
        assertTrue(validator.validate(dto).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("partitions")));
    }

    @Test
    void nullReplicationFactorIsRejected() {
        TopicRequestDTO dto = new TopicRequestDTO("t", 3, null, null, null, null, null);
        assertTrue(validator.validate(dto).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("replicationFactor")));
    }

    @Test
    void updateDtoValidatesPartitions() {
        TopicConfigUpdateDTO valid = new TopicConfigUpdateDTO(null, null, null, null, null);
        assertTrue(validator.validate(valid).isEmpty());

        TopicConfigUpdateDTO invalid = new TopicConfigUpdateDTO(0, null, null, null, null);
        assertFalse(validator.validate(invalid).isEmpty());
    }
}
