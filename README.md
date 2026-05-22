# Rain AI Agent Platform

Rain AI Agent Platform 是一个基于 Spring Boot 3.5、JDK 21、Spring AI、PostgreSQL/pgvector 和 RocketMQ 的企业知识库 Agent 后端。

## 当前进度

- 已完成 Spring Boot 3.5 + JDK 21 项目骨架。
- 已完成统一响应和统一异常处理。
- 已完成健康检查接口。
- 已完成 PostgreSQL/pgvector、RocketMQ 本地容器配置。
- 已完成基于 Spring AI `TikaDocumentReader`、`TokenTextSplitter`、`VectorStore`、`QuestionAnswerAdvisor` 的 RAG 主链路。
- 已完成基于 Spring AI `@Tool` 的 Agent 工具调用。
- 已完成基于 Spring AI `MessageChatMemoryAdvisor` 的 Agent 会话记忆。
- 已完成文档摄取 RocketMQ 可靠投递 outbox，并使用虚拟线程并发投递阻塞式 MQ 请求。
- 已完成 RocketMQ 消费侧线程、批量、重试次数控制，避免异常文档拖住整批消息。
- 已完成知识库文档列表查询，前端可直接展示文档摄取状态和失败原因。

阶段性架构、流程、表结构和面试讲解见：[后端阶段总结](docs/后端阶段总结.md)。

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

## 启动前端

前端工程位于 `web` 目录，使用 Vite + React + TypeScript 实现。

```bash
cd web
npm install
npm run dev
```

开发环境默认通过 Vite proxy 把 `/api` 转发到 `http://localhost:8080`，所以后端保持 8080 端口启动即可。

## 健康检查

应用启动后可通过以下命令检查服务状态：

```bash
curl http://localhost:8080/api/health
```

## RAG 问答接口

当前已经支持文档上传后的基础 RAG 问答链路：

1. 上传文档后写入 `knowledge_document`，文档状态为 `PENDING`。
2. 同一个数据库事务内写入 `document_ingestion_outbox`，保证文档登记和消息意图一起提交。
3. Outbox relay 定时领取待投递消息，用虚拟线程并发调用 RocketMQ producer。
4. RocketMQ 消费文档摄取消息。
5. 文档状态依次变为 `PARSING`、`CHUNKING`、`EMBEDDING`。
6. 使用 Spring AI `TikaDocumentReader` 读取上传文档正文，支持 PDF、Word、HTML、文本等多种常见格式，而不是把文件简单当成字符串读取。
7. 使用 Spring AI `TokenTextSplitter` 切分文档，而不是手写切分算法。
8. 使用 Spring AI `VectorStore` 写入 pgvector，由框架负责 embedding 调用和向量表操作。
9. 文档处理完成后状态变为 `COMPLETED`，失败则变为 `FAILED` 并记录 `error_message`。
10. 问答接口使用 Spring AI `QuestionAnswerAdvisor` 从 `VectorStore` 召回上下文，并把上下文注入 ChatClient。
11. 回答生成后使用 Spring AI structured output 做依据性自检，判断回答是否被召回片段支撑。
12. 应用只保留业务约束和引用片段转换，不再自己手写 Prompt Engine。

```bash
curl -X POST http://localhost:8080/api/rag/ask \
  -H "Content-Type: application/json" \
  -d "{\"knowledgeBaseId\":\"你的知识库ID\",\"question\":\"合同审批规则是什么？\"}"
```

RAG 返回体中的 `groundingEvaluation` 表示依据性自检结果：

- `grounded`：回答是否被参考片段充分支撑。
- `conclusion`：审查结论。
- `unsupportedClaims`：回答中缺少依据的关键结论。

## Spring AI Tools

当前工具不再通过自定义接口手动执行，而是通过 Spring AI `@Tool` 暴露给大模型，由 Agent 在对话中自主选择是否调用。

- `GET /api/tools`：查看系统已注册工具。

已内置工具：

- `listKnowledgeBases`：查询知识库列表。
- `listFailedDocuments`：查询指定知识库下处理失败的文档。
- `reingestFailedDocuments`：重新投递指定知识库下所有处理失败的文档摄取任务。
- `reingestAllDocuments`：重新投递指定知识库下全部文档摄取任务。
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

流式对话接口使用 SSE 返回，适合后续前端逐字展示模型输出：

```bash
curl -N -X POST http://localhost:8080/api/agent/chat/stream \
  -H "Content-Type: application/json" \
  -d "{\"message\":\"帮我列出有哪些知识库\"}"
```

SSE 事件说明：

- `session`：返回本次对话使用的 `sessionId`。
- `delta`：模型流式输出片段。
- `citations`：当本轮请求启用 RAG 时，返回 Spring AI `QuestionAnswerAdvisor` 召回到的引用片段。
- `done`：本轮输出结束。
- `error`：模型调用或流式发送失败。

执行策略：

- 配置真实模型 Key 时，使用 Spring AI `ChatClient` + `@Tool` 完成工具调用。
- 请求携带 `knowledgeBaseId` 时，同时启用 `QuestionAnswerAdvisor` 注入 RAG 上下文。
- 请求携带相同 `sessionId` 时，通过 Spring AI `MessageChatMemoryAdvisor` 注入历史对话。
- 流式接口使用 Spring AI `ChatClient.stream().chatClientResponse()`，从真实流式响应中提取 `delta`，并从响应元数据中提取 RAG 引用片段。
- 未配置真实模型 Key 时，接口会直接提示模型未配置，不再伪造本地降级回答。

当前可选择的工具：

- 查询知识库列表：`listKnowledgeBases`
- 查询失败文档：`listFailedDocuments`
- 重驱动失败文档：`reingestFailedDocuments`
- 重驱动整库文档：`reingestAllDocuments`
- 检索知识库原文片段：`searchKnowledgeBase`

当前版本已经删除 `agent_task` 和自定义 `skill`。文档处理进度直接看 `knowledge_document.status`；工具调用交给 Spring AI，而不是项目自己维护一套执行框架。

## Agent 记忆

当前会话记忆不是 Redis 最近消息缓存，而是 PostgreSQL 持久化消息流：

- `agent_conversation_message`：保存同一 `sessionId` 下的用户消息、助手消息、工具调用和工具响应。
- `PostgresAgentChatMemory`：实现 Spring AI `ChatMemory` 接口，负责从数据库读取最近窗口并追加新消息。
- `MessageChatMemoryAdvisor`：Spring AI 官方 Advisor，负责把历史消息放进当前 `ChatClient` 请求，并在模型回复后写回记忆。

`rain.ai.agent.memory.window-size` 控制每次进入模型上下文的最近消息数。数据库保留完整原始消息流，窗口只是为了控制上下文长度。

长期摘要记忆由 `agent_conversation_summary` 保存：

- `AgentConversationSummaryService`：当同一会话出现足够多的新消息时，调用 Spring AI `ChatClient.entity(...)` 生成结构化摘要。
- `ConversationSummaryDraft`：模型结构化输出对象，包含摘要、稳定事实、用户偏好和待确认问题。
- `AgentChatService`：每次请求先读取长期摘要，并注入系统提示词；最近对话仍由 `MessageChatMemoryAdvisor` 注入。

这形成两层记忆：短期窗口负责上下文连续性，长期摘要负责跨长对话保留稳定信息。

## RocketMQ 可靠投递

文档摄取没有直接在事务提交后调用 RocketMQ，而是使用本地消息表解决一致性问题：

- `document_ingestion_outbox`：保存待投递的文档摄取消息、投递状态、重试次数和失败原因。
- `DocumentIngestionService`：上传或重新摄取文档时，在同一个数据库事务内写 `knowledge_document` 和 outbox。
- `DocumentIngestionOutboxRelay`：定时领取 `PENDING`、未最终确认的 `SENDING` 或可重试的 `FAILED` 记录，投递 RocketMQ，成功后标记 `SENT`。
- `GET /api/knowledge-bases/{knowledgeBaseId}/documents`：查询指定知识库下的文档列表、摄取状态和失败原因。
- `POST /api/knowledge-bases/{knowledgeBaseId}/documents/reingest`：把指定知识库下全部文档重新置为 `PENDING`，并重新写入 outbox。
- `POST /api/knowledge-bases/{knowledgeBaseId}/documents/failed/reingest`：把指定知识库下失败文档重新置为 `PENDING`，并重新写入 outbox 等待投递。
- `DocumentIngestionOutboxConfig`：使用 JDK 21 虚拟线程执行阻塞式 RocketMQ `send`，避免平台线程被大量 MQ 网络 IO 占住。
- `DocumentReingestConfig`：使用 JDK 21 虚拟线程并发登记批量重摄取任务，`rain.ai.knowledge.reingest-concurrency` 控制进入数据库事务的并发上限。
- `DocumentIngestionConsumerConfig`：使用 RocketMQ 原生消费线程、单批消息数和最大重试次数控制；文档摄取默认单条消费，避免一条异常消息拖住整批消息。

这个设计解决的是“数据库已经提交，但 MQ 尚未发送时应用宕机”的问题。极端情况下可能重复投递，例如 MQ 已发送但还没标记 `SENT` 时应用重启；消费者会先按 `document_id` 删除旧向量再写入新向量，因此摄取结果保持幂等。若 outbox 投递 RocketMQ 达到最大失败次数，仍处于 `PENDING` 的文档会被标记为 `FAILED`，后续可通过接口或 Spring AI Tool 重新驱动。
