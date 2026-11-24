package com.vidasalud.ms.kafkaadmin.controller;

import com.vidasalud.ms.kafkaadmin.dto.ConsumerGroupInfoDTO;
import com.vidasalud.ms.kafkaadmin.dto.TopicConfigUpdateDTO;
import com.vidasalud.ms.kafkaadmin.dto.TopicInfoDTO;
import com.vidasalud.ms.kafkaadmin.dto.TopicRequestDTO;
import com.vidasalud.ms.kafkaadmin.service.KafkaAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API REST de administración de Kafka: tópicos y consumer groups.
 */
@RestController
@RequestMapping("/api/v1/admin/kafka")
public class KafkaAdminController {

    private final KafkaAdminService kafkaAdminService;

    public KafkaAdminController(KafkaAdminService kafkaAdminService) {
        this.kafkaAdminService = kafkaAdminService;
    }

    @PostMapping("/topics")
    public ResponseEntity<String> createTopic(@Valid @RequestBody TopicRequestDTO dto) {
        kafkaAdminService.createTopic(dto);
        return new ResponseEntity<>("Tópico creado exitosamente", HttpStatus.CREATED);
    }

    @GetMapping("/topics")
    public ResponseEntity<List<TopicInfoDTO>> listTopics() {
        return ResponseEntity.ok(kafkaAdminService.listTopics());
    }

    @GetMapping("/topics/{name}")
    public ResponseEntity<TopicInfoDTO> describeTopic(@PathVariable String name) {
        return ResponseEntity.ok(kafkaAdminService.describeTopic(name));
    }

    @PutMapping("/topics/{name}/config")
    public ResponseEntity<String> updateTopicConfig(@PathVariable String name,
                                                    @Valid @RequestBody TopicConfigUpdateDTO dto) {
        kafkaAdminService.updateTopicConfiguration(name, dto);
        return ResponseEntity.ok("Configuración del tópico actualizada exitosamente");
    }

    @DeleteMapping("/topics/{name}")
    public ResponseEntity<String> deleteTopic(@PathVariable String name) {
        kafkaAdminService.deleteTopic(name);
        return ResponseEntity.ok("Tópico eliminado exitosamente");
    }

    @GetMapping("/consumer-groups")
    public ResponseEntity<List<ConsumerGroupInfoDTO>> listConsumerGroups() {
        return ResponseEntity.ok(kafkaAdminService.listConsumerGroups());
    }

    @GetMapping("/consumer-groups/{groupId}")
    public ResponseEntity<ConsumerGroupInfoDTO> describeConsumerGroup(@PathVariable String groupId) {
        return ResponseEntity.ok(kafkaAdminService.describeConsumerGroup(groupId));
    }
}
