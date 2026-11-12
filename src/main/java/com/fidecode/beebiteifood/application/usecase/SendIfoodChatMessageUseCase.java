package com.fidecode.beebiteifood.application.usecase;

import com.fidecode.beebiteifood.application.port.IfoodChatClient;
import com.fidecode.beebiteifood.domain.credential.entity.IfoodCredential;
import com.fidecode.beebiteifood.domain.credential.repository.IfoodCredentialRepository;
import com.fidecode.beebiteifood.domain.shared.exception.IfoodIntegrationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

/**
 * @deprecated O iFood não possui API REST pública para envio de chat.
 * O chat oficial com clientes e entregadores deve ser operado via Widget oficial do iFood.
 */
@Deprecated(forRemoval = true)
@ApplicationScoped
public class SendIfoodChatMessageUseCase {

    private final IfoodCredentialRepository credentialRepository;
    private final IfoodChatClient chatClient;
    private final ProvideValidIfoodTokenUseCase tokenUseCase;

    @Inject
    public SendIfoodChatMessageUseCase(IfoodCredentialRepository credentialRepository,
                                       IfoodChatClient chatClient,
                                       ProvideValidIfoodTokenUseCase tokenUseCase) {
        this.credentialRepository = credentialRepository;
        this.chatClient = chatClient;
        this.tokenUseCase = tokenUseCase;
    }

    public void execute(String storeId, String orderId, String message) {
        UUID storeUuid = UUID.fromString(storeId);
        IfoodCredential credential = credentialRepository.findByStoreId(storeUuid)
                .orElseThrow(() -> new IfoodIntegrationException("Credencial iFood não encontrada para a loja " + storeId));

        tokenUseCase.ensureValid(credential);

        chatClient.sendMessage(credential.getAccessToken(), orderId, message);
    }
}
