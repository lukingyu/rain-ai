package com.rain.ai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        properties = {
                "spring.ai.openai.api-key=test-key",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.jdbc.JdbcRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.jdbc.JdbcClientAutoConfiguration,"
                        + "org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration",
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
