package com.fidecode.biteflowifood.domain.shared.exception;

/**
 * Exceção lançada quando há falha de comunicação genérica com a API do iFood
 * (timeout, 5xx, parsing error, etc.).
 * O client infra converte erros HTTP para esta exceção.
 */
public class IfoodIntegrationException extends BusinessRuleException {

    public IfoodIntegrationException(String message) {
        super("Falha na integração com o iFood: " + message);
    }

    public IfoodIntegrationException(String message, Throwable cause) {
        super("Falha na integração com o iFood: " + message);
        initCause(cause);
    }
}
