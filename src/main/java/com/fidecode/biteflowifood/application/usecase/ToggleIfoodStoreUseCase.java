package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.IfoodInterruptionResponse;
import com.fidecode.biteflowifood.application.dto.IfoodTokenResponse;
import com.fidecode.biteflowifood.application.dto.ToggleStoreRequest;
import com.fidecode.biteflowifood.application.port.IfoodAuthClient;
import com.fidecode.biteflowifood.application.port.IfoodMerchantClient;
import com.fidecode.biteflowifood.application.port.SecretEncryptor;
import com.fidecode.biteflowifood.domain.credential.entity.IfoodCredential;
import com.fidecode.biteflowifood.domain.credential.exception.CredentialNotFoundException;
import com.fidecode.biteflowifood.domain.credential.repository.IfoodCredentialRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ToggleIfoodStoreUseCase {

    private final IfoodCredentialRepository credentialRepository;
    private final IfoodMerchantClient merchantClient;
    private final IfoodAuthClient authClient;
    private final SecretEncryptor secretEncryptor;

    @Inject
    public ToggleIfoodStoreUseCase(IfoodCredentialRepository credentialRepository,
                                   IfoodMerchantClient merchantClient,
                                   IfoodAuthClient authClient,
                                   SecretEncryptor secretEncryptor) {
        this.credentialRepository = credentialRepository;
        this.merchantClient = merchantClient;
        this.authClient = authClient;
        this.secretEncryptor = secretEncryptor;
    }

    public void execute(UUID storeId, ToggleStoreRequest request) {
        IfoodCredential credential = credentialRepository.findByStoreId(storeId)
                .orElseThrow(() -> new CredentialNotFoundException(storeId.toString()));

        ensureValidToken(credential);

        String token = credential.getAccessToken();
        String merchantId = credential.getMerchantId();

        if ("OPEN".equalsIgnoreCase(request.action())) {
            List<IfoodInterruptionResponse> interruptions = merchantClient.getInterruptions(token, merchantId);
            for (IfoodInterruptionResponse interruption : interruptions) {
                merchantClient.deleteInterruption(token, merchantId, interruption.id());
            }
        } else if ("CLOSE".equalsIgnoreCase(request.action())) {
            String desc = request.description() != null ? request.description() : "Fechado pelo operador";
            String start = LocalDateTime.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
            String end = LocalDateTime.now().plusHours(12).atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
            merchantClient.createInterruption(token, merchantId, desc, start, end);
        } else {
            throw new IllegalArgumentException("Ação inválida: " + request.action());
        }
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
