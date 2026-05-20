# Rain AI Agent Platform

Rain AI Agent Platform 是一个基于 Spring Boot 3.5、JDK 21、Spring AI、PostgreSQL/pgvector 和 RocketMQ 的企业知识库 Agent 后端。

## 当前进度

- 已完成 Spring Boot 3.5 + JDK 21 项目骨架。
- 已完成统一响应和统一异常处理。
- 已完成健康检查接口。
- 已完成 PostgreSQL/pgvector、RocketMQ 本地容器配置。
- 已完成基于 Spring AI `TokenTextSplitter`、`VectorStore`、`QuestionAnswerAdvisor` 的 RAG 主链路。
- 已完成基于 Spring AI `@Tool` 的 Agent 工具调用。
- 已完成基于 Spring AI `MessageChatMemoryAdvisor` 的 Agent 会话记忆。

## 本地环境

建议准备以下环境：

- JDK 21
- Maven 3.9+
- Docker 与 Docker Compose
- 可用的 OpenAI API Key 或 OpenAI 兼容服务地址

首次启动前复制环境变量模板：

```bash
cp .env.example .env
```

根据本地配置修改 `.env` 中的数据库、缓存、消息队列和模型服务参数。

当前聊天模型使用 OpenAI 兼容配置接入 DeepSeek。为了兼容 Spring AI 的 tool calling 循环，`application.yml` 中已通过 `extra-body.thinking.type=disabled` 关闭 DeepSeek thinking mode，避免工具调用多轮请求时出现 `reasoning_content` 兼容问题。

## 启动依赖服务

如果项目根目录提供了 `docker-compose.yml`，可使用以下命令启动本地依赖：

```bash
docker compose up -d
```

## 启动应用

```bash
mvn spring-boot:run
```

## 健康检查

应用启动后可通过以下命令检查服务状态：

```bash
curl http://localhost:8080/api/health
```

## RAG 问答接口

当前已经支持文档上传后的基础 RAG 问答链路：

1. 上传文档后写入 `knowledge_document`，文档状态为 `PENDING`。
2. RocketMQ 消费文档摄取消息。
3. 文档状态依次变为 `PARSING`、`CHUNKING`、`EMBEDDING`。
4. 使用 Spring AI `TokenTextSplitter` 切分文档，而不是手写切分算法。
5. 使用 Spring AI `VectorStore` 写入 pgvector，由框架负责 embedding 调用和向量表操作。
6. 文档处理完成后状态变为 `COMPLETED`，失败则变为 `FAILED` 并记录 `error_message`。
7. 问答接口使用 Spring AI `QuestionAnswerAdvisor` 从 `VectorStore` 召回上下文，并把上下文注入 ChatClient。
8. 应用只保留业务约束和引用片段转换，不再自己手写 Prompt Engine。

```bash
curl -X POST http://localhost:8080/api/rag/ask \
  -H "Content-Type: application/json" \
  -d "{\"knowledgeBaseId\":\"你的知识库ID\",\"question\":\"合同审批规则是什么？\"}"
```

## Spring AI Tools

当前工具不再通过自定义接口手动执行，而是通过 Spring AI `@Tool` 暴露给大模型，由 Agent 在对话中自主选择是否调用。

- `GET /api/tools`：查看系统已注册工具。

已内置工具：

- `listKnowledgeBases`：查询知识库列表。
- `listFailedDocuments`：查询指定知识库下处理失败的文档。
- `searchKnowledgeBase`：从指定知识库向量库中召回相关原文片段。

```bash
curl http://localhost:8080/api/tools
```

## Agent Chat

当前已经支持 Agent Chat，可以用自然语言触发工具执行：

```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d "{\"message\":\"帮我列出有哪些知识库\"}"
```

执行策略：

- 配置真实模型 Key 时，使用 Spring AI `ChatClient` + `@Tool` 完成工具调用。
- 请求携带 `knowledgeBaseId` 时，同时启用 `QuestionAnswerAdvisor` 注入 RAG 上下文。
- 请求携带相同 `sessionId` 时，通过 Spring AI `MessageChatMemoryAdvisor` 注入历史对话。
- 未配置真实模型 Key 时，接口会直接提示模型未配置，不再伪造本地降级回答。

当前可选择的工具：

- 查询知识库列表：`listKnowledgeBases`
- 查询失败文档：`listFailedDocuments`
- 检索知识库原文片段：`searchKnowledgeBase`

当前版本已经删除 `agent_task` 和自定义 `skill`。文档处理进度直接看 `knowledge_document.status`；工具调用交给 Spring AI，而不是项目自己维护一套执行框架。

## Agent 记忆

当前会话记忆不是 Redis 最近消息缓存，而是 PostgreSQL 持久化消息流：

- `agent_conversation_message`：保存同一 `sessionId` 下的用户消息、助手消息、工具调用和工具响应。
- `PostgresAgentChatMemory`：实现 Spring AI `ChatMemory` 接口，负责从数据库读取最近窗口并追加新消息。
- `MessageChatMemoryAdvisor`：Spring AI 官方 Advisor，负责把历史消息放进当前 `ChatClient` 请求，并在模型回复后写回记忆。

`rain.ai.agent.memory.window-size` 控制每次进入模型上下文的最近消息数。数据库保留完整原始消息流，窗口只是为了控制上下文长度；后续长期摘要记忆会基于这张原始消息表做 AI 压缩，而不是丢弃历史。
