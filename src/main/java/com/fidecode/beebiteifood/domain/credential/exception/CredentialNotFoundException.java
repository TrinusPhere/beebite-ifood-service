package com.fidecode.beebiteifood.domain.credential.exception;

import com.fidecode.beebiteifood.domain.shared.exception.EntityNotFoundException;

public class CredentialNotFoundException extends EntityNotFoundException {

    public CredentialNotFoundException(String storeId) {
        super("Credenciais iFood não encontradas para a store: " + storeId);
    }
}
