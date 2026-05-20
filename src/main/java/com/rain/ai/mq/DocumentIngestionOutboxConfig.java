package com.rain.ai.mq;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableScheduling
public class DocumentIngestionOutboxConfig {

    @Bean(destroyMethod = "close")
    public ExecutorService documentIngestionOutboxExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
