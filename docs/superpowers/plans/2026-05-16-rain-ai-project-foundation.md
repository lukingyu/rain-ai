# Rain AI 项目骨架与基础设施 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建 Rain AI Agent Platform 的第一块可运行后端基础：Spring Boot 3.5 + JDK 21 项目骨架、统一响应与异常、健康检查接口、Docker Compose 基础设施配置。

**Architecture:** 第一阶段采用单体 Spring Boot 项目，按包名表达模块边界，不做 Maven 多模块拆分。基础设施先接入 PostgreSQL/pgvector、Redis、RocketMQ 的容器配置，应用侧先完成可启动、可测试、可扩展的最小骨架。

**Tech Stack:** JDK 21、Spring Boot 3.5.14、Spring AI 1.1.6、Maven、JUnit 5、PostgreSQL/pgvector、Redis、RocketMQ 5.3.2、Docker Compose。

---

## 版本依据

- Spring Boot 官方文档在 2026-05-16 显示稳定版本包含 `3.5.14`。
- Spring AI 官方文档在 2026-05-16 显示当前版本为 `1.1.6`，并说明支持 Spring Boot `3.4.x` 和 `3.5.x`。
- RocketMQ 官方 Docker 快速开始文档使用 `apache/rocketmq:5.3.2` 作为示例镜像。

## 文件结构

本计划创建以下文件：

- `.gitignore`：忽略 Java、Maven、IDE、日志和本地环境文件。
- `.env.example`：提供本地基础设施默认环境变量。
- `README.md`：说明项目定位、第一阶段能力和本地启动命令。
- `pom.xml`：声明 JDK 21、Spring Boot、Spring AI、数据库、Redis、测试依赖。
- `docker-compose.yml`：定义 PostgreSQL/pgvector、Redis、RocketMQ namesrv、RocketMQ broker。
- `src/main/java/com/rain/ai/RainAiApplication.java`：Spring Boot 启动类。
- `src/main/java/com/rain/ai/api/HealthCheckController.java`：健康检查接口。
- `src/main/java/com/rain/ai/common/api/ApiResponse.java`：统一响应结构。
- `src/main/java/com/rain/ai/common/exception/ErrorCode.java`：业务错误码枚举。
- `src/main/java/com/rain/ai/common/exception/BizException.java`：业务异常。
- `src/main/java/com/rain/ai/common/exception/GlobalExceptionHandler.java`：统一异常处理器。
- `src/main/resources/application.yml`：应用配置、数据源、Redis、虚拟线程、AI 基础配置。
- `src/test/java/com/rain/ai/RainAiApplicationTests.java`：应用上下文启动测试。
- `src/test/java/com/rain/ai/api/HealthCheckControllerTest.java`：健康检查接口测试。

本计划不接入真实模型调用，不创建知识库表，不写 RAG 逻辑。那些内容会在后续独立计划中实现。

---

### Task 1: 创建 Maven 项目基础文件

**Files:**
- Create: `.gitignore`
- Create: `.env.example`
- Create: `pom.xml`
- Create: `README.md`

- [ ] **Step 1: 写入 `.gitignore`**

创建 `.gitignore`，内容如下：

```gitignore
target/
*.log
*.tmp
*.swp

.idea/
*.iml
.vscode/

.env
.DS_Store
Thumbs.db

logs/
data/
```

- [ ] **Step 2: 写入 `.env.example`**

创建 `.env.example`，内容如下：

```dotenv
POSTGRES_DB=rain_ai
POSTGRES_USER=rain
POSTGRES_PASSWORD=rain_pwd
POSTGRES_PORT=5432

REDIS_PORT=6379

ROCKETMQ_NAMESRV_PORT=9876
ROCKETMQ_BROKER_PORT=10911

OPENAI_API_KEY=replace-with-your-api-key
OPENAI_BASE_URL=https://api.openai.com
```

- [ ] **Step 3: 写入 `pom.xml`**

创建 `pom.xml`，内容如下：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.14</version>
        <relativePath/>
    </parent>

    <groupId>com.rain</groupId>
    <artifactId>rain-ai</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <name>rain-ai</name>
    <description>企业知识库与智能任务执行 AI 后端平台</description>

    <properties>
        <java.version>21</java.version>
        <spring-ai.version>1.1.6</spring-ai.version>
        <rocketmq.version>5.3.2</rocketmq.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>
                <version>${spring-ai.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jdbc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-openai</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-vector-store-pgvector</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.rocketmq</groupId>
            <artifactId>rocketmq-client-java</artifactId>
            <version>${rocketmq.version}</version>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <release>21</release>
                    <parameters>true</parameters>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 4: 写入 `README.md`**

创建 `README.md`，内容如下：

```markdown
# Rain AI Agent Platform

Rain AI Agent Platform 是一个企业知识库与智能任务执行 AI 后端平台。

第一阶段目标：

- 构建 Spring Boot 3.5 + JDK 21 后端骨架。
- 启动 PostgreSQL/pgvector、Redis、RocketMQ 本地依赖。
- 建立统一响应、统一异常和健康检查接口。
- 为后续 RAG、Prompt Engine、Tool Registry、Skill 和异步任务编排打基础。

## 本地环境

- JDK 21
- Maven 3.9+
- Docker Desktop

## 启动依赖

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

健康检查：

```bash
curl http://localhost:8080/api/health
```
```

- [ ] **Step 5: 验证 Maven 模型能被解析**

Run:

```bash
mvn -q help:effective-pom -DskipTests
```

Expected: 命令退出码为 `0`，输出不包含 `Non-resolvable parent POM` 或 `Could not find artifact`。

- [ ] **Step 6: 提交基础文件**

Run:

```bash
git add .gitignore .env.example pom.xml README.md
git commit -m "初始化项目基础文件"
```

Expected: Git 生成一个包含 4 个文件的提交。

---

### Task 2: 创建 Spring Boot 启动类和应用配置

**Files:**
- Create: `src/main/java/com/rain/ai/RainAiApplication.java`
- Create: `src/main/resources/application.yml`
- Test: `src/test/java/com/rain/ai/RainAiApplicationTests.java`

- [ ] **Step 1: 写入失败测试**

创建 `src/test/java/com/rain/ai/RainAiApplicationTests.java`：

```java
package com.rain.ai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        properties = {
                "spring.ai.openai.api-key=test-key",
                "spring.datasource.url=jdbc:postgresql://localhost:5432/rain_ai",
                "spring.datasource.username=rain",
                "spring.datasource.password=rain_pwd"
        }
)
class RainAiApplicationTests {

    @Test
    void 应用上下文可以启动() {
    }
}
```

- [ ] **Step 2: 运行测试并确认失败**

Run:

```bash
mvn -q test -Dtest=RainAiApplicationTests
```

Expected: FAIL，错误中包含 `ClassNotFoundException` 或找不到 `com.rain.ai.RainAiApplication`。

- [ ] **Step 3: 写入启动类**

创建 `src/main/java/com/rain/ai/RainAiApplication.java`：

```java
package com.rain.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RainAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(RainAiApplication.class, args);
    }
}
```

- [ ] **Step 4: 写入 `application.yml`**

创建 `src/main/resources/application.yml`：

```yaml
server:
  port: 8080

spring:
  application:
    name: rain-ai
  threads:
    virtual:
      enabled: true
  datasource:
    url: jdbc:postgresql://localhost:5432/rain_ai
    username: rain
    password: rain_pwd
    driver-class-name: org.postgresql.Driver
  data:
    redis:
      host: localhost
      port: 6379
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:replace-with-your-api-key}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com}

rain:
  ai:
    workspace:
      default-id: default-workspace
    task:
      default-timeout-seconds: 120
```

- [ ] **Step 5: 运行测试并确认通过**

Run:

```bash
mvn -q test -Dtest=RainAiApplicationTests
```

Expected: PASS，退出码为 `0`。

- [ ] **Step 6: 提交启动骨架**

Run:

```bash
git add src/main/java/com/rain/ai/RainAiApplication.java src/main/resources/application.yml src/test/java/com/rain/ai/RainAiApplicationTests.java
git commit -m "创建 Spring Boot 启动骨架"
```

Expected: Git 生成一个包含启动类、配置和测试的提交。

---

### Task 3: 实现统一响应结构

**Files:**
- Create: `src/main/java/com/rain/ai/common/api/ApiResponse.java`
- Test: `src/test/java/com/rain/ai/common/api/ApiResponseTest.java`

- [ ] **Step 1: 写入失败测试**

创建 `src/test/java/com/rain/ai/common/api/ApiResponseTest.java`：

```java
package com.rain.ai.common.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void 成功响应包含数据和成功标记() {
        ApiResponse<String> response = ApiResponse.success("ok");

        assertThat(response.success()).isTrue();
        assertThat(response.code()).isEqualTo("成功");
        assertThat(response.message()).isEqualTo("请求成功");
        assertThat(response.data()).isEqualTo("ok");
    }

    @Test
    void 失败响应包含错误码和错误信息() {
        ApiResponse<Void> response = ApiResponse.failure("参数错误", "知识库名称不能为空");

        assertThat(response.success()).isFalse();
        assertThat(response.code()).isEqualTo("参数错误");
        assertThat(response.message()).isEqualTo("知识库名称不能为空");
        assertThat(response.data()).isNull();
    }
}
```

- [ ] **Step 2: 运行测试并确认失败**

Run:

```bash
mvn -q test -Dtest=ApiResponseTest
```

Expected: FAIL，错误中包含 `cannot find symbol class ApiResponse`。

- [ ] **Step 3: 写入统一响应结构**

创建 `src/main/java/com/rain/ai/common/api/ApiResponse.java`：

```java
package com.rain.ai.common.api;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "成功", "请求成功", data);
    }

    public static <T> ApiResponse<T> failure(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }
}
```

- [ ] **Step 4: 运行测试并确认通过**

Run:

```bash
mvn -q test -Dtest=ApiResponseTest
```

Expected: PASS，退出码为 `0`。

- [ ] **Step 5: 提交统一响应结构**

Run:

```bash
git add src/main/java/com/rain/ai/common/api/ApiResponse.java src/test/java/com/rain/ai/common/api/ApiResponseTest.java
git commit -m "实现统一响应结构"
```

Expected: Git 生成一个包含响应结构和测试的提交。

---

### Task 4: 实现统一异常处理

**Files:**
- Create: `src/main/java/com/rain/ai/common/exception/ErrorCode.java`
- Create: `src/main/java/com/rain/ai/common/exception/BizException.java`
- Create: `src/main/java/com/rain/ai/common/exception/GlobalExceptionHandler.java`
- Test: `src/test/java/com/rain/ai/common/exception/BizExceptionTest.java`

- [ ] **Step 1: 写入失败测试**

创建 `src/test/java/com/rain/ai/common/exception/BizExceptionTest.java`：

```java
package com.rain.ai.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BizExceptionTest {

    @Test
    void 业务异常携带错误码和消息() {
        BizException exception = new BizException(ErrorCode.参数错误, "知识库名称不能为空");

        assertThat(exception.errorCode()).isEqualTo(ErrorCode.参数错误);
        assertThat(exception.getMessage()).isEqualTo("知识库名称不能为空");
    }
}
```

- [ ] **Step 2: 运行测试并确认失败**

Run:

```bash
mvn -q test -Dtest=BizExceptionTest
```

Expected: FAIL，错误中包含 `cannot find symbol class BizException` 或 `cannot find symbol class ErrorCode`。

- [ ] **Step 3: 写入错误码枚举**

创建 `src/main/java/com/rain/ai/common/exception/ErrorCode.java`：

```java
package com.rain.ai.common.exception;

public enum ErrorCode {
    参数错误,
    资源不存在,
    外部服务失败,
    系统错误
}
```

- [ ] **Step 4: 写入业务异常**

创建 `src/main/java/com/rain/ai/common/exception/BizException.java`：

```java
package com.rain.ai.common.exception;

public class BizException extends RuntimeException {

    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
```

- [ ] **Step 5: 写入统一异常处理器**

创建 `src/main/java/com/rain/ai/common/exception/GlobalExceptionHandler.java`：

```java
package com.rain.ai.common.exception;

import com.rain.ai.common.api.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBizException(BizException exception) {
        return ApiResponse.failure(exception.errorCode().name(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("请求参数不合法");
        return ApiResponse.failure(ErrorCode.参数错误.name(), message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleConstraintViolationException(ConstraintViolationException exception) {
        return ApiResponse.failure(ErrorCode.参数错误.name(), exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception exception) {
        return ApiResponse.failure(ErrorCode.系统错误.name(), "系统处理失败");
    }
}
```

- [ ] **Step 6: 运行测试并确认通过**

Run:

```bash
mvn -q test -Dtest=BizExceptionTest
```

Expected: PASS，退出码为 `0`。

- [ ] **Step 7: 提交统一异常处理**

Run:

```bash
git add src/main/java/com/rain/ai/common/exception src/test/java/com/rain/ai/common/exception/BizExceptionTest.java
git commit -m "实现统一异常处理"
```

Expected: Git 生成一个包含异常类型、错误码、异常处理器和测试的提交。

---

### Task 5: 实现健康检查接口

**Files:**
- Create: `src/main/java/com/rain/ai/api/HealthCheckController.java`
- Test: `src/test/java/com/rain/ai/api/HealthCheckControllerTest.java`

- [ ] **Step 1: 写入失败测试**

创建 `src/test/java/com/rain/ai/api/HealthCheckControllerTest.java`：

```java
package com.rain.ai.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        properties = {
                "spring.ai.openai.api-key=test-key",
                "spring.datasource.url=jdbc:postgresql://localhost:5432/rain_ai",
                "spring.datasource.username=rain",
                "spring.datasource.password=rain_pwd"
        }
)
@AutoConfigureMockMvc
class HealthCheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 健康检查返回应用状态() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.application").value("rain-ai"));
    }
}
```

- [ ] **Step 2: 运行测试并确认失败**

Run:

```bash
mvn -q test -Dtest=HealthCheckControllerTest
```

Expected: FAIL，HTTP 状态为 `404`。

- [ ] **Step 3: 写入健康检查接口**

创建 `src/main/java/com/rain/ai/api/HealthCheckController.java`：

```java
package com.rain.ai.api;

import com.rain.ai.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthCheckController {

    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success(Map.of(
                "status", "UP",
                "application", "rain-ai",
                "timestamp", Instant.now().toString()
        ));
    }
}
```

- [ ] **Step 4: 运行测试并确认通过**

Run:

```bash
mvn -q test -Dtest=HealthCheckControllerTest
```

Expected: PASS，退出码为 `0`。

- [ ] **Step 5: 提交健康检查接口**

Run:

```bash
git add src/main/java/com/rain/ai/api/HealthCheckController.java src/test/java/com/rain/ai/api/HealthCheckControllerTest.java
git commit -m "实现健康检查接口"
```

Expected: Git 生成一个包含接口和测试的提交。

---

### Task 6: 配置 Docker Compose 基础设施

**Files:**
- Create: `docker-compose.yml`

- [ ] **Step 1: 写入 Docker Compose 配置**

创建 `docker-compose.yml`：

```yaml
services:
  postgres:
    image: pgvector/pgvector:pg17
    container_name: rain-ai-postgres
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-rain_ai}
      POSTGRES_USER: ${POSTGRES_USER:-rain}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-rain_pwd}
    ports:
      - "${POSTGRES_PORT:-5432}:5432"
    volumes:
      - rain-ai-postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-rain} -d ${POSTGRES_DB:-rain_ai}"]
      interval: 10s
      timeout: 5s
      retries: 10

  redis:
    image: redis:7.4-alpine
    container_name: rain-ai-redis
    ports:
      - "${REDIS_PORT:-6379}:6379"
    command: ["redis-server", "--appendonly", "yes"]
    volumes:
      - rain-ai-redis-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 10

  rocketmq-namesrv:
    image: apache/rocketmq:5.3.2
    container_name: rain-ai-rocketmq-namesrv
    command: ["sh", "mqnamesrv"]
    ports:
      - "${ROCKETMQ_NAMESRV_PORT:-9876}:9876"

  rocketmq-broker:
    image: apache/rocketmq:5.3.2
    container_name: rain-ai-rocketmq-broker
    depends_on:
      - rocketmq-namesrv
    environment:
      NAMESRV_ADDR: rocketmq-namesrv:9876
    command: ["sh", "mqbroker", "-n", "rocketmq-namesrv:9876", "autoCreateTopicEnable=true"]
    ports:
      - "${ROCKETMQ_BROKER_PORT:-10911}:10911"
    volumes:
      - rain-ai-rocketmq-store:/home/rocketmq/store

volumes:
  rain-ai-postgres-data:
  rain-ai-redis-data:
  rain-ai-rocketmq-store:
```

- [ ] **Step 2: 校验 Docker Compose 配置**

Run:

```bash
docker compose config
```

Expected: 命令退出码为 `0`，输出中包含 `rain-ai-postgres`、`rain-ai-redis`、`rain-ai-rocketmq-namesrv`、`rain-ai-rocketmq-broker`。

- [ ] **Step 3: 启动基础设施**

Run:

```bash
docker compose up -d
```

Expected: 命令退出码为 `0`，Docker 创建 4 个容器。

- [ ] **Step 4: 检查容器状态**

Run:

```bash
docker compose ps
```

Expected: 输出中 PostgreSQL、Redis、RocketMQ namesrv、RocketMQ broker 均为 `running` 或 `healthy`。

- [ ] **Step 5: 提交 Docker Compose 配置**

Run:

```bash
git add docker-compose.yml
git commit -m "配置本地基础设施容器"
```

Expected: Git 生成一个包含 Docker Compose 配置的提交。

---

### Task 7: 全量验证项目骨架

**Files:**
- Modify: `README.md`

- [ ] **Step 1: 运行全部测试**

Run:

```bash
mvn test
```

Expected: PASS，退出码为 `0`，测试包含：

- `RainAiApplicationTests`
- `ApiResponseTest`
- `BizExceptionTest`
- `HealthCheckControllerTest`

- [ ] **Step 2: 启动应用**

Run:

```bash
mvn spring-boot:run
```

Expected: 应用启动成功，日志中包含 `Started RainAiApplication`。

- [ ] **Step 3: 调用健康检查接口**

在另一个终端运行：

```bash
curl http://localhost:8080/api/health
```

Expected: 返回 JSON，结构如下：

```json
{
  "success": true,
  "code": "成功",
  "message": "请求成功",
  "data": {
    "status": "UP",
    "application": "rain-ai"
  }
}
```

响应中的 `data.timestamp` 是动态时间，不需要固定断言。

- [ ] **Step 4: 更新 README 的当前进度**

修改 `README.md`，在第一阶段目标后增加：

```markdown
## 当前进度

- 已完成 Spring Boot 3.5 + JDK 21 项目骨架。
- 已完成统一响应和统一异常处理。
- 已完成健康检查接口。
- 已完成 PostgreSQL/pgvector、Redis、RocketMQ 本地容器配置。
```

- [ ] **Step 5: 提交验证文档更新**

Run:

```bash
git add README.md
git commit -m "更新项目骨架进度说明"
```

Expected: Git 生成一个 README 更新提交。

---

## 自检清单

本计划覆盖设计文档中的第一阶段基础部分：

- Spring Boot 项目初始化：Task 1、Task 2
- Docker Compose 基础环境：Task 6
- PostgreSQL、pgvector、Redis、RocketMQ 接入准备：Task 1、Task 2、Task 6
- 统一异常、响应结构、配置管理：Task 3、Task 4
- 健康检查和启动验证：Task 5、Task 7
- 中文文档、中文注释、中文提交信息约定：Task 1、Task 3、Task 4、Task 7

未纳入本计划但已在设计文档中确认的后续独立计划：

- 知识库、文档、任务数据模型
- 文档上传和 RocketMQ 文档摄取
- 文档解析、切分、Embedding 和 pgvector 入库
- RAG Pipeline
- Prompt Engine
- Tool Registry
- Skill 执行模型
- SSE 流式回答
