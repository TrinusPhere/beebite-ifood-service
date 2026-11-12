package com.fidecode.beebiteifood.presentation.rest;

import com.fidecode.beebiteifood.domain.shared.exception.DomainException;
import com.fidecode.beebiteifood.presentation.exception.ErrorResponse;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

/**
 * GlobalExceptionMapper — Resolve exceções via polimorfismo (P3-OCP).
 * Nova exceção de domínio não requer modificação desta classe.
 *
 * Hierarquia de resolução:
 * 1. DomainException → polimórfico (getHttpStatus + getErrorType)
 * 2. ConstraintViolationException → 400
 * 3. Security exceptions (por class name, sem import) → 401/403
 * 4. WebApplicationException → status nativo
 * 5. Fallback → 500
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Override
    public Response toResponse(Exception exception) {

        // 1. Hierarquia de domínio — resolução polimórfica
        if (exception instanceof DomainException domainEx) {
            LOG.debugf("DomainException: %s", domainEx.getMessage());
            return Response.status(domainEx.getHttpStatus())
                    .entity(new ErrorResponse(domainEx.getErrorType(), domainEx.getMessage()))
                    .build();
        }

        // 2. Validação de beans
        if (exception instanceof ConstraintViolationException cve) {
            String message = cve.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Dados inválidos");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("VALIDATION_ERROR", message))
                    .build();
        }

        // 3. Security (por class name — sem import de frameworks de segurança)
        String className = exception.getClass().getName();
        if (className.equals("io.quarkus.security.UnauthorizedException")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorResponse("UNAUTHORIZED", "Não autenticado"))
                    .build();
        }
        if (className.equals("io.quarkus.security.ForbiddenException")) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new ErrorResponse("FORBIDDEN", "Acesso negado"))
                    .build();
        }

        // 4. JAX-RS nativo
        if (exception instanceof WebApplicationException wae) {
            return wae.getResponse();
        }

        // 5. Fallback
        LOG.errorf(exception, "Erro não tratado: %s", exception.getMessage());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "Erro interno do servidor"))
                .build();
    }
}
