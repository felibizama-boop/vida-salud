package com.vidasalud.ms.kafkaadmin.config;

import org.apache.kafka.common.config.TopicConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Propiedades de configuración bajo el prefijo {@code vidasalud.kafka}.
 */
@ConfigurationProperties(prefix = "vidasalud.kafka")
public class VidaSaludKafkaProperties {

    /**
     * Indica si al arrancar se deben crear los tópicos por defecto que no existan.
     */
    private boolean initializeDefaultTopics = true;

    /**
     * Definición de los tópicos por defecto gestionados por el microservicio.
     */
    private List<TopicSpec> topics = new ArrayList<>();

    public boolean isInitializeDefaultTopics() {
        return initializeDefaultTopics;
    }

    public void setInitializeDefaultTopics(boolean initializeDefaultTopics) {
        this.initializeDefaultTopics = initializeDefaultTopics;
    }

    public List<TopicSpec> getTopics() {
        return topics;
    }

    public void setTopics(List<TopicSpec> topics) {
        this.topics = topics;
    }

    /**
     * Especificación de un tópico de Kafka.
     */
    public static class TopicSpec {

        private String name;
        private int partitions = 3;
        private short replicationFactor = 3;
        private String cleanupPolicy = "delete";
        private long retentionMs = 604800000L;
        private int minInsyncReplicas = 1;
        private Map<String, String> extraConfigs = new HashMap<>();

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

        public short getReplicationFactor() {
            return replicationFactor;
        }

        public void setReplicationFactor(short replicationFactor) {
            this.replicationFactor = replicationFactor;
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

        public Map<String, String> getExtraConfigs() {
            return extraConfigs;
        }

        public void setExtraConfigs(Map<String, String> extraConfigs) {
            this.extraConfigs = extraConfigs;
        }

        /**
         * Construye el mapa de configuraciones Kafka a partir de esta especificación.
         *
         * @return mapa con {@code cleanup.policy}, {@code retention.ms}, {@code min.insync.replicas} y las configs extra.
         */
        public Map<String, String> toTopicConfig() {
            Map<String, String> configs = new HashMap<>();
            if (cleanupPolicy != null) {
                configs.put(TopicConfig.CLEANUP_POLICY_CONFIG, cleanupPolicy);
            }
            configs.put(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(retentionMs));
            configs.put(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(minInsyncReplicas));
            if (extraConfigs != null) {
                configs.putAll(extraConfigs);
            }
            return configs;
        }
    }
}
