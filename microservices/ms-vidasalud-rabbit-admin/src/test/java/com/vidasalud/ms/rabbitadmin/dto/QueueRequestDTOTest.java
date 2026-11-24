package com.vidasalud.ms.rabbitadmin.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de validación de {@link QueueRequestDTO}. No levanta contexto de Spring.
 */
class QueueRequestDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void nombreEnBlancoGeneraViolaciones() {
        QueueRequestDTO dto = new QueueRequestDTO("   ", null, null, true, false, false, Map.of());

        Set<ConstraintViolation<QueueRequestDTO>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty(), "Un nombre en blanco debe generar violaciones");
    }

    @Test
    void nombreConCaracterInvalidoGeneraViolaciones() {
        QueueRequestDTO dto = new QueueRequestDTO("cola con espacio", null, null, true, false, false, Map.of());

        Set<ConstraintViolation<QueueRequestDTO>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty(), "Un nombre con espacios debe generar violaciones");
    }

    @Test
    void dtoValidoNoGeneraViolaciones() {
        QueueRequestDTO dto = new QueueRequestDTO("cola-valida.1", null, null, true, false, false, Map.of());

        Set<ConstraintViolation<QueueRequestDTO>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty(), "Un DTO válido no debe generar violaciones");
    }

    @Test
    void valoresPorDefectoSeAplicanCuandoVienenNulos() {
        QueueRequestDTO dto = new QueueRequestDTO("cola-valida", null, null, null, null, null, null);

        assertEquals(Boolean.TRUE, dto.durable(), "durable debe ser true por defecto");
        assertEquals(Boolean.FALSE, dto.exclusive(), "exclusive debe ser false por defecto");
        assertEquals(Boolean.FALSE, dto.autoDelete(), "autoDelete debe ser false por defecto");
        assertTrue(validator.validate(dto).isEmpty(), "El DTO con valores por defecto debe ser válido");
    }
}
