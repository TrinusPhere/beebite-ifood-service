-- ─────────────────────────────────────────────────────────────────────────────
-- V1 — Fundação do biteflow-ifood-service
-- Cria as tabelas para credenciais iFood e rastreamento de eventos processados
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE ifood_credential (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id                UUID NOT NULL UNIQUE,
    client_id               VARCHAR(255) NOT NULL,
    encrypted_client_secret TEXT NOT NULL,
    merchant_id             VARCHAR(255) NOT NULL,
    access_token            TEXT,
    refresh_token           TEXT,
    authorization_code      VARCHAR(32),
    token_expires_at        TIMESTAMP,
    enabled                 BOOLEAN NOT NULL DEFAULT true,
    created_at              TIMESTAMP NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE ifood_processed_event (
    event_id     VARCHAR(255) PRIMARY KEY,
    store_id     UUID NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Índices para performance
CREATE INDEX idx_ifood_credential_store    ON ifood_credential(store_id);
CREATE INDEX idx_ifood_credential_enabled  ON ifood_credential(enabled);
CREATE INDEX idx_ifood_processed_event_store ON ifood_processed_event(store_id);
CREATE INDEX idx_ifood_processed_event_date  ON ifood_processed_event(processed_at);