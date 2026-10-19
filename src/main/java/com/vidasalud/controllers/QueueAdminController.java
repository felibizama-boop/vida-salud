package com.vidasalud.controllers;

import com.vidasalud.dto.QueueRequestDTO;
import com.vidasalud.services.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API REST para administración de colas en tiempo real.
 */
@RestController
@RequestMapping("/api/v1/admin/queues")
public class QueueAdminController {

    private final RabbitAdminService rabbitAdminService;

    @Autowired
    public QueueAdminController(RabbitAdminService rabbitAdminService) {
        this.rabbitAdminService = rabbitAdminService;
    }

    @PostMapping
    public ResponseEntity<String> createQueue(@Valid @RequestBody QueueRequestDTO dto) {
        rabbitAdminService.createQueue(dto);
        return new ResponseEntity<>("Cola creada exitosamente", HttpStatus.CREATED);
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<String> deleteQueue(@PathVariable String name) {
        rabbitAdminService.deleteQueue(name);
        return new ResponseEntity<>("Cola eliminada exitosamente", HttpStatus.OK);
    }
}
