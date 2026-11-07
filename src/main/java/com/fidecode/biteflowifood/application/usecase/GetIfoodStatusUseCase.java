package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.GetStatusResponse;
import com.fidecode.biteflowifood.application.dto.IfoodMerchantStatusResponse;
import com.fidecode.biteflowifood.application.port.IfoodMerchantClient;
import com.fidecode.biteflowifood.domain.credential.entity.IfoodCredential;
import com.fidecode.biteflowifood.domain.credential.exception.CredentialNotFoundException;
import com.fidecode.biteflowifood.domain.credential.repository.IfoodCredentialRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;

/**
 * Use Case — GetIfoodStatusUseCase.
 * Consulta o status da loja (aberta/fechada) no iFood via Merchant API.
 */
@ApplicationScoped
public class GetIfoodStatusUseCase {

    private final IfoodCredentialRepository credentialRepository;
    private final IfoodMerchantClient merchantClient;
    private final ProvideValidIfoodTokenUseCase tokenUseCase;

    @Inject
    public GetIfoodStatusUseCase(IfoodCredentialRepository credentialRepository,
                                 IfoodMerchantClient merchantClient,
                                 ProvideValidIfoodTokenUseCase tokenUseCase) {
        this.credentialRepository = credentialRepository;
        this.merchantClient = merchantClient;
        this.tokenUseCase = tokenUseCase;
    }

    public GetStatusResponse execute(UUID storeId) {
        IfoodCredential credential = credentialRepository.findByStoreId(storeId)
                .orElseThrow(() -> new CredentialNotFoundException(storeId.toString()));

        tokenUseCase.ensureValid(credential);

        List<IfoodMerchantStatusResponse> operations = merchantClient.getMerchantStatus(
                credential.getAccessToken(), credential.getMerchantId());

        boolean isOpen = operations.stream().anyMatch(IfoodMerchantStatusResponse::available);

        return new GetStatusResponse(isOpen, operations);
    }
}
