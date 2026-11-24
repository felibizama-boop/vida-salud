package com.vidasalud.ms.kafkaadmin.service;

import com.vidasalud.ms.kafkaadmin.dto.ConsumerGroupInfoDTO;
import com.vidasalud.ms.kafkaadmin.dto.TopicConfigUpdateDTO;
import com.vidasalud.ms.kafkaadmin.dto.TopicInfoDTO;
import com.vidasalud.ms.kafkaadmin.dto.TopicRequestDTO;
import com.vidasalud.ms.kafkaadmin.exception.KafkaAdminOperationException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.AlterConfigsResult;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.CreateTopicsResult;
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
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio que encapsula toda la lógica de administración de Kafka:
 * creación, listado, modificación, eliminación de tópicos e inspección de consumer groups.
 */
@Service
public class KafkaAdminService {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdminService.class);

    private final AdminClient adminClient;

    public KafkaAdminService(AdminClient adminClient) {
        this.adminClient = adminClient;
    }

    // ------------------------------------------------------------------
    // Tópicos
    // ------------------------------------------------------------------

    /**
     * Crea un tópico a partir de la definición recibida.
     */
    public void createTopic(TopicRequestDTO dto) {
        Map<String, String> configs = new HashMap<>();
        if (dto.cleanupPolicy() != null) {
            configs.put(TopicConfig.CLEANUP_POLICY_CONFIG, dto.cleanupPolicy());
        }
        if (dto.retentionMs() != null) {
            configs.put(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(dto.retentionMs()));
        }
        if (dto.minInsyncReplicas() != null) {
            configs.put(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(dto.minInsyncReplicas()));
        }
        if (dto.extraConfigs() != null) {
            configs.putAll(dto.extraConfigs());
        }

        NewTopic newTopic = new NewTopic(dto.name(), dto.partitions(), dto.replicationFactor()).configs(configs);
        log.info("[KafkaAdminService] Creando tópico '{}' (particiones={}, replicación={})",
                dto.name(), dto.partitions(), dto.replicationFactor());
        try {
            CreateTopicsResult result = adminClient.createTopics(Collections.singletonList(newTopic));
            result.all().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaAdminOperationException("Operación interrumpida al crear el tópico '" + dto.name() + "'", e);
        } catch (Exception e) {
            throw new KafkaAdminOperationException("No se pudo crear el tópico '" + dto.name() + "': " + e.getMessage(), e);
        }
    }

    /**
     * Devuelve únicamente los nombres de los tópicos existentes (incluye internos).
     */
    public List<String> listTopicNames() {
        try {
            ListTopicsResult result = adminClient.listTopics(new ListTopicsOptions().listInternal(true));
            Collection<TopicListing> listings = result.listings().get();
            return listings.stream().map(TopicListing::name).collect(Collectors.toList());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaAdminOperationException("Operación interrumpida al listar los tópicos", e);
        } catch (Exception e) {
            throw new KafkaAdminOperationException("No se pudieron listar los tópicos: " + e.getMessage(), e);
        }
    }

    /**
     * Lista todos los tópicos con su información detallada.
     */
    public List<TopicInfoDTO> listTopics() {
        try {
            Collection<TopicListing> listings = adminClient
                    .listTopics(new ListTopicsOptions().listInternal(true))
                    .listings()
                    .get();
            if (listings.isEmpty()) {
                return Collections.emptyList();
            }
            List<String> names = listings.stream().map(TopicListing::name).collect(Collectors.toList());
            Map<String, TopicDescription> descriptions = adminClient.describeTopics(names).allTopicNames().get();
            Map<ConfigResource, Config> configs = describeConfigs(names);

            List<TopicInfoDTO> topics = new ArrayList<>();
            for (String name : names) {
                TopicDescription description = descriptions.get(name);
                if (description != null) {
                    topics.add(toTopicInfo(description, configs.get(topicConfigResource(name))));
                }
            }
            return topics;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaAdminOperationException("Operación interrumpida al listar los tópicos", e);
        } catch (Exception e) {
            throw new KafkaAdminOperationException("No se pudieron listar los tópicos: " + e.getMessage(), e);
        }
    }

    /**
     * Describe un tópico concreto.
     */
    public TopicInfoDTO describeTopic(String topicName) {
        try {
            DescribeTopicsResult topicsResult = adminClient.describeTopics(Collections.singletonList(topicName));
            TopicDescription description = topicsResult.allTopicNames().get().get(topicName);
            if (description == null) {
                throw new KafkaAdminOperationException("El tópico '" + topicName + "' no existe");
            }
            Map<ConfigResource, Config> configs = describeConfigs(Collections.singletonList(topicName));
            return toTopicInfo(description, configs.get(topicConfigResource(topicName)));
        } catch (KafkaAdminOperationException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaAdminOperationException("Operación interrumpida al describir el tópico '" + topicName + "'", e);
        } catch (Exception e) {
            throw new KafkaAdminOperationException("No se pudo describir el tópico '" + topicName + "': " + e.getMessage(), e);
        }
    }

    /**
     * Actualiza la configuración de un tópico existente. Solo se aplican los campos informados.
     */
    public void updateTopicConfiguration(String topicName, TopicConfigUpdateDTO dto) {
        try {
            if (dto.partitions() != null) {
                log.info("[KafkaAdminService] Aumentando particiones del tópico '{}' a {}", topicName, dto.partitions());
                adminClient.createPartitions(
                        Collections.singletonMap(topicName, NewPartitions.increaseTo(dto.partitions()))
                ).all().get();
            }

            List<AlterConfigOp> ops = new ArrayList<>();
            if (dto.cleanupPolicy() != null) {
                ops.add(new AlterConfigOp(new ConfigEntry(TopicConfig.CLEANUP_POLICY_CONFIG, dto.cleanupPolicy()),
                        AlterConfigOp.OpType.SET));
            }
            if (dto.retentionMs() != null) {
                ops.add(new AlterConfigOp(new ConfigEntry(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(dto.retentionMs())),
                        AlterConfigOp.OpType.SET));
            }
            if (dto.minInsyncReplicas() != null) {
                ops.add(new AlterConfigOp(new ConfigEntry(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(dto.minInsyncReplicas())),
                        AlterConfigOp.OpType.SET));
            }
            if (dto.extraConfigs() != null) {
                dto.extraConfigs().forEach((key, value) ->
                        ops.add(new AlterConfigOp(new ConfigEntry(key, value), AlterConfigOp.OpType.SET)));
            }

            if (!ops.isEmpty()) {
                ConfigResource resource = topicConfigResource(topicName);
                log.info("[KafkaAdminService] Actualizando {} configuración(es) del tópico '{}'", ops.size(), topicName);
                AlterConfigsResult result = adminClient.incrementalAlterConfigs(Collections.singletonMap(resource, ops));
                result.all().get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaAdminOperationException("Operación interrumpida al actualizar el tópico '" + topicName + "'", e);
        } catch (Exception e) {
            throw new KafkaAdminOperationException("No se pudo actualizar el tópico '" + topicName + "': " + e.getMessage(), e);
        }
    }

    /**
     * Elimina un tópico por su nombre.
     */
    public void deleteTopic(String topicName) {
        log.info("[KafkaAdminService] Eliminando tópico '{}'", topicName);
        try {
            adminClient.deleteTopics(Collections.singletonList(topicName)).all().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaAdminOperationException("Operación interrumpida al eliminar el tópico '" + topicName + "'", e);
        } catch (Exception e) {
            throw new KafkaAdminOperationException("No se pudo eliminar el tópico '" + topicName + "': " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------
    // Consumer groups
    // ------------------------------------------------------------------

    /**
     * Lista todos los consumer groups con información resumida y su lag total.
     */
    public List<ConsumerGroupInfoDTO> listConsumerGroups() {
        try {
            Collection<ConsumerGroupListing> listings = adminClient.listConsumerGroups().all().get();
            if (listings.isEmpty()) {
                return Collections.emptyList();
            }
            List<String> groupIds = listings.stream().map(ConsumerGroupListing::groupId).collect(Collectors.toList());
            Map<String, ConsumerGroupDescription> descriptions = adminClient
                    .describeConsumerGroups(groupIds).all().get();

            List<ConsumerGroupInfoDTO> groups = new ArrayList<>();
            for (String groupId : groupIds) {
                ConsumerGroupDescription description = descriptions.get(groupId);
                if (description != null) {
                    groups.add(toConsumerGroupInfo(description, computeTotalLag(groupId)));
                }
            }
            return groups;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaAdminOperationException("Operación interrumpida al listar los consumer groups", e);
        } catch (Exception e) {
            throw new KafkaAdminOperationException("No se pudieron listar los consumer groups: " + e.getMessage(), e);
        }
    }

    /**
     * Describe un consumer group concreto, incluyendo miembros y lag total.
     */
    public ConsumerGroupInfoDTO describeConsumerGroup(String groupId) {
        try {
            Map<String, ConsumerGroupDescription> descriptions = adminClient
                    .describeConsumerGroups(Collections.singletonList(groupId)).all().get();
            ConsumerGroupDescription description = descriptions.get(groupId);
            if (description == null) {
                throw new KafkaAdminOperationException("El consumer group '" + groupId + "' no existe");
            }
            return toConsumerGroupInfo(description, computeTotalLag(groupId));
        } catch (KafkaAdminOperationException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaAdminOperationException("Operación interrumpida al describir el consumer group '" + groupId + "'", e);
        } catch (Exception e) {
            throw new KafkaAdminOperationException("No se pudo describir el consumer group '" + groupId + "': " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------
    // Helpers internos
    // ------------------------------------------------------------------

    private Map<ConfigResource, Config> describeConfigs(Collection<String> topicNames) throws Exception {
        List<ConfigResource> resources = topicNames.stream()
                .map(this::topicConfigResource)
                .collect(Collectors.toList());
        DescribeConfigsResult result = adminClient.describeConfigs(resources);
        return result.all().get();
    }

    private ConfigResource topicConfigResource(String topicName) {
        return new ConfigResource(ConfigResource.Type.TOPIC, topicName);
    }

    private TopicInfoDTO toTopicInfo(TopicDescription description, Config config) {
        Map<String, String> configs = toConfigMap(config);
        int partitions = description.partitions().size();
        int replicationFactor = description.partitions().isEmpty()
                ? 0
                : description.partitions().get(0).replicas().size();
        Long retentionMs = parseLong(configs.get(TopicConfig.RETENTION_MS_CONFIG));
        String cleanupPolicy = configs.get(TopicConfig.CLEANUP_POLICY_CONFIG);
        return new TopicInfoDTO(
                description.name(),
                partitions,
                replicationFactor,
                description.isInternal(),
                configs,
                retentionMs,
                cleanupPolicy
        );
    }

    private Map<String, String> toConfigMap(Config config) {
        if (config == null) {
            return Collections.emptyMap();
        }
        Map<String, String> map = new HashMap<>();
        for (ConfigEntry entry : config.entries()) {
            map.put(entry.name(), entry.value());
        }
        return map;
    }

    private Long parseLong(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ConsumerGroupInfoDTO toConsumerGroupInfo(ConsumerGroupDescription description, Long totalLag) {
        List<ConsumerGroupInfoDTO.MemberInfoDTO> members = new ArrayList<>();
        List<String> topics = new ArrayList<>();
        for (MemberDescription member : description.members()) {
            MemberAssignment assignment = member.assignment();
            List<String> assignmentList = new ArrayList<>();
            if (assignment != null) {
                for (TopicPartition tp : assignment.topicPartitions()) {
                    assignmentList.add(tp.topic() + "-" + tp.partition());
                    if (!topics.contains(tp.topic())) {
                        topics.add(tp.topic());
                    }
                }
            }
            members.add(new ConsumerGroupInfoDTO.MemberInfoDTO(
                    member.consumerId(),
                    member.clientId(),
                    member.host(),
                    assignmentList
            ));
        }
        return new ConsumerGroupInfoDTO(
                description.groupId(),
                description.state() != null ? description.state().name() : null,
                description.isSimpleConsumerGroup(),
                members.size(),
                members,
                topics,
                totalLag
        );
    }

    private Long computeTotalLag(String groupId) {
        try {
            Map<TopicPartition, OffsetAndMetadata> committed = adminClient
                    .listConsumerGroupOffsets(groupId)
                    .partitionsToOffsetAndMetadata()
                    .get();
            if (committed.isEmpty()) {
                return 0L;
            }
            Map<TopicPartition, OffsetSpec> request = new HashMap<>();
            for (TopicPartition tp : committed.keySet()) {
                request.put(tp, OffsetSpec.latest());
            }
            Map<TopicPartition, ListOffsetsResultInfo> endOffsets = adminClient.listOffsets(request).all().get();

            long total = 0L;
            for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : committed.entrySet()) {
                ListOffsetsResultInfo info = endOffsets.get(entry.getKey());
                if (info != null) {
                    total += Math.max(0L, info.offset() - entry.getValue().offset());
                }
            }
            return total;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[KafkaAdminService] Cálculo de lag interrumpido para el grupo '{}'", groupId);
            return null;
        } catch (Exception e) {
            log.warn("[KafkaAdminService] No se pudo calcular el lag del grupo '{}': {}", groupId, e.getMessage());
            return null;
        }
    }
}
