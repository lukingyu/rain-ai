# Rain AI Agent Platform

Rain AI Agent Platform 是一个基于 Spring Boot 的 AI Agent 平台项目基础骨架，计划集成 PostgreSQL、Redis、RocketMQ 和 OpenAI 兼容模型能力。

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
