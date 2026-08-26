package com.fidecode.biteflowifood.infrastructure.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fidecode.biteflowifood.application.dto.IfoodTokenResponse;
import com.fidecode.biteflowifood.application.port.IfoodAuthClient;
import com.fidecode.biteflowifood.domain.shared.exception.IfoodAuthenticationException;
import com.fidecode.biteflowifood.domain.shared.exception.IfoodIntegrationException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Implementação HTTP do IfoodAuthClient.
 * Faz POST /authentication/v1.0/oauth/token com grantType=client_credentials.
 * Converte erros HTTP para exceções de domínio (nunca vaza detalhes de infra para cima).
 */
@ApplicationScoped
public class IfoodAuthRestClient implements IfoodAuthClient {

    private static final String AUTH_PATH = "/authentication/v1.0/oauth/token";

    @ConfigProperty(name = "biteflow.ifood.base-url")
    String baseUrl;

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public IfoodTokenResponse authenticate(String clientId, String clientSecret) {
        String body = "grantType=client_credentials"
                + "&clientId=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&clientSecret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + AUTH_PATH))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(15))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 401 || response.statusCode() == 403) {
                throw new IfoodAuthenticationException("HTTP " + response.statusCode() + ": " + response.body());
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IfoodIntegrationException("HTTP " + response.statusCode() + ": " + response.body());
            }

            return objectMapper.readValue(response.body(), IfoodTokenResponse.class);

        } catch (IfoodAuthenticationException | IfoodIntegrationException e) {
            throw e;
        } catch (Exception e) {
            throw new IfoodIntegrationException("Falha ao comunicar com endpoint de autenticação do iFood", e);
        }
    }
}
