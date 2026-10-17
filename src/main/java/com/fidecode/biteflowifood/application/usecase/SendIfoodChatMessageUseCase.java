package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.port.IfoodAuthClient;
import com.fidecode.biteflowifood.application.port.IfoodChatClient;
import com.fidecode.biteflowifood.application.port.SecretEncryptor;
import com.fidecode.biteflowifood.domain.credential.entity.IfoodCredential;
import com.fidecode.biteflowifood.domain.credential.repository.IfoodCredentialRepository;
import com.fidecode.biteflowifood.domain.shared.exception.IfoodIntegrationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SendIfoodChatMessageUseCase {

    private final IfoodCredentialRepository credentialRepository;
    private final IfoodAuthClient authClient;
    private final IfoodChatClient chatClient;
    private final SecretEncryptor secretEncryptor;

    @Inject
    public SendIfoodChatMessageUseCase(IfoodCredentialRepository credentialRepository,
                                       IfoodAuthClient authClient,
                                       IfoodChatClient chatClient,
                                       SecretEncryptor secretEncryptor) {
        this.credentialRepository = credentialRepository;
        this.authClient = authClient;
        this.chatClient = chatClient;
        this.secretEncryptor = secretEncryptor;
    }

    public void execute(String storeId, String orderId, String message) {
        IfoodCredential credential = credentialRepository.findByStoreId(storeId)
                .orElseThrow(() -> new IfoodIntegrationException("Credencial iFood não encontrada para a loja " + storeId));

        if (!credential.isTokenValid()) {
            String plainSecret = secretEncryptor.decrypt(credential.getEncryptedClientSecret());
            var newToken = authClient.authenticate(credential.getClientId(), plainSecret);
            credential.updateToken(newToken.accessToken(), newToken.refreshToken(), newToken.expiresIn());
            credentialRepository.save(credential);
        }

        chatClient.sendMessage(credential.getAccessToken(), orderId, message);
    }
}
