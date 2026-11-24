package com.vidasalud.ms.rabbitadmin.controller;

import com.vidasalud.ms.rabbitadmin.dto.QueueInfoDTO;
import com.vidasalud.ms.rabbitadmin.dto.QueueRequestDTO;
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
 * Endpoints de administración de colas.
 */
@RestController
@RequestMapping("/api/v1/rabbit/queues")
public class QueueAdminController {

    private final RabbitAdminService rabbitAdminService;

    public QueueAdminController(RabbitAdminService rabbitAdminService) {
        this.rabbitAdminService = rabbitAdminService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createQueue(@Valid @RequestBody QueueRequestDTO dto) {
        rabbitAdminService.createQueue(dto);
        return Map.of(
                "message", "Cola creada exitosamente",
                "queue", dto.name());
    }

    @GetMapping
    public List<QueueInfoDTO> listQueues() {
        return rabbitAdminService.listQueues();
    }

    @DeleteMapping("/{name}")
    public Map<String, Object> deleteQueue(@PathVariable String name) {
        rabbitAdminService.deleteQueue(name);
        return Map.of(
                "message", "Cola eliminada exitosamente",
                "queue", name);
    }

    @PostMapping("/{name}/purge")
    public Map<String, Object> purgeQueue(@PathVariable String name) {
        rabbitAdminService.purgeQueue(name);
        return Map.of(
                "message", "Cola purgada exitosamente",
                "queue", name);
    }
}
