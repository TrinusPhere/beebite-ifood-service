package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.port.IfoodAuthClient;
import com.fidecode.biteflowifood.application.port.SecretEncryptor;
import com.fidecode.biteflowifood.application.dto.IfoodTokenResponse;
import com.fidecode.biteflowifood.domain.credential.entity.IfoodCredential;
import com.fidecode.biteflowifood.domain.credential.exception.CredentialNotFoundException;
import com.fidecode.biteflowifood.domain.credential.repository.IfoodCredentialRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

/**
 * Use Case — Testa a conexão com o iFood usando as credenciais salvas.
 * Autentica e retorna o merchantId para confirmar que está tudo OK.
 */
@ApplicationScoped
public class TestConnectionUseCase {

    private final IfoodCredentialRepository repository;
    private final IfoodAuthClient authClient;
    private final SecretEncryptor encryptor;

    @Inject
    public TestConnectionUseCase(IfoodCredentialRepository repository,
                                  IfoodAuthClient authClient,
                                  SecretEncryptor encryptor) {
        this.repository = repository;
        this.authClient = authClient;
        this.encryptor = encryptor;
    }

    public String execute(UUID storeId) {
        IfoodCredential cred = repository.findByStoreId(storeId)
                .orElseThrow(() -> new CredentialNotFoundException(storeId.toString()));

        String plainSecret = encryptor.decrypt(cred.getEncryptedClientSecret());
        IfoodTokenResponse token = authClient.authenticate(cred.getClientId(), plainSecret);

        return "Conexão OK. MerchantId: " + cred.getMerchantId()
                + " | Token expira em: " + token.expiresIn() + "s";
    }
}
