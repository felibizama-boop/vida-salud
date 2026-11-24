package com.vidasalud.ms.kafkaadmin.service;

import com.vidasalud.ms.kafkaadmin.dto.ConsumerGroupInfoDTO;
import com.vidasalud.ms.kafkaadmin.dto.TopicConfigUpdateDTO;
import com.vidasalud.ms.kafkaadmin.dto.TopicInfoDTO;
import com.vidasalud.ms.kafkaadmin.dto.TopicRequestDTO;
import com.vidasalud.ms.kafkaadmin.exception.KafkaAdminOperationException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigsResult;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.CreatePartitionsResult;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.clients.admin.DescribeConsumerGroupsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.MemberAssignment;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaAdminServiceTest {

    private AdminClient adminClient;
    private KafkaAdminService service;

    @BeforeEach
    void setUp() {
        adminClient = mock(AdminClient.class);
        service = new KafkaAdminService(adminClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    void createTopicSendsConfigsToKafka() throws Exception {
        CreateTopicsResult result = mock(CreateTopicsResult.class);
        when(result.all()).thenReturn(KafkaFuture.completedFuture(null));
        when(adminClient.createTopics(anyCollection())).thenReturn(result);

        service.createTopic(new TopicRequestDTO("appointments.events", 3, (short) 3, "delete", 604800000L, 2,
                Map.of("max.message.bytes", "1048576")));

        ArgumentCaptor<Collection<NewTopic>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(adminClient).createTopics(captor.capture());
        NewTopic newTopic = captor.getValue().iterator().next();
        assertEquals("appointments.events", newTopic.name());
        assertEquals(3, newTopic.numPartitions());
        assertEquals((short) 3, newTopic.replicationFactor());
        assertEquals("delete", newTopic.configs().get(TopicConfig.CLEANUP_POLICY_CONFIG));
        assertEquals("604800000", newTopic.configs().get(TopicConfig.RETENTION_MS_CONFIG));
        assertEquals("2", newTopic.configs().get(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG));
        assertEquals("1048576", newTopic.configs().get("max.message.bytes"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void createTopicThrowsWhenKafkaFails() throws Exception {
        KafkaFuture<Void> future = mock(KafkaFuture.class);
        when(future.get()).thenThrow(new RuntimeException("boom"));
        CreateTopicsResult result = mock(CreateTopicsResult.class);
        when(result.all()).thenReturn(future);
        when(adminClient.createTopics(anyCollection())).thenReturn(result);

        assertThrows(KafkaAdminOperationException.class,
                () -> service.createTopic(new TopicRequestDTO("t", 1, (short) 1, null, null, null, null)));
    }

    @Test
    void listTopicNamesReturnsAllNames() throws Exception {
        ListTopicsResult result = mock(ListTopicsResult.class);
        when(result.listings()).thenReturn(KafkaFuture.completedFuture(
                List.of(new TopicListing("a", false), new TopicListing("b", true))));
        when(adminClient.listTopics(any(ListTopicsOptions.class))).thenReturn(result);

        assertEquals(List.of("a", "b"), service.listTopicNames());
    }

    @Test
    void listTopicsReturnsDetailedInfo() throws Exception {
        ListTopicsResult listTopicsResult = mock(ListTopicsResult.class);
        when(listTopicsResult.listings()).thenReturn(KafkaFuture.completedFuture(
                List.of(new TopicListing("appointments.events", false))));
        when(adminClient.listTopics(any(ListTopicsOptions.class))).thenReturn(listTopicsResult);

        TopicPartitionInfo partition = new TopicPartitionInfo(0, node(1),
                List.of(node(1), node(2)), List.of());
        TopicDescription description = new TopicDescription("appointments.events", false, List.of(partition));
        DescribeTopicsResult describeTopicsResult = mock(DescribeTopicsResult.class);
        when(describeTopicsResult.allTopicNames()).thenReturn(KafkaFuture.completedFuture(
                Map.of("appointments.events", description)));
        when(adminClient.describeTopics(anyCollection())).thenReturn(describeTopicsResult);

        Config config = mock(Config.class);
        when(config.entries()).thenReturn(List.of(
                new ConfigEntry(TopicConfig.CLEANUP_POLICY_CONFIG, "delete"),
                new ConfigEntry(TopicConfig.RETENTION_MS_CONFIG, "604800000")));
        DescribeConfigsResult describeConfigsResult = mock(DescribeConfigsResult.class);
        when(describeConfigsResult.all()).thenReturn(KafkaFuture.completedFuture(
                Map.of(new ConfigResource(ConfigResource.Type.TOPIC, "appointments.events"), config)));
        when(adminClient.describeConfigs(anyCollection())).thenReturn(describeConfigsResult);

        List<TopicInfoDTO> topics = service.listTopics();
        assertEquals(1, topics.size());
        TopicInfoDTO info = topics.get(0);
        assertEquals("appointments.events", info.name());
        assertEquals(1, info.partitions());
        assertEquals(2, info.replicationFactor());
        assertFalse(info.internal());
        assertEquals("delete", info.cleanupPolicy());
        assertEquals(604800000L, info.retentionMs());
    }

    @Test
    void updateTopicConfigurationAppliesPartitionsAndConfigs() throws Exception {
        CreatePartitionsResult createPartitionsResult = mock(CreatePartitionsResult.class);
        when(createPartitionsResult.all()).thenReturn(KafkaFuture.completedFuture(null));
        when(adminClient.createPartitions(anyMap())).thenReturn(createPartitionsResult);

        AlterConfigsResult alterConfigsResult = mock(AlterConfigsResult.class);
        when(alterConfigsResult.all()).thenReturn(KafkaFuture.completedFuture(null));
        when(adminClient.incrementalAlterConfigs(anyMap())).thenReturn(alterConfigsResult);

        service.updateTopicConfiguration("appointments.events",
                new TopicConfigUpdateDTO(6, "compact", 1000L, 2, Map.of("foo", "bar")));

        verify(adminClient).createPartitions(anyMap());
        verify(adminClient).incrementalAlterConfigs(anyMap());
    }

    @Test
    void deleteTopicInvokesAdminClient() throws Exception {
        DeleteTopicsResult deleteTopicsResult = mock(DeleteTopicsResult.class);
        when(deleteTopicsResult.all()).thenReturn(KafkaFuture.completedFuture(null));
        when(adminClient.deleteTopics(anyCollection())).thenReturn(deleteTopicsResult);

        service.deleteTopic("appointments.events");

        verify(adminClient).deleteTopics(anyCollection());
    }

    @Test
    void describeConsumerGroupComputesTotalLag() throws Exception {
        ConsumerGroupDescription description = mock(ConsumerGroupDescription.class);
        when(description.groupId()).thenReturn("group-1");
        when(description.state()).thenReturn(ConsumerGroupState.STABLE);
        when(description.isSimpleConsumerGroup()).thenReturn(false);

        MemberAssignment assignment = mock(MemberAssignment.class);
        when(assignment.topicPartitions()).thenReturn(Set.of(new TopicPartition("appointments.events", 0)));
        MemberDescription member = mock(MemberDescription.class);
        when(member.consumerId()).thenReturn("consumer-1");
        when(member.clientId()).thenReturn("client-1");
        when(member.host()).thenReturn("/127.0.0.1");
        when(member.assignment()).thenReturn(assignment);
        when(description.members()).thenReturn(List.of(member));

        DescribeConsumerGroupsResult describeGroups = mock(DescribeConsumerGroupsResult.class);
        when(describeGroups.all()).thenReturn(KafkaFuture.completedFuture(Map.of("group-1", description)));
        when(adminClient.describeConsumerGroups(anyCollection())).thenReturn(describeGroups);

        TopicPartition tp = new TopicPartition("appointments.events", 0);
        ListConsumerGroupOffsetsResult offsets = mock(ListConsumerGroupOffsetsResult.class);
        when(offsets.partitionsToOffsetAndMetadata())
                .thenReturn(KafkaFuture.completedFuture(Map.of(tp, new OffsetAndMetadata(10L))));
        when(adminClient.listConsumerGroupOffsets("group-1")).thenReturn(offsets);

        ListOffsetsResultInfo endInfo = mock(ListOffsetsResultInfo.class);
        when(endInfo.offset()).thenReturn(25L);
        ListOffsetsResult listOffsets = mock(ListOffsetsResult.class);
        when(listOffsets.all()).thenReturn(KafkaFuture.completedFuture(Map.of(tp, endInfo)));
        when(adminClient.listOffsets(anyMap())).thenReturn(listOffsets);

        ConsumerGroupInfoDTO info = service.describeConsumerGroup("group-1");
        assertEquals("group-1", info.groupId());
        assertEquals("STABLE", info.state());
        assertEquals(1, info.memberCount());
        assertEquals(List.of("appointments.events"), info.topics());
        assertEquals(15L, info.totalLag());
        assertEquals(List.of("appointments.events-0"), info.members().get(0).assignment());
    }

    private Node node(int id) {
        return new Node(id, "host-" + id, 9092);
    }
}
