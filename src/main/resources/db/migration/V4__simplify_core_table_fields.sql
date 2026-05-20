ALTER TABLE knowledge_base DROP CONSTRAINT IF EXISTS uk_knowledge_base_workspace_name;
ALTER TABLE knowledge_base DROP COLUMN IF EXISTS workspace_id;
ALTER TABLE knowledge_base DROP COLUMN IF EXISTS description;
ALTER TABLE knowledge_base DROP COLUMN IF EXISTS created_at;
ALTER TABLE knowledge_base DROP COLUMN IF EXISTS updated_at;
ALTER TABLE knowledge_base ADD CONSTRAINT uk_knowledge_base_name UNIQUE (name);

ALTER TABLE knowledge_document DROP COLUMN IF EXISTS workspace_id;
ALTER TABLE knowledge_document DROP COLUMN IF EXISTS content_type;
ALTER TABLE knowledge_document DROP COLUMN IF EXISTS size_bytes;
ALTER TABLE knowledge_document DROP COLUMN IF EXISTS created_at;
ALTER TABLE knowledge_document DROP COLUMN IF EXISTS updated_at;

ALTER TABLE agent_task DROP COLUMN IF EXISTS workspace_id;
ALTER TABLE agent_task DROP COLUMN IF EXISTS payload;
ALTER TABLE agent_task DROP COLUMN IF EXISTS created_at;
ALTER TABLE agent_task DROP COLUMN IF EXISTS updated_at;
