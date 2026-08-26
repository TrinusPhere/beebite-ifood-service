package com.fidecode.biteflowifood.application.dto;

/**
 * DTO — Motivo de cancelamento disponível na API do iFood.
 */
public record IfoodCancellationReason(String cancelCode, String description) {}
