package com.vidasalud.ms.kafkaadmin.config;

import org.apache.kafka.common.config.TopicConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VidaSaludKafkaPropertiesTest {

    @Test
    void toTopicConfigIncludesStandardAndExtraConfigs() {
        VidaSaludKafkaProperties.TopicSpec spec = new VidaSaludKafkaProperties.TopicSpec();
        spec.setName("audit.timeline");
        spec.setCleanupPolicy("compact,delete");
        spec.setRetentionMs(2592000000L);
        spec.setMinInsyncReplicas(2);
        spec.setExtraConfigs(Map.of("segment.bytes", "1073741824"));

        Map<String, String> configs = spec.toTopicConfig();

        assertEquals("compact,delete", configs.get(TopicConfig.CLEANUP_POLICY_CONFIG));
        assertEquals("2592000000", configs.get(TopicConfig.RETENTION_MS_CONFIG));
        assertEquals("2", configs.get(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG));
        assertEquals("1073741824", configs.get("segment.bytes"));
    }

    @Test
    void defaultsAreSane() {
        VidaSaludKafkaProperties.TopicSpec spec = new VidaSaludKafkaProperties.TopicSpec();
        assertEquals(3, spec.getPartitions());
        assertEquals((short) 3, spec.getReplicationFactor());
        assertEquals("delete", spec.getCleanupPolicy());
        assertEquals(604800000L, spec.getRetentionMs());
        assertEquals(1, spec.getMinInsyncReplicas());
    }
}
