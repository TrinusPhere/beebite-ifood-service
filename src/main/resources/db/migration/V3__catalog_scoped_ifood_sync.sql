-- ─────────────────────────────────────────────────────────────────────────────
-- V3 — Escopo de Catálogo Compartilhado e Unicidade de Merchant iFood
-- 1. Vincula ifood_credential ao catalog_id compartilhado e garante 1 merchant_id único
-- 2. Vincula catalog_mapping ao catalog_id compartilhado (sincronização unificada entre lojas)
-- ─────────────────────────────────────────────────────────────────────────────

ALTER TABLE ifood_credential
    ADD COLUMN catalog_id UUID;

UPDATE ifood_credential
SET catalog_id = store_id
WHERE catalog_id IS NULL;

ALTER TABLE ifood_credential
    ALTER COLUMN catalog_id SET NOT NULL;

CREATE UNIQUE INDEX uq_ifood_credential_merchant_id ON ifood_credential(merchant_id);
CREATE UNIQUE INDEX uq_ifood_credential_catalog_id  ON ifood_credential(catalog_id);

ALTER TABLE catalog_mapping
    ADD COLUMN catalog_id UUID;

UPDATE catalog_mapping cm
SET catalog_id = COALESCE(
    (SELECT ic.catalog_id FROM ifood_credential ic WHERE ic.store_id = cm.store_id LIMIT 1),
    cm.store_id
)
WHERE cm.catalog_id IS NULL;

ALTER TABLE catalog_mapping
    ALTER COLUMN catalog_id SET NOT NULL;

CREATE INDEX idx_catalog_mapping_catalog ON catalog_mapping(catalog_id);
CREATE UNIQUE INDEX uq_catalog_mapping_catalog_ifood_type
    ON catalog_mapping(catalog_id, ifood_id, mapping_type);

