package com.trinusphere.beebiteifood.application.usecase;

import com.trinusphere.beebiteifood.domain.credential.exception.CredentialNotFoundException;
import com.trinusphere.beebiteifood.domain.credential.repository.IfoodCredentialRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

/** Use Case — Remove credenciais iFood de uma Store. */
@ApplicationScoped
public class DeleteCredentialUseCase {

    private final IfoodCredentialRepository repository;

    @Inject
    public DeleteCredentialUseCase(IfoodCredentialRepository repository) {
        this.repository = repository;
    }

    public void execute(UUID storeId) {
        repository.findByStoreId(storeId)
                .orElseThrow(() -> new CredentialNotFoundException(storeId.toString()));
        repository.deleteByStoreId(storeId);
    }
}
