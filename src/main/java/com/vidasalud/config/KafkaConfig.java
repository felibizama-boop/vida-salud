package com.vidasalud.config;

import jakarta.validation.ConstraintViolationException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.function.BiFunction;

/**
 * Configuración avanzada del cliente Kafka para VidaSalud.
 * <p>
 * Define la infraestructura de mensajería Kafka sin afectar la topología de RabbitMQ:
 * <ul>
 *   <li>{@code ProducerFactory} / {@code KafkaTemplate} con serialización JSON e idempotencia.</li>
 *   <li>{@code ConsumerFactory} con {@code ErrorHandlingDeserializer} y {@code consumer group} explícito.</li>
 *   <li>{@code ConcurrentKafkaListenerContainerFactory} con ack manual y concurrencia configurable.</li>
 *   <li>{@code DefaultErrorHandler} con reintentos exponenciales y publicación a la Dead Letter Topic.</li>
 *   <li>{@code KafkaAdmin} que asegura los tópicos de negocio y sus DLT al arrancar.</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(VidaSaludKafkaProperties.class)
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    private final KafkaProperties kafkaProperties;
    private final VidaSaludKafkaProperties vidasaludKafkaProperties;

    public KafkaConfig(KafkaProperties kafkaProperties, VidaSaludKafkaProperties vidasaludKafkaProperties) {
        this.kafkaProperties = kafkaProperties;
        this.vidasaludKafkaProperties = vidasaludKafkaProperties;
    }

    // ------------------------------------------------------------------
    // Productores
    // ------------------------------------------------------------------

    @Bean
    public ProducerFactory<String, Object> kafkaProducerFactory() {
        return new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties());
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> kafkaProducerFactory) {
        return new KafkaTemplate<>(kafkaProducerFactory);
    }

    // ------------------------------------------------------------------
    // Consumidores
    // ------------------------------------------------------------------

    @Bean
    public ConsumerFactory<String, Object> kafkaConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties());
    }

    /**
     * Error handler con reintentos exponenciales y envío a la Dead Letter Topic correspondiente.
     * <p>
     * Los errores de validación del payload ({@link IllegalArgumentException} y
     * {@link ConstraintViolationException}) no se reintentan: van directo a la DLT.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        BiFunction<ConsumerRecord<?, ?>, Exception, TopicPartition> destinationResolver =
                (record, ex) -> new TopicPartition(resolveDlt(record.topic()), record.partition());

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate, destinationResolver);

        VidaSaludKafkaProperties.Retry retry = vidasaludKafkaProperties.getRetry();
        ExponentialBackOff backOff = new ExponentialBackOff();
        backOff.setMaxAttempts(retry.getMaxAttempts());
        backOff.setInitialInterval(retry.getInitialIntervalMs());
        backOff.setMultiplier(retry.getMultiplier());
        backOff.setMaxInterval(retry.getMaxIntervalMs());

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class, ConstraintViolationException.class);
        errorHandler.setCommitRecovered(true);
        log.info("[KafkaConfig] ErrorHandler configurado: {} intentos, backoff {}-{} ms (x{}), destino DLT.",
                retry.getMaxAttempts(), retry.getInitialIntervalMs(), retry.getMaxIntervalMs(), retry.getMultiplier());
        return errorHandler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> kafkaConsumerFactory,
            DefaultErrorHandler kafkaErrorHandler,
            @Value("${spring.kafka.listener.concurrency:3}") int concurrency) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(kafkaConsumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        factory.setConcurrency(concurrency);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    // ------------------------------------------------------------------
    // Administración de tópicos
    // ------------------------------------------------------------------

    @Bean
    public KafkaAdmin kafkaAdmin() {
        KafkaAdmin kafkaAdmin = new KafkaAdmin(kafkaProperties.buildAdminProperties());
        // La aplicación debe arrancar aunque Kafka no esté disponible en el entorno local.
        kafkaAdmin.setFatalIfBrokerNotAvailable(false);
        return kafkaAdmin;
    }

    /**
     * Asegura al arranque la existencia de los tópicos de negocio y sus Dead Letter Topics,
     * aplicando particiones, factor de replicación, {@code cleanup.policy} y retención.
     */
    @Bean
    public ApplicationRunner initializeKafkaInfrastructure(KafkaAdmin kafkaAdmin) {
        return args -> {
            log.info("[KafkaConfig] Asegurando tópicos Kafka definidos en vidasalud.kafka.topics...");
            vidasaludKafkaProperties.getTopics().forEach((key, spec) -> {
                try {
                    kafkaAdmin.createOrModifyTopics(toNewTopic(spec));
                    if (spec.getDlt() != null && !spec.getDlt().isBlank()) {
                        kafkaAdmin.createOrModifyTopics(buildDlt(spec));
                    }
                    log.info("[KafkaConfig] Tópico '{}' listo (particiones={}, réplicas={}, cleanup.policy={}, retention.ms={}, DLT='{}').",
                            spec.getName(), spec.getPartitions(), spec.getReplicas(), spec.getCleanupPolicy(),
                            spec.getRetentionMs(), spec.getDlt());
                } catch (Exception e) {
                    log.warn("[KafkaConfig] No se pudo asegurar el tópico '{}': {}", spec.getName(), e.getMessage());
                }
            });
        };
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private NewTopic toNewTopic(VidaSaludKafkaProperties.TopicSpec spec) {
        TopicBuilder builder = TopicBuilder.name(spec.getName())
                .partitions(spec.getPartitions())
                .replicas(spec.getReplicas());
        spec.toTopicConfig().forEach(builder::config);
        return builder.build();
    }

    private NewTopic buildDlt(VidaSaludKafkaProperties.TopicSpec spec) {
        return TopicBuilder.name(spec.getDlt())
                .partitions(spec.getPartitions())
                .replicas(spec.getReplicas())
                .config(org.apache.kafka.common.config.TopicConfig.CLEANUP_POLICY_CONFIG, "delete")
                .build();
    }

    /**
     * Resuelve el nombre de la Dead Letter Topic para un tópico origen.
     *
     * @param topic tópico de origen del registro fallido
     * @return la DLT configurada o {@code <topic>.DLT} por convención
     */
    private String resolveDlt(String topic) {
        VidaSaludKafkaProperties.TopicSpec spec = vidasaludKafkaProperties.findByName(topic);
        if (spec != null && spec.getDlt() != null && !spec.getDlt().isBlank()) {
            return spec.getDlt();
        }
        return topic + ".DLT";
    }
}
