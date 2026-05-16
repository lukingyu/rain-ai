package com.rain.ai.mq;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rain.ai.rocketmq")
public record RocketMqProperties(
        String nameServer,
        String producerGroup,
        String documentConsumerGroup,
        Topics topics
) {

    public record Topics(
            String documentIngestion
    ) {
    }
}
