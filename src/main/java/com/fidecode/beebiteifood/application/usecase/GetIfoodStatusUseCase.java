package com.fidecode.beebiteifood.application.usecase;

import com.fidecode.beebiteifood.application.dto.GetStatusResponse;
import com.fidecode.beebiteifood.application.dto.IfoodMerchantStatusResponse;
import com.fidecode.beebiteifood.application.port.IfoodMerchantClient;
import com.fidecode.beebiteifood.domain.credential.entity.IfoodCredential;
import com.fidecode.beebiteifood.domain.credential.exception.CredentialNotFoundException;
import com.fidecode.beebiteifood.domain.credential.repository.IfoodCredentialRepository;

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
