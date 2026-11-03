package com.fidecode.biteflowifood.infrastructure.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fidecode.biteflowifood.application.port.IfoodChatClient;
import com.fidecode.biteflowifood.domain.shared.exception.IfoodIntegrationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @deprecated Rota REST /chat/v1.0/... não existe no iFood. O chat é oferecido exclusivamente via Widget JS oficial.
 */
@Deprecated(forRemoval = true)
@ApplicationScoped
public class IfoodChatRestClient implements IfoodChatClient {

    @ConfigProperty(name = "biteflow.ifood.base-url")
    String baseUrl;

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public void sendMessage(String accessToken, String orderId, String message) {
        try {
            String path = "/chat/v1.0/orders/" + orderId + "/messages";
            Map<String, String> body = Map.of("message", message);
            String bodyJson = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IfoodIntegrationException("Erro ao enviar mensagem chat iFood: HTTP " + response.statusCode() + " " + response.body());
            }

        } catch (IfoodIntegrationException e) {
            throw e;
        } catch (Exception e) {
            throw new IfoodIntegrationException("Erro ao enviar mensagem chat iFood", e);
        }
    }

    @Override
    public List<IfoodChatMessage> getMessages(String accessToken, String orderId) {
        try {
            String path = "/chat/v1.0/orders/" + orderId + "/messages";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                 return Collections.emptyList();
            }

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IfoodIntegrationException("Erro ao buscar mensagens chat iFood: HTTP " + response.statusCode());
            }

            List<Map<String, Object>> rawMessages = objectMapper.readValue(
                    response.body(), new TypeReference<>() {});

            return rawMessages.stream()
                    .map(raw -> new IfoodChatMessage(
                            (String) raw.get("id"),
                            (String) raw.get("message"),
                            (String) raw.get("sender"),
                            (String) raw.get("createdAt")
                    ))
                    .toList();

        } catch (IfoodIntegrationException e) {
            throw e;
        } catch (Exception e) {
            throw new IfoodIntegrationException("Erro ao buscar mensagens chat iFood", e);
        }
    }
}
