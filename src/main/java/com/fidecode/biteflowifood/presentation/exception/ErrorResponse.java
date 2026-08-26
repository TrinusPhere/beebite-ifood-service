package com.fidecode.biteflowifood.presentation.exception;

/**
 * Record padrão de resposta de erro da API do serviço de integração.
 */
public record ErrorResponse(String type, String message) {}
