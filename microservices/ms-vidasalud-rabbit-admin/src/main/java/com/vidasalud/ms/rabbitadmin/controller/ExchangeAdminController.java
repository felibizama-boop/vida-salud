package com.vidasalud.ms.rabbitadmin.controller;

import com.vidasalud.ms.rabbitadmin.dto.ExchangeInfoDTO;
import com.vidasalud.ms.rabbitadmin.dto.ExchangeRequestDTO;
import com.vidasalud.ms.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Endpoints de administración de exchanges.
 */
@RestController
@RequestMapping("/api/v1/rabbit/exchanges")
public class ExchangeAdminController {

    private final RabbitAdminService rabbitAdminService;

    public ExchangeAdminController(RabbitAdminService rabbitAdminService) {
        this.rabbitAdminService = rabbitAdminService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createExchange(@Valid @RequestBody ExchangeRequestDTO dto) {
        rabbitAdminService.createExchange(dto);
        return Map.of(
                "message", "Exchange creado exitosamente",
                "exchange", dto.name());
    }

    @GetMapping
    public List<ExchangeInfoDTO> listExchanges() {
        return rabbitAdminService.listExchanges();
    }

    @DeleteMapping("/{name}")
    public Map<String, Object> deleteExchange(@PathVariable String name) {
        rabbitAdminService.deleteExchange(name);
        return Map.of(
                "message", "Exchange eliminado exitosamente",
                "exchange", name);
    }
}
