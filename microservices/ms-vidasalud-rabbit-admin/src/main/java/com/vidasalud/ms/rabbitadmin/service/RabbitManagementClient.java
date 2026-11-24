package com.vidasalud.ms.rabbitadmin.service;

import com.vidasalud.ms.rabbitadmin.dto.BindingInfoDTO;
import com.vidasalud.ms.rabbitadmin.dto.ExchangeInfoDTO;
import com.vidasalud.ms.rabbitadmin.dto.QueueInfoDTO;
import com.vidasalud.ms.rabbitadmin.exception.RabbitAdminException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Cliente HTTP para la Management API de RabbitMQ.
 * Se usa para operaciones de consulta (listados) que no expone {@code AmqpAdmin}.
 */
@Component
public class RabbitManagementClient {

    private final RestClient restClient;

    public RabbitManagementClient(
            @Value("${vidasalud.rabbitmq.management.base-url}") String baseUrl,
            @Value("${vidasalud.rabbitmq.management.username}") String username,
            @Value("${vidasalud.rabbitmq.management.password}") String password) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeaders(headers -> headers.setBasicAuth(username, password))
                .build();
    }

    public List<QueueInfoDTO> listQueues() {
        return getList("/api/queues", new ParameterizedTypeReference<List<QueueInfoDTO>>() {
        }, "listar colas");
    }

    public List<ExchangeInfoDTO> listExchanges() {
        return getList("/api/exchanges", new ParameterizedTypeReference<List<ExchangeInfoDTO>>() {
        }, "listar exchanges");
    }

    public List<BindingInfoDTO> listBindings() {
        return getList("/api/bindings", new ParameterizedTypeReference<List<BindingInfoDTO>>() {
        }, "listar bindings");
    }

    private <T> T getList(String uri, ParameterizedTypeReference<T> typeReference, String operation) {
        try {
            T result = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(typeReference);
            return result;
        } catch (RestClientException ex) {
            throw new RabbitAdminException(
                    "No se pudo conectar con la Management API de RabbitMQ al " + operation + ": " + ex.getMessage(),
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ex);
        }
    }
}
