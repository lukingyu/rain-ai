CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE knowledge_base (
    id UUID PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_knowledge_base_workspace_name UNIQUE (workspace_id, name)
);

CREATE TABLE knowledge_document (
    id UUID PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    knowledge_base_id UUID NOT NULL REFERENCES knowledge_base(id),
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(120),
    size_bytes BIGINT NOT NULL,
    storage_path TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_knowledge_document_base ON knowledge_document(knowledge_base_id);
CREATE INDEX idx_knowledge_document_status ON knowledge_document(status);

CREATE TABLE agent_task (
    id UUID PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    task_type VARCHAR(64) NOT NULL,
    aggregate_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    result JSONB,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_agent_task_aggregate ON agent_task(aggregate_id);
CREATE INDEX idx_agent_task_status ON agent_task(status);

CREATE TABLE document_chunk (
    id UUID PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    knowledge_base_id UUID NOT NULL REFERENCES knowledge_base(id),
    document_id UUID NOT NULL REFERENCES knowledge_document(id),
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    token_count INTEGER NOT NULL DEFAULT 0,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_document_chunk_index UNIQUE (document_id, chunk_index)
);

CREATE INDEX idx_document_chunk_document ON document_chunk(document_id);
CREATE INDEX idx_document_chunk_base ON document_chunk(knowledge_base_id);

CREATE TABLE embedding_record (
    id UUID PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    knowledge_base_id UUID NOT NULL REFERENCES knowledge_base(id),
    document_id UUID NOT NULL REFERENCES knowledge_document(id),
    chunk_id UUID NOT NULL REFERENCES document_chunk(id),
    embedding_model VARCHAR(120) NOT NULL,
    embedding VECTOR(1536),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_embedding_record_chunk UNIQUE (chunk_id)
);
