package com.vidasalud.ms.rabbitadmin.controller;

import com.vidasalud.ms.rabbitadmin.dto.BindingInfoDTO;
import com.vidasalud.ms.rabbitadmin.dto.BindingRequestDTO;
import com.vidasalud.ms.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Endpoints de administración de bindings.
 */
@RestController
@RequestMapping("/api/v1/rabbit/bindings")
public class BindingAdminController {

    private final RabbitAdminService rabbitAdminService;

    public BindingAdminController(RabbitAdminService rabbitAdminService) {
        this.rabbitAdminService = rabbitAdminService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createBinding(@Valid @RequestBody BindingRequestDTO dto) {
        rabbitAdminService.createBinding(dto);
        return Map.of(
                "message", "Binding creado exitosamente",
                "queue", dto.queueName(),
                "exchange", dto.exchangeName(),
                "routingKey", dto.routingKey());
    }

    @GetMapping
    public List<BindingInfoDTO> listBindings() {
        return rabbitAdminService.listBindings();
    }

    @DeleteMapping
    public Map<String, Object> deleteBinding(@Valid @RequestBody BindingRequestDTO dto) {
        rabbitAdminService.deleteBinding(dto);
        return Map.of(
                "message", "Binding eliminado exitosamente",
                "queue", dto.queueName(),
                "exchange", dto.exchangeName(),
                "routingKey", dto.routingKey());
    }
}
