package com.fidecode.biteflowifood.application.port;

import com.fidecode.biteflowifood.application.dto.IfoodTokenResponse;

/**
 * Port — IfoodAuthClient.
 * Abstração para autenticação OAuth com a API do iFood.
 * Implementado em infrastructure por IfoodAuthRestClient.
 */
public interface IfoodAuthClient {

    /**
     * Autentica com as credenciais do iFood e retorna um access token.
     *
     * @param clientId     client ID cadastrado no portal iFood
     * @param clientSecret client secret descriptografado
     * @return token de acesso OAuth
     * @throws com.fidecode.biteflowifood.domain.shared.exception.IfoodAuthenticationException se as credenciais forem inválidas
     */
    IfoodTokenResponse authenticate(String clientId, String clientSecret);
}
