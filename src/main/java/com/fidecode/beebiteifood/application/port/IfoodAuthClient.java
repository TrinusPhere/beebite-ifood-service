package com.fidecode.beebiteifood.application.port;

import com.fidecode.beebiteifood.application.dto.IfoodTokenResponse;
import com.fidecode.beebiteifood.application.dto.IfoodUserCodeResponse;

/**
 * Port — IfoodAuthClient.
 * Abstração para autenticação OAuth com a API do iFood.
 * Implementado em infrastructure por IfoodAuthRestClient.
 */
public interface IfoodAuthClient {

    /**
     * Autentica via grantType=client_credentials (App Centralizado).
     */
    IfoodTokenResponse authenticate(String clientId, String clientSecret);

    /**
     * Solicita um User Code para iniciar o fluxo OAuth de App Distribuído.
     */
    IfoodUserCodeResponse generateUserCode(String clientId);

    /**
     * Troca o Authorization Code pelo Access Token e Refresh Token.
     */
    IfoodTokenResponse exchangeAuthCode(String clientId, String clientSecret, String authorizationCode, String authorizationCodeVerifier);

    /**
     * Renova o Access Token usando o Refresh Token.
     */
    IfoodTokenResponse refreshToken(String clientId, String clientSecret, String refreshToken);
}
