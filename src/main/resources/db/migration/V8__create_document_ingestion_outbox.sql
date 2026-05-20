CREATE TABLE document_ingestion_outbox (
    id BIGSERIAL PRIMARY KEY,
    document_id UUID NOT NULL,
    knowledge_base_id UUID NOT NULL,
    storage_path TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_message TEXT
);

CREATE INDEX idx_document_ingestion_outbox_status
    ON document_ingestion_outbox(status, id);

CREATE INDEX idx_document_ingestion_outbox_document
    ON document_ingestion_outbox(document_id);
