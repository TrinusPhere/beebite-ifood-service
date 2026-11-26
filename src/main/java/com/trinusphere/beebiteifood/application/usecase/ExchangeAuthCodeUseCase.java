package com.trinusphere.beebiteifood.application.usecase;

import com.trinusphere.beebiteifood.application.dto.CredentialResponse;
import com.trinusphere.beebiteifood.application.dto.ExchangeAuthCodeRequest;
import com.trinusphere.beebiteifood.application.dto.IfoodTokenResponse;
import com.trinusphere.beebiteifood.application.dto.biteflow.BiteflowCatalogResponse.StoreCatalogContextResponse;
import com.trinusphere.beebiteifood.application.port.BiteflowCatalogClient;
import com.trinusphere.beebiteifood.application.port.IfoodAuthClient;
import com.trinusphere.beebiteifood.application.port.SecretEncryptor;
import com.trinusphere.beebiteifood.domain.credential.entity.IfoodCredential;
import com.trinusphere.beebiteifood.domain.credential.repository.IfoodCredentialRepository;
import com.trinusphere.beebiteifood.domain.shared.exception.BusinessRuleException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class ExchangeAuthCodeUseCase {

    private final IfoodAuthClient authClient;
    private final IfoodCredentialRepository credentialRepository;
    private final SecretEncryptor secretEncryptor;
    private final BiteflowCatalogClient biteflowCatalogClient;

    @Inject
    public ExchangeAuthCodeUseCase(IfoodAuthClient authClient,
                                   IfoodCredentialRepository credentialRepository,
                                   SecretEncryptor secretEncryptor,
                                   BiteflowCatalogClient biteflowCatalogClient) {
        this.authClient = authClient;
        this.credentialRepository = credentialRepository;
        this.secretEncryptor = secretEncryptor;
        this.biteflowCatalogClient = biteflowCatalogClient;
    }

    public CredentialResponse execute(ExchangeAuthCodeRequest request) {
        UUID resolvedCatalogId = validateStoreAndMerchantUniqueness(request.storeId(), request.merchantId());

        IfoodTokenResponse tokenResponse = authClient.exchangeAuthCode(
                request.clientId(),
                request.clientSecret(),
                request.authorizationCode(),
                request.authorizationCodeVerifier()
        );

        String encryptedSecret = secretEncryptor.encrypt(request.clientSecret());

        IfoodCredential credential = credentialRepository.findByStoreId(request.storeId())
                .orElse(new IfoodCredential(request.storeId(), resolvedCatalogId, request.clientId(), encryptedSecret, request.merchantId()));

        credential.assignCatalog(resolvedCatalogId);
        credential.setClientId(request.clientId());
        credential.setEncryptedClientSecret(encryptedSecret);
        credential.setMerchantId(request.merchantId().trim());
        credential.setAuthorizationCode(request.authorizationCode());
        credential.setEnabled(true);

        credential.updateToken(tokenResponse.accessToken(), tokenResponse.refreshToken(), tokenResponse.expiresIn());

        IfoodCredential saved = credentialRepository.save(credential);

        return new CredentialResponse(
                saved.getId(),
                saved.getStoreId(),
                saved.getCatalogId(),
                saved.getClientId(),
                saved.getMerchantId(),
                saved.isEnabled(),
                saved.isTokenValid(),
                saved.getUpdatedAt()
        );
    }

    private UUID validateStoreAndMerchantUniqueness(UUID storeId, String merchantId) {
        StoreCatalogContextResponse ctx = biteflowCatalogClient != null
                ? biteflowCatalogClient.getStoreCatalogContext(storeId)
                : null;

        if (ctx != null && "EDGE".equalsIgnoreCase(ctx.syncMode())) {
            throw new BusinessRuleException(
                    "Unidades locais (EDGE / Balcão) não podem receber conexão direta do iFood. "
                            + "Conecte o iFood na unidade Cloud / Delivery da marca — o cardápio compartilhado será sincronizado automaticamente."
            );
        }

        String normalizedMerchant = merchantId != null ? merchantId.trim() : "";
        credentialRepository.findByMerchantId(normalizedMerchant).ifPresent(existing -> {
            if (!existing.getStoreId().equals(storeId)) {
                throw new BusinessRuleException(
                        "O Merchant ID informado já está vinculado a outra unidade no BeeBite."
                );
            }
        });

        UUID catalogId = (ctx != null && ctx.catalogId() != null) ? ctx.catalogId() : storeId;
        credentialRepository.findByCatalogId(catalogId).ifPresent(existing -> {
            if (!existing.getStoreId().equals(storeId)) {
                throw new BusinessRuleException(
                        "Este cardápio compartilhado já possui uma unidade Delivery conectada ao iFood."
                );
            }
        });

        return catalogId;
    }
}
