package com.rain.ai.mq;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rain.ai.rocketmq")
public record RocketMqProperties(
        String nameServer,
        String producerGroup,
        String documentConsumerGroup,
        Topics topics,
        Consumer consumer
) {

    public Consumer consumer() {
        if (consumer == null) {
            return Consumer.defaults();
        }
        return consumer.normalized();
    }

    public record Topics(
            String documentIngestion
    ) {
    }

    public record Consumer(
            int consumeThreadMin,
            int consumeThreadMax,
            int batchSize,
            int maxReconsumeTimes
    ) {

        private static Consumer defaults() {
            return new Consumer(2, 8, 1, 3);
        }

        private Consumer normalized() {
            int minThreadCount = Math.max(consumeThreadMin, 1);
            int maxThreadCount = Math.max(consumeThreadMax, minThreadCount);
            int messageBatchSize = Math.max(batchSize, 1);
            int retryCount = Math.max(maxReconsumeTimes, 1);
            return new Consumer(minThreadCount, maxThreadCount, messageBatchSize, retryCount);
        }
    }
}
