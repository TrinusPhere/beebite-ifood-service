package com.fidecode.beebiteifood.domain.shared.exception;

import jakarta.ws.rs.core.Response;

/**
 * Exceção 404 — entidade não encontrada.
 */
public class EntityNotFoundException extends DomainException {

    public EntityNotFoundException(String message) {
        super(message);
    }

    @Override
    public Response.Status getHttpStatus() { return Response.Status.NOT_FOUND; }

    @Override
    public String getErrorType() { return "NOT_FOUND"; }
}
