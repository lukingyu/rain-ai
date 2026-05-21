package com.rain.ai.knowledge;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class DocumentReingestConfig {

    @Bean(destroyMethod = "close")
    public ExecutorService documentReingestExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
