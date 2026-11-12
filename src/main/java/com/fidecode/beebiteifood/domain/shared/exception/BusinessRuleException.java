package com.fidecode.beebiteifood.domain.shared.exception;

import jakarta.ws.rs.core.Response;

/**
 * Exceção 422 — regra de negócio violada.
 * Nota: Jakarta RS 3.x não possui Response.Status.UNPROCESSABLE_ENTITY.
 * Usamos fromStatusCode(422) para obter o status equivalente.
 */
public class BusinessRuleException extends DomainException {

    public BusinessRuleException(String message) {
        super(message);
    }

    @Override
    public Response.Status getHttpStatus() { return Response.Status.BAD_REQUEST; }

    @Override
    public String getErrorType() { return "BUSINESS_RULE_VIOLATION"; }
}

