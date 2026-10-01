package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.IfoodTokenResponse;
import com.fidecode.biteflowifood.application.port.IfoodAuthClient;
import com.fidecode.biteflowifood.application.port.SecretEncryptor;
import com.fidecode.biteflowifood.domain.credential.entity.IfoodCredential;
import com.fidecode.biteflowifood.domain.credential.repository.IfoodCredentialRepository;
import com.fidecode.biteflowifood.domain.shared.exception.BusinessRuleException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class ProvideValidIfoodTokenUseCase {

    private final IfoodCredentialRepository repository;
    private final IfoodAuthClient authClient;
    private final SecretEncryptor encryptor;

    @Inject
    public ProvideValidIfoodTokenUseCase(IfoodCredentialRepository repository, IfoodAuthClient authClient, SecretEncryptor encryptor) {
        this.repository = repository;
        this.authClient = authClient;
        this.encryptor = encryptor;
    }

    public IfoodCredential execute(UUID storeId) {
        IfoodCredential credential = repository.findByStoreId(storeId)
                .orElseThrow(() -> new BusinessRuleException("Credenciais do iFood não configuradas para esta loja."));

        if (!credential.isTokenValid()) {
            String plainSecret = encryptor.decrypt(credential.getEncryptedClientSecret());
            IfoodTokenResponse tokenResponse;
            if (credential.getRefreshToken() != null && !credential.getRefreshToken().isBlank()) {
                tokenResponse = authClient.refreshToken(credential.getClientId(), plainSecret, credential.getRefreshToken());
            } else {
                tokenResponse = authClient.authenticate(credential.getClientId(), plainSecret);
            }
            credential.updateToken(tokenResponse.accessToken(), tokenResponse.refreshToken(), tokenResponse.expiresIn());
            repository.save(credential);
        }

        return credential;
    }
}
