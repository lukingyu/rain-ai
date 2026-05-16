package com.rain.ai.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rain.ai.knowledge.DocumentIngestionMessage;
import com.rain.ai.knowledge.DocumentIngestionProcessor;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

@Configuration
public class DocumentIngestionConsumerConfig {

    @Bean(destroyMethod = "shutdown")
    public DefaultMQPushConsumer documentIngestionConsumer(
            RocketMqProperties properties,
            ObjectMapper objectMapper,
            DocumentIngestionProcessor processor
    ) throws Exception {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(properties.documentConsumerGroup());
        consumer.setNamesrvAddr(properties.nameServer());
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        consumer.subscribe(properties.topics().documentIngestion(), "*");
        MessageListenerConcurrently listener = (messages, context) -> {
            try {
                for (org.apache.rocketmq.common.message.MessageExt message : messages) {
                    String json = new String(message.getBody(), StandardCharsets.UTF_8);
                    processor.process(objectMapper.readValue(json, DocumentIngestionMessage.class));
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            } catch (Exception exception) {
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
        };
        consumer.registerMessageListener(listener);
        consumer.start();
        return consumer;
    }
}
