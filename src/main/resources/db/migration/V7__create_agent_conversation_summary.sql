CREATE TABLE agent_conversation_summary (
    conversation_id VARCHAR(80) PRIMARY KEY,
    summary TEXT NOT NULL,
    summarized_message_id BIGINT NOT NULL
);
