CREATE TABLE agent_conversation_message (
    id BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(80) NOT NULL,
    message_type VARCHAR(20) NOT NULL,
    content TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    tool_calls JSONB NOT NULL DEFAULT '[]'::jsonb,
    tool_responses JSONB NOT NULL DEFAULT '[]'::jsonb
);

CREATE INDEX idx_agent_conversation_message_session
    ON agent_conversation_message(conversation_id, id);
