package com.fidecode.biteflowifood.application.dto;

/**
 * DTO — Motivo de cancelamento disponível na API do iFood.
 * Resposta de GET /orders/{id}/cancellationReasons: array de {cancelCodeId, description}.
 * O {@code cancelCodeId} é o valor enviado depois como {@code cancellationCode} em requestCancellation.
 */
public record IfoodCancellationReason(String cancelCodeId, String description) {}
