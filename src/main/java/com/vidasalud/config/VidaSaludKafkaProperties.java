package com.vidasalud.config;

import org.apache.kafka.common.config.TopicConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Propiedades tipadas del stack Kafka de VidaSalud (prefijo {@code vidasalud.kafka}).
 * <p>
 * Centraliza la definición de tópicos (particiones, réplicas, {@code cleanup.policy},
 * retención, réplicas mínimas sincronizadas y su Dead Letter Topic), los
 * {@code consumer group} de cada flujo y la política de reintentos.
 */
@ConfigurationProperties(prefix = "vidasalud.kafka")
public class VidaSaludKafkaProperties {

    /**
     * Tópicos de negocio indexados por clave lógica (por ejemplo {@code appointments} o {@code audit}).
     */
    private Map<String, TopicSpec> topics = new LinkedHashMap<>();

    /**
     * Identificadores de los consumer groups por flujo de negocio.
     */
    private ConsumerGroups consumerGroups = new ConsumerGroups();

    /**
     * Política de reintentos aplicada por el {@code DefaultErrorHandler} antes de la DLT.
     */
    private Retry retry = new Retry();

    public Map<String, TopicSpec> getTopics() {
        return topics;
    }

    public void setTopics(Map<String, TopicSpec> topics) {
        this.topics = topics;
    }

    public ConsumerGroups getConsumerGroups() {
        return consumerGroups;
    }

    public void setConsumerGroups(ConsumerGroups consumerGroups) {
        this.consumerGroups = consumerGroups;
    }

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry;
    }

    /**
     * Busca la definición de un tópico por su nombre físico.
     *
     * @param name nombre del tópico (por ejemplo {@code appointments.events})
     * @return la especificación encontrada o {@code null} si no existe
     */
    public TopicSpec findByName(String name) {
        if (name == null) {
            return null;
        }
        return topics.values().stream()
                .filter(spec -> name.equals(spec.getName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Especificación de un tópico de Kafka.
     */
    public static class TopicSpec {

        private String name;
        private int partitions = 3;
        private int replicas = 3;
        private String cleanupPolicy = "delete";
        private long retentionMs = 604800000L;
        private int minInsyncReplicas = 2;
        private String dlt;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getPartitions() {
            return partitions;
        }

        public void setPartitions(int partitions) {
            this.partitions = partitions;
        }

        public int getReplicas() {
            return replicas;
        }

        public void setReplicas(int replicas) {
            this.replicas = replicas;
        }

        public String getCleanupPolicy() {
            return cleanupPolicy;
        }

        public void setCleanupPolicy(String cleanupPolicy) {
            this.cleanupPolicy = cleanupPolicy;
        }

        public long getRetentionMs() {
            return retentionMs;
        }

        public void setRetentionMs(long retentionMs) {
            this.retentionMs = retentionMs;
        }

        public int getMinInsyncReplicas() {
            return minInsyncReplicas;
        }

        public void setMinInsyncReplicas(int minInsyncReplicas) {
            this.minInsyncReplicas = minInsyncReplicas;
        }

        public String getDlt() {
            return dlt;
        }

        public void setDlt(String dlt) {
            this.dlt = dlt;
        }

        /**
         * Mapa de configuraciones aplicadas al crear el tópico.
         *
         * @return configuraciones ({@code cleanup.policy}, {@code retention.ms}, {@code min.insync.replicas})
         */
        public Map<String, String> toTopicConfig() {
            Map<String, String> configs = new LinkedHashMap<>();
            configs.put(TopicConfig.CLEANUP_POLICY_CONFIG, cleanupPolicy);
            configs.put(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(retentionMs));
            configs.put(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(minInsyncReplicas));
            return configs;
        }
    }

    /**
     * Consumer groups por flujo de negocio.
     */
    public static class ConsumerGroups {

        private String appointments = "vidasalud-appointments-group";
        private String audit = "vidasalud-audit-group";

        public String getAppointments() {
            return appointments;
        }

        public void setAppointments(String appointments) {
            this.appointments = appointments;
        }

        public String getAudit() {
            return audit;
        }

        public void setAudit(String audit) {
            this.audit = audit;
        }
    }

    /**
     * Parámetros de reintento/backoff previos a la Dead Letter Topic.
     */
    public static class Retry {

        private int maxAttempts = 3;
        private long initialIntervalMs = 1000L;
        private double multiplier = 2.0;
        private long maxIntervalMs = 10000L;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getInitialIntervalMs() {
            return initialIntervalMs;
        }

        public void setInitialIntervalMs(long initialIntervalMs) {
            this.initialIntervalMs = initialIntervalMs;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(double multiplier) {
            this.multiplier = multiplier;
        }

        public long getMaxIntervalMs() {
            return maxIntervalMs;
        }

        public void setMaxIntervalMs(long maxIntervalMs) {
            this.maxIntervalMs = maxIntervalMs;
        }
    }
}
