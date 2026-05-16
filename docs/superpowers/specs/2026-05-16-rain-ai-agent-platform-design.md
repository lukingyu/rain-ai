# Rain AI Agent Platform 后端设计文档

## 1. 项目定位

Rain AI Agent Platform 是一个面向企业知识库和智能任务执行的 Java AI 后端项目。

项目目标不是做一个简单的聊天机器人，而是构建一个具备 RAG、Prompt Engine、Tools、Function Calling、Skills、异步任务编排能力的 AI 应用后端平台。它的核心价值是让用户能够上传企业文档，系统完成异步解析、切分、Embedding、向量入库，然后在提问时基于知识库进行高级检索增强生成，并在需要时调用受控业务工具或创建异步 AI 任务。

求职展示方向以 AI 应用后端工程师为主，高并发 Java 后端能力为支撑亮点。工程质量和可观测性保持必要水准，但不作为第一阶段主卖点。

## 2. 技术选型

第一阶段采用以下技术栈：

- JDK 21
- Spring Boot 3.5.x
- Spring AI
- PostgreSQL + pgvector
- Redis
- RocketMQ 5.x
- Maven
- Docker Compose
- OpenAI 或兼容 OpenAI 协议的模型服务

选型理由：

- Spring Boot 仍然是 Java 求职中最容易被面试官接受的主流生态。
- Spring AI 能自然承载模型调用、Embedding、RAG、工具调用和 MCP 等 AI 应用能力。
- PostgreSQL + pgvector 可以同时保存结构化业务数据和向量数据，适合第一阶段降低部署复杂度。
- RocketMQ 用于文档摄取、批量 Embedding、Skill 长任务执行、失败重试和死信处理。
- JDK 21 虚拟线程适合 AI 后端大量阻塞 IO 场景，例如模型调用、向量检索、数据库访问和工具调用。

## 3. 核心业务场景

### 3.1 知识库管理

用户可以创建知识库、上传文档、查看文档处理状态。

文档上传后不在请求线程内同步处理，而是创建文档摄取任务，并通过 RocketMQ 投递消息。消费者负责文档解析、切分、Embedding 和向量入库。

文档处理状态包括：

- `PENDING`：等待处理
- `PARSING`：解析文档
- `CHUNKING`：文本切分
- `EMBEDDING`：向量化
- `COMPLETED`：处理完成
- `FAILED`：处理失败

### 3.2 高级 RAG 问答

用户在指定知识库范围内提问，系统通过完整 RAG Pipeline 生成有来源依据的回答。

RAG 链路：

```text
原始问题
 -> 问题标准化
 -> Query Rewrite
 -> 向量召回
 -> 关键词召回
 -> 召回结果合并去重
 -> Rerank
 -> 上下文压缩
 -> 引用构建
 -> Prompt Engine 组装提示词
 -> LLM 生成答案
```

回答内容包括：

- 最终答案
- 引用片段
- 来源文档
- 检索过程摘要
- 工具调用轨迹

### 3.3 Agent 工具调用

系统内置受控工具，让模型只能在明确边界内调用业务能力。

第一批工具：

- `search_knowledge`：在指定知识库中检索资料
- `create_task`：创建异步 AI 任务
- `get_task_status`：查询任务状态

工具调用不直接暴露任意代码执行能力，而是通过 Tool Registry 注册和执行。每个工具都必须声明名称、描述、参数 schema、权限要求和结果类型。

### 3.4 Skill 执行

Skill 是可复用的 AI 工作流，用来承载“总结文档”“分析风险点”这类稳定任务。

第一阶段实现两个 Skill：

- `document_summary_skill`：总结指定文档
- `risk_analysis_skill`：分析制度、合同、规范中的风险点

每个 Skill 包含：

- 名称
- 描述
- 输入 schema
- 绑定的 prompt template 版本
- 允许使用的工具列表
- 执行模式：同步或异步

长耗时 Skill 通过 RocketMQ 执行，避免阻塞 HTTP 请求线程。

## 4. 总体架构

后端第一阶段采用 Spring Boot 单体架构，内部按模块边界组织代码，不做微服务拆分。

模块划分：

- `api`：REST 接口、SSE 流式接口、请求响应对象
- `ai-core`：模型调用、Prompt Engine、对话编排、Skill 调度
- `rag`：文档切分、Embedding、检索、Rerank、上下文压缩、引用构建
- `agent-tools`：工具注册、参数校验、工具执行、调用日志
- `task`：异步任务、任务状态机、RocketMQ 生产与消费、失败重试
- `knowledge`：知识库、文档、分段、向量元数据
- `common`：统一异常、基础响应、配置、通用工具

核心链路：

```text
用户上传文档
  -> 保存文档元数据
  -> 创建文档摄取任务
  -> RocketMQ 投递消息
  -> 消费端解析文档
  -> 文档切分
  -> 批量 Embedding
  -> 写入 pgvector

用户发起提问
  -> Prompt Engine 构造系统规则
  -> RAG Pipeline 检索上下文
  -> LLM 判断是否需要工具调用
  -> Tool Registry 执行受控工具
  -> 返回答案、引用来源和工具调用轨迹
```

## 5. 关键技术难点

### 5.1 Prompt Engine

Prompt Engine 不使用简单字符串拼接，而是实现轻量模板引擎和版本管理。

能力包括：

- Prompt 模板版本管理
- 变量注入和变量完整性校验
- system、developer、user、context 分层
- RAG 上下文和用户输入隔离
- 防 Prompt Injection 规则
- 渲染失败时明确抛出业务异常

重点解决的问题：

- Prompt 分散在 Service 中导致难维护
- 缺少版本管理导致实验不可追踪
- 检索内容污染系统指令
- 变量缺失时产生不可控模型行为

### 5.2 RAG Pipeline

RAG 不是“向量查询 + 塞给模型”，而是一条可组合流水线。

核心组件：

- `QuestionNormalizer`
- `QueryRewriter`
- `HybridRetriever`
- `RetrievalMerger`
- `Reranker`
- `ContextCompressor`
- `CitationBuilder`

重点解决的问题：

- 用户口语化问题不适合直接检索
- 单一路径召回容易漏内容
- 重复片段浪费上下文窗口
- 长上下文增加 token 成本并降低回答质量
- 没有引用来源会降低回答可信度

第一阶段的 Rerank 先采用规则评分，后续可替换为模型 Rerank。

### 5.3 Tool Registry

Tool Registry 负责管理 AI 可调用的业务工具。

核心抽象：

- `AiTool`
- `ToolDefinition`
- `ToolRegistry`
- `ToolExecutor`
- `ToolCallLog`

重点解决的问题：

- 模型不能直接调用任意后端方法
- 工具参数必须校验
- 工具权限必须受控
- 工具执行结果必须标准化
- 工具调用过程必须可追溯

工具结果类型：

- 成功
- 普通失败
- 可重试失败
- 权限失败
- 参数校验失败

### 5.4 Skill 工作流

Skill 是 Agent 的可复用任务能力。

第一阶段 Skill 不追求复杂多 Agent 协作，而是重点做到输入 schema、prompt 版本、允许工具、执行模式的明确绑定。

重点解决的问题：

- 常见 AI 任务不能散落在 Controller 或 Service 中
- 不同任务需要不同工具权限
- 长任务需要异步执行
- 任务输出结构需要稳定，方便前端和后续系统消费

### 5.5 RocketMQ 异步编排

RocketMQ 承担耗时和不稳定任务的异步化。

第一阶段消息类型：

- 文档摄取消息
- Embedding 批处理消息
- Skill 执行消息
- 死信补偿消息

重点解决的问题：

- 大文档处理不能阻塞上传请求
- Embedding 批处理需要失败重试
- AI 长任务执行时间不可控
- 重复消费必须通过任务幂等避免重复写入
- 多次失败后进入死信队列，便于人工或接口补偿

### 5.6 JDK 21 并发能力

AI 后端的主要瓶颈是外部 IO。第一阶段使用 JDK 21 虚拟线程和异步编排提升吞吐。

应用场景：

- HTTP 请求处理
- 模型调用
- Embedding 调用
- 向量检索
- 工具调用
- RAG 多路召回

并发策略：

- 虚拟线程承接高并发阻塞 IO
- `CompletableFuture` 并行执行向量召回和关键词召回
- AI 调用、Embedding、文档解析、MQ 消费使用独立执行器隔离
- 所有外部调用设置超时
- Embedding 批处理增加限流

## 6. 数据模型

第一阶段核心表：

- `knowledge_base`：知识库
- `knowledge_document`：文档元数据
- `document_chunk`：文档分段
- `embedding_record`：向量记录
- `ai_conversation`：会话
- `ai_message`：消息
- `prompt_template`：Prompt 模板
- `tool_call_log`：工具调用日志
- `agent_task`：AI 任务
- `skill_definition`：Skill 定义

数据存储策略：

- PostgreSQL 保存业务结构化数据
- pgvector 保存向量
- Redis 保存热点任务状态、短期会话记忆和必要缓存

第一阶段保留 `workspace_id` 字段，但不实现完整多租户权限系统。

## 7. API 边界

第一阶段接口分组：

- 知识库接口：创建知识库、查询知识库
- 文档接口：上传文档、查询文档处理状态
- 问答接口：普通问答、SSE 流式问答
- 任务接口：创建任务、查询任务状态、查询任务结果
- Skill 接口：执行文档总结、执行风险分析

接口返回统一结构，错误码使用中文语义命名，错误信息面向开发调试清晰可读。

## 8. 第一阶段 MVP 范围

第一阶段必须完成：

- Spring Boot 项目初始化
- Docker Compose 基础环境
- PostgreSQL、pgvector、Redis、RocketMQ 接入
- 知识库创建
- 文档上传
- 文档摄取任务创建
- RocketMQ 文档处理消费
- 文档解析和切分
- 批量 Embedding
- 向量入库
- RAG Pipeline
- Prompt Engine
- Tool Registry
- `search_knowledge`、`create_task`、`get_task_status`
- `document_summary_skill`
- `risk_analysis_skill`
- SSE 流式回答
- 必要测试和接口文档

第一阶段暂不实现：

- 用户登录注册
- 完整权限系统
- 完整多租户隔离
- 复杂管理后台
- 多模型路由
- 成本统计大盘
- 完整可观测平台
- 微服务拆分

## 9. 开发顺序

推荐按以下顺序实现：

1. 初始化 Spring Boot + Maven 项目。
2. 编写 Docker Compose，启动 PostgreSQL、pgvector、Redis、RocketMQ。
3. 建立知识库、文档、任务数据模型。
4. 实现文档上传和任务创建。
5. 接入 RocketMQ，完成文档异步处理骨架。
6. 实现文档解析和文本切分。
7. 接入 Embedding，写入 pgvector。
8. 实现基础向量检索。
9. 升级为完整 RAG Pipeline。
10. 实现 Prompt Engine。
11. 实现 Tool Registry 和第一批工具。
12. 实现 Skill 执行模型。
13. 实现 SSE 流式回答。
14. 补充必要测试和接口文档。
15. 后端稳定后再实现前端。

## 10. 代码与协作约定

项目文档、代码注释、Git 提交信息默认使用中文。

代码风格要求：

- 避免无意义的重复代码。
- 业务边界清晰，避免 Controller 直接堆业务逻辑。
- AI 编排逻辑、RAG 逻辑、工具执行逻辑分层实现。
- 注释只解释关键业务意图、复杂流程和非显而易见的技术选择。
- 不为了炫技引入无必要抽象。
- 第一阶段不拆微服务，先保证完整闭环和技术深度。

测试策略：

- RAG Pipeline、Prompt Engine、Tool Registry、任务状态机必须有单元测试。
- 数据库、RocketMQ、pgvector 等基础设施优先使用集成测试或本地 Docker 环境验证。
- 模型调用相关测试保留可替换接口，避免测试强依赖真实模型服务。

## 11. 面试叙事重点

面试时项目可以按以下主线讲：

> 我做的是一个企业知识库与智能任务执行平台，核心不是套壳聊天，而是围绕 AI 应用后端的关键问题做工程化实现：文档异步摄取、完整 RAG Pipeline、Prompt Engine、受控 Tool Calling、可复用 Skill 工作流，以及 RocketMQ 和 JDK 21 支撑的高并发异步执行。

重点亮点：

- RAG 质量：Query Rewrite、Hybrid Search、Rerank、上下文压缩、引用来源
- Prompt 治理：版本管理、变量校验、防注入、角色分层
- 工具调用：Tool Registry、参数 schema、权限边界、调用日志
- Skill 工作流：可复用 AI 任务、允许工具绑定、同步和异步执行
- 异步可靠性：RocketMQ、任务状态机、重试、死信、幂等
- 并发能力：JDK 21 虚拟线程、异步召回、线程池隔离、超时控制

## 12. 验收标准

第一阶段完成时，至少满足以下标准：

- 能通过 Docker Compose 启动依赖环境。
- 能创建知识库并上传文档。
- 文档能异步完成解析、切分、Embedding 和向量入库。
- 能基于指定知识库进行 RAG 问答。
- 回答能返回引用来源。
- Prompt Engine 能从模板渲染提示词，并校验变量。
- 至少三个工具能通过 Tool Registry 注册和执行。
- 至少两个 Skill 能被调用，其中一个支持异步执行。
- RocketMQ 消费失败能更新任务状态，并具备重试或死信处理路径。
- SSE 流式问答接口可用。
- 核心模块具备必要测试。
