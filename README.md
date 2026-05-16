# Rain AI Agent Platform

Rain AI Agent Platform 是一个基于 Spring Boot 的 AI Agent 平台项目基础骨架，计划集成 PostgreSQL、Redis、RocketMQ 和 OpenAI 兼容模型能力。

## 当前进度

- 已完成 Spring Boot 3.5 + JDK 21 项目骨架。
- 已完成统一响应和统一异常处理。
- 已完成健康检查接口。
- 已完成 PostgreSQL/pgvector、Redis、RocketMQ 本地容器配置。

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

## 运行测试

```bash
mvn test
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
4. 问答接口从知识分片中召回上下文。
5. Prompt Engine 组装系统提示词和用户提示词。
6. 配置真实 `OPENAI_API_KEY` 时调用 Spring AI ChatModel，未配置时使用本地降级回答。

```bash
curl -X POST http://localhost:8080/api/rag/ask \
  -H "Content-Type: application/json" \
  -d "{\"knowledgeBaseId\":\"你的知识库ID\",\"question\":\"合同审批规则是什么？\"}"
```
