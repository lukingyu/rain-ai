package com.rain.ai.config;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAiRagConfig {

    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return TokenTextSplitter.builder()
                .withChunkSize(420)
                .withMinChunkSizeChars(80)
                .withMinChunkLengthToEmbed(20)
                .withMaxNumChunks(10_000)
                .withKeepSeparator(true)
                .build();
    }
}
