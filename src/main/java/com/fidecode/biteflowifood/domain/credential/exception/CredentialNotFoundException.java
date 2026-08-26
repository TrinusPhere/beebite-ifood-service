package com.fidecode.biteflowifood.domain.credential.exception;

import com.fidecode.biteflowifood.domain.shared.exception.EntityNotFoundException;

public class CredentialNotFoundException extends EntityNotFoundException {

    public CredentialNotFoundException(String storeId) {
        super("Credenciais iFood não encontradas para a store: " + storeId);
    }
}
