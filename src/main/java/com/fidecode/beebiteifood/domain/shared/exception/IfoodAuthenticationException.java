package com.fidecode.beebiteifood.domain.shared.exception;

/**
 * Exceção lançada quando a autenticação com a API do iFood falha.
 * O repositório/client infra converte erros HTTP 401 para esta exceção.
 */
public class IfoodAuthenticationException extends BusinessRuleException {

    public IfoodAuthenticationException(String message) {
        super("Falha na autenticação com o iFood: " + message);
    }
}
