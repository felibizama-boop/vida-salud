package com.vidasalud.ms.kafkaadmin;

import com.vidasalud.ms.kafkaadmin.config.VidaSaludKafkaProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = "vidasalud.kafka.initialize-default-topics=false")
@ActiveProfiles("local")
class KafkaAdminApplicationTests {

    @Autowired
    private VidaSaludKafkaProperties properties;

    @Test
    void contextLoads() {
        assertNotNull(properties);
    }

    @Test
    void defaultTopicsAreBoundFromConfiguration() {
        assertEquals(2, properties.getTopics().size());

        VidaSaludKafkaProperties.TopicSpec first = properties.getTopics().get(0);
        assertEquals("appointments.events", first.getName());
        assertEquals(3, first.getPartitions());
        assertEquals((short) 3, first.getReplicationFactor());
        assertEquals("delete", first.getCleanupPolicy());
        assertEquals(604800000L, first.getRetentionMs());
        assertEquals(2, first.getMinInsyncReplicas());

        VidaSaludKafkaProperties.TopicSpec second = properties.getTopics().get(1);
        assertEquals("audit.timeline", second.getName());
        assertEquals("compact,delete", second.getCleanupPolicy());
        assertEquals(2592000000L, second.getRetentionMs());
    }
}
