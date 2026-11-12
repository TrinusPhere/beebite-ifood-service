package com.fidecode.beebiteifood.application.usecase;

import com.fidecode.beebiteifood.application.dto.CredentialResponse;
import com.fidecode.beebiteifood.domain.credential.exception.CredentialNotFoundException;
import com.fidecode.beebiteifood.domain.credential.entity.IfoodCredential;
import com.fidecode.beebiteifood.domain.credential.repository.IfoodCredentialRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

/** Use Case — Busca credenciais iFood de uma Store (sem expor clientSecret). */
@ApplicationScoped
public class GetCredentialUseCase {

    private final IfoodCredentialRepository repository;

    @Inject
    public GetCredentialUseCase(IfoodCredentialRepository repository) {
        this.repository = repository;
    }

    public CredentialResponse execute(UUID storeId) {
        IfoodCredential cred = repository.findByStoreId(storeId)
                .orElseThrow(() -> new CredentialNotFoundException(storeId.toString()));

        return new CredentialResponse(
                cred.getId(), cred.getStoreId(), cred.getClientId(),
                cred.getMerchantId(), cred.isEnabled(), cred.isTokenValid(), cred.getUpdatedAt()
        );
    }
}
