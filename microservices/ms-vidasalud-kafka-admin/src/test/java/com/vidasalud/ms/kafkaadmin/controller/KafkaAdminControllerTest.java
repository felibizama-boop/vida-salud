package com.vidasalud.ms.kafkaadmin.controller;

import com.vidasalud.ms.kafkaadmin.dto.ConsumerGroupInfoDTO;
import com.vidasalud.ms.kafkaadmin.dto.TopicInfoDTO;
import com.vidasalud.ms.kafkaadmin.dto.TopicRequestDTO;
import com.vidasalud.ms.kafkaadmin.service.KafkaAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = KafkaAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("local")
class KafkaAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KafkaAdminService kafkaAdminService;

    @Test
    void createTopicReturnsCreated() throws Exception {
        String body = "{\"name\":\"appointments.events\",\"partitions\":3,\"replicationFactor\":3}";

        mockMvc.perform(post("/api/v1/admin/kafka/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        verify(kafkaAdminService).createTopic(any(TopicRequestDTO.class));
    }

    @Test
    void createTopicWithInvalidNameReturnsBadRequestWithDetails() throws Exception {
        String body = "{\"name\":\"bad name!\",\"partitions\":3,\"replicationFactor\":3}";

        mockMvc.perform(post("/api/v1/admin/kafka/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.name").exists());

        verifyNoInteractions(kafkaAdminService);
    }

    @Test
    void createTopicWithMissingPartitionsReturnsBadRequest() throws Exception {
        String body = "{\"name\":\"appointments.events\",\"replicationFactor\":3}";

        mockMvc.perform(post("/api/v1/admin/kafka/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.partitions").exists());
    }

    @Test
    void listTopicsReturnsOk() throws Exception {
        when(kafkaAdminService.listTopics()).thenReturn(List.of(
                new TopicInfoDTO("appointments.events", 3, 3, false, Map.of(), 604800000L, "delete")));

        mockMvc.perform(get("/api/v1/admin/kafka/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("appointments.events"))
                .andExpect(jsonPath("$[0].cleanupPolicy").value("delete"));
    }

    @Test
    void describeTopicReturnsOk() throws Exception {
        when(kafkaAdminService.describeTopic("audit.timeline")).thenReturn(
                new TopicInfoDTO("audit.timeline", 3, 3, false, Map.of(), 2592000000L, "compact,delete"));

        mockMvc.perform(get("/api/v1/admin/kafka/topics/audit.timeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("audit.timeline"));
    }

    @Test
    void updateTopicConfigReturnsOk() throws Exception {
        String body = "{\"partitions\":6,\"cleanupPolicy\":\"compact\"}";

        mockMvc.perform(put("/api/v1/admin/kafka/topics/appointments.events/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(kafkaAdminService).updateTopicConfiguration(any(String.class), any());
    }

    @Test
    void deleteTopicReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/kafka/topics/appointments.events"))
                .andExpect(status().isOk());

        verify(kafkaAdminService).deleteTopic("appointments.events");
    }

    @Test
    void listConsumerGroupsReturnsOk() throws Exception {
        when(kafkaAdminService.listConsumerGroups()).thenReturn(List.of(
                new ConsumerGroupInfoDTO("group-1", "STABLE", false, 0, List.of(), List.of(), 0L)));

        mockMvc.perform(get("/api/v1/admin/kafka/consumer-groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].groupId").value("group-1"))
                .andExpect(jsonPath("$[0].state").value("STABLE"));
    }
}
