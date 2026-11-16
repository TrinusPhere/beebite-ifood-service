package com.trinusphere.beebiteifood.application.usecase;

import com.trinusphere.beebiteifood.application.dto.CredentialResponse;
import com.trinusphere.beebiteifood.application.dto.IfoodTokenResponse;
import com.trinusphere.beebiteifood.application.dto.SaveCredentialRequest;
import com.trinusphere.beebiteifood.application.port.IfoodAuthClient;
import com.trinusphere.beebiteifood.application.port.SecretEncryptor;
import com.trinusphere.beebiteifood.domain.credential.entity.IfoodCredential;
import com.trinusphere.beebiteifood.domain.credential.repository.IfoodCredentialRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Use Case — Salvar ou atualizar credenciais iFood de uma Store.
 *
 * Fluxo (fail-fast):
 * 1. Criptografa clientSecret
 * 2. Testa autenticação → lança IfoodAuthenticationException se inválido
 * 3. Salva credencial com token já preenchido
 */
@ApplicationScoped
public class SaveCredentialUseCase {

    private final IfoodCredentialRepository repository;
    private final IfoodAuthClient authClient;
    private final SecretEncryptor encryptor;

    @Inject
    public SaveCredentialUseCase(IfoodCredentialRepository repository,
                                  IfoodAuthClient authClient,
                                  SecretEncryptor encryptor) {
        this.repository = repository;
        this.authClient = authClient;
        this.encryptor = encryptor;
    }

    public CredentialResponse execute(SaveCredentialRequest request) {
        String encryptedSecret = encryptor.encrypt(request.clientSecret());

        // Fail-fast: valida credenciais antes de salvar
        IfoodTokenResponse tokenResponse = authClient.authenticate(request.clientId(), request.clientSecret());

        IfoodCredential credential = repository.findByStoreId(request.storeId())
                .orElseGet(() -> new IfoodCredential(
                        request.storeId(), request.clientId(), encryptedSecret, request.merchantId()));

        // Atualiza campos
        credential.setClientId(request.clientId());
        credential.setEncryptedClientSecret(encryptedSecret);
        credential.setMerchantId(request.merchantId());
        credential.updateToken(tokenResponse.accessToken(), tokenResponse.refreshToken(), tokenResponse.expiresIn());

        IfoodCredential saved = repository.save(credential);

        return new CredentialResponse(
                saved.getId(),
                saved.getStoreId(),
                saved.getClientId(),
                saved.getMerchantId(),
                saved.isEnabled(),
                saved.isTokenValid(),
                saved.getUpdatedAt()
        );
    }
}
