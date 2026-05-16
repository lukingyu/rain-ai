CREATE INDEX IF NOT EXISTS idx_embedding_record_vector
    ON embedding_record USING hnsw (embedding vector_cosine_ops);
