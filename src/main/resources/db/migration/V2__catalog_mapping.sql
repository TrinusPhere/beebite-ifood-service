CREATE TABLE IF NOT EXISTS catalog_mapping (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL,
    ifood_id VARCHAR(50) NOT NULL,
    biteflow_id UUID NOT NULL,
    mapping_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_catalog_mapping_store ON catalog_mapping(store_id);
CREATE INDEX idx_catalog_mapping_ifood ON catalog_mapping(ifood_id);
CREATE INDEX idx_catalog_mapping_biteflow ON catalog_mapping(biteflow_id);
