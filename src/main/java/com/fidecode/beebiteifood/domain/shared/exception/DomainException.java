package com.fidecode.beebiteifood.domain.shared.exception;

import jakarta.ws.rs.core.Response;

/**
 * Base de todas as exceções de domínio do beebite-ifood-service.
 * Polimorfismo garante que o GlobalExceptionMapper não precise de instanceof.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    public abstract Response.Status getHttpStatus();
    public abstract String getErrorType();
}
