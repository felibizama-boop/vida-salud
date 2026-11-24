package com.vidasalud.ms.kafkaadmin.config;

import com.vidasalud.ms.kafkaadmin.dto.TopicRequestDTO;
import com.vidasalud.ms.kafkaadmin.service.KafkaAdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Inicializador idempotente de tópicos por defecto.
 * <p>
 * Nunca impide el arranque de la aplicación: cualquier error (por ejemplo, Kafka no disponible)
 * se registra como advertencia.
 */
@Component
public class DefaultTopicsInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultTopicsInitializer.class);

    private final VidaSaludKafkaProperties properties;
    private final KafkaAdminService kafkaAdminService;

    public DefaultTopicsInitializer(VidaSaludKafkaProperties properties, KafkaAdminService kafkaAdminService) {
        this.properties = properties;
        this.kafkaAdminService = kafkaAdminService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isInitializeDefaultTopics()) {
            log.info("[DefaultTopicsInitializer] La inicialización de tópicos por defecto está deshabilitada.");
            return;
        }

        List<VidaSaludKafkaProperties.TopicSpec> topics = properties.getTopics();
        if (topics == null || topics.isEmpty()) {
            log.info("[DefaultTopicsInitializer] No hay tópicos por defecto configurados.");
            return;
        }

        try {
            List<String> existing = kafkaAdminService.listTopicNames();
            for (VidaSaludKafkaProperties.TopicSpec spec : topics) {
                if (spec.getName() == null || spec.getName().isBlank()) {
                    continue;
                }
                if (existing.contains(spec.getName())) {
                    log.info("[DefaultTopicsInitializer] El tópico '{}' ya existe, se omite.", spec.getName());
                    continue;
                }
                log.info("[DefaultTopicsInitializer] Creando tópico por defecto '{}'...", spec.getName());
                kafkaAdminService.createTopic(toRequestDTO(spec));
            }
        } catch (Exception e) {
            log.warn("[DefaultTopicsInitializer] No se pudieron inicializar los tópicos por defecto: {}",
                    e.getMessage());
        }
    }

    private TopicRequestDTO toRequestDTO(VidaSaludKafkaProperties.TopicSpec spec) {
        return new TopicRequestDTO(
                spec.getName(),
                spec.getPartitions(),
                spec.getReplicationFactor(),
                spec.getCleanupPolicy(),
                spec.getRetentionMs(),
                spec.getMinInsyncReplicas(),
                spec.getExtraConfigs()
        );
    }
}
