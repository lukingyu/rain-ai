# Rain AI Agent Platform

Rain AI Agent Platform 是一个基于 Spring Boot 的 AI Agent 平台项目基础骨架，计划集成 PostgreSQL、RocketMQ 和 OpenAI 兼容模型能力。

## 当前进度

- 已完成 Spring Boot 3.5 + JDK 21 项目骨架。
- 已完成统一响应和统一异常处理。
- 已完成健康检查接口。
- 已完成 PostgreSQL/pgvector、RocketMQ 本地容器配置。

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

1. 上传文档后写入数据库任务。
2. RocketMQ 消费文档摄取消息。
3. 文档内容切分为知识分片。
4. 使用 Spring AI `TokenTextSplitter` 切分文档，而不是手写切分算法。
5. 使用 Spring AI `VectorStore` 写入 pgvector，由框架负责 embedding 调用和向量表操作。
6. 问答接口通过 Spring AI `VectorStore.similaritySearch` 按知识库 metadata 过滤并召回上下文。
7. Prompt Engine 组装系统提示词和用户提示词。
8. 配置真实模型 Key 时调用 Spring AI `ChatModel`，未配置时使用本地降级回答。

```bash
curl -X POST http://localhost:8080/api/rag/ask \
  -H "Content-Type: application/json" \
  -d "{\"knowledgeBaseId\":\"你的知识库ID\",\"question\":\"合同审批规则是什么？\"}"
```

## Tool 与 Skill

当前已经支持最小可运行的工具执行框架：

- `GET /api/tools`：查看系统已注册工具。
- `POST /api/tools/execute`：按工具名和参数执行工具。
- `GET /api/skills`：查看由工具组合出来的业务技能。

已内置工具：

- `knowledge_base.list`：查询当前工作区知识库列表。
- `document.failed.list`：查询指定知识库下处理失败的文档。
- `rag.ask`：复用 RAG 能力基于知识库回答问题。

```bash
curl -X POST http://localhost:8080/api/tools/execute \
  -H "Content-Type: application/json" \
  -d "{\"toolName\":\"knowledge_base.list\",\"arguments\":{}}"
```

## Task 查询

工具执行和文档摄取都会写入 `agent_task`，任务不只是执行日志，也用于查询进度、定位失败和后续扩展重试能力。

- `GET /api/tasks/{taskId}`：查询单个任务。
- `GET /api/tasks?taskType=TOOL_EXECUTION&status=COMPLETED&limit=20`：按类型和状态查询最近任务。

## Agent Chat

当前已经支持 Agent Chat，可以用自然语言触发工具执行：

```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d "{\"message\":\"帮我列出有哪些知识库\"}"
```

执行策略：

- 配置真实模型 Key 时，使用 Spring AI `ChatModel` 进行 Planner 规划，由模型选择工具或技能。
- 未配置真实模型 Key 时，自动降级到规则版 Planner，保证本地开发链路可运行。

当前可选择的工具：

- 查询知识库列表：`knowledge_base.list`
- 查询失败文档：`document.failed.list`
- 其他知识库问题：`rag.ask`

当前重构版暂不把 Redis 会话缓存作为亮点。后续如果实现长期记忆，会采用数据库保存原始消息，并结合 AI 摘要和事实抽取，而不是简单缓存最近几条对话。
