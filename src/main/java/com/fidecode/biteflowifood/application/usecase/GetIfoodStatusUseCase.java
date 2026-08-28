package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.GetStatusResponse;
import com.fidecode.biteflowifood.application.dto.IfoodMerchantStatusResponse;
import com.fidecode.biteflowifood.application.dto.IfoodTokenResponse;
import com.fidecode.biteflowifood.application.port.IfoodAuthClient;
import com.fidecode.biteflowifood.application.port.IfoodMerchantClient;
import com.fidecode.biteflowifood.application.port.SecretEncryptor;
import com.fidecode.biteflowifood.domain.credential.entity.IfoodCredential;
import com.fidecode.biteflowifood.domain.credential.exception.CredentialNotFoundException;
import com.fidecode.biteflowifood.domain.credential.repository.IfoodCredentialRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class GetIfoodStatusUseCase {

    private final IfoodCredentialRepository credentialRepository;
    private final IfoodMerchantClient merchantClient;
    private final IfoodAuthClient authClient;
    private final SecretEncryptor secretEncryptor;

    @Inject
    public GetIfoodStatusUseCase(IfoodCredentialRepository credentialRepository,
                                 IfoodMerchantClient merchantClient,
                                 IfoodAuthClient authClient,
                                 SecretEncryptor secretEncryptor) {
        this.credentialRepository = credentialRepository;
        this.merchantClient = merchantClient;
        this.authClient = authClient;
        this.secretEncryptor = secretEncryptor;
    }

    public GetStatusResponse execute(UUID storeId) {
        IfoodCredential credential = credentialRepository.findByStoreId(storeId)
                .orElseThrow(() -> new CredentialNotFoundException(storeId.toString()));

        ensureValidToken(credential);

        List<IfoodMerchantStatusResponse> operations = merchantClient.getMerchantStatus(
                credential.getAccessToken(), credential.getMerchantId());

        boolean isOpen = operations.stream().anyMatch(IfoodMerchantStatusResponse::available);

        return new GetStatusResponse(isOpen, operations);
    }

    private void ensureValidToken(IfoodCredential credential) {
        if (!credential.isTokenValid()) {
            String plainSecret = secretEncryptor.decrypt(credential.getEncryptedClientSecret());
            IfoodTokenResponse newToken;
            
            if (credential.getRefreshToken() != null && !credential.getRefreshToken().isBlank()) {
                newToken = authClient.refreshToken(credential.getClientId(), plainSecret, credential.getRefreshToken());
            } else {
                newToken = authClient.authenticate(credential.getClientId(), plainSecret);
            }
            
            credential.updateToken(newToken.accessToken(), newToken.refreshToken(), newToken.expiresIn());
            credentialRepository.save(credential);
        }
    }
}
