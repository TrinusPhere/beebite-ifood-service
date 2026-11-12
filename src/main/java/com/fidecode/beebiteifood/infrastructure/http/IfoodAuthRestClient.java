package com.fidecode.beebiteifood.infrastructure.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fidecode.beebiteifood.application.dto.IfoodTokenResponse;
import com.fidecode.beebiteifood.application.dto.IfoodUserCodeResponse;
import com.fidecode.beebiteifood.application.port.IfoodAuthClient;
import com.fidecode.beebiteifood.domain.shared.exception.IfoodAuthenticationException;
import com.fidecode.beebiteifood.domain.shared.exception.IfoodIntegrationException;

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

    @Override
    public IfoodUserCodeResponse generateUserCode(String clientId) {
        String body = "clientId=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/authentication/v1.0/oauth/userCode"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(15))
                .build();

        return executeRequest(request, IfoodUserCodeResponse.class);
    }

    @Override
    public IfoodTokenResponse exchangeAuthCode(String clientId, String clientSecret, String authorizationCode, String authorizationCodeVerifier) {
        String body = "grantType=authorization_code"
                + "&clientId=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&clientSecret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                + "&authorizationCode=" + URLEncoder.encode(authorizationCode, StandardCharsets.UTF_8)
                + "&authorizationCodeVerifier=" + URLEncoder.encode(authorizationCodeVerifier, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + AUTH_PATH))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(15))
                .build();

        return executeRequest(request, IfoodTokenResponse.class);
    }

    @Override
    public IfoodTokenResponse refreshToken(String clientId, String clientSecret, String refreshToken) {
        String body = "grantType=refresh_token"
                + "&clientId=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&clientSecret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                + "&refreshToken=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + AUTH_PATH))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(15))
                .build();

        return executeRequest(request, IfoodTokenResponse.class);
    }

    private <T> T executeRequest(HttpRequest request, Class<T> responseClass) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 401 || response.statusCode() == 403) {
                throw new IfoodAuthenticationException("HTTP " + response.statusCode() + ": " + response.body());
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IfoodIntegrationException("HTTP " + response.statusCode() + ": " + response.body());
            }

            return objectMapper.readValue(response.body(), responseClass);

        } catch (IfoodAuthenticationException | IfoodIntegrationException e) {
            throw e;
        } catch (Exception e) {
            throw new IfoodIntegrationException("Falha ao comunicar com endpoint de autenticação do iFood", e);
        }
    }
}
