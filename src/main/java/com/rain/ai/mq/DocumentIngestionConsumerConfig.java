package com.rain.ai.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rain.ai.knowledge.DocumentIngestionMessage;
import com.rain.ai.knowledge.DocumentIngestionProcessor;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

@Configuration
public class DocumentIngestionConsumerConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentIngestionConsumerConfig.class);

    @Bean(destroyMethod = "shutdown")
    public DefaultMQPushConsumer documentIngestionConsumer(
            RocketMqProperties properties,
            ObjectMapper objectMapper,
            DocumentIngestionProcessor processor
    ) throws Exception {
        RocketMqProperties.Consumer consumerProperties = properties.consumer();
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(properties.documentConsumerGroup());
        consumer.setNamesrvAddr(properties.nameServer());
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        consumer.setConsumeThreadMin(consumerProperties.consumeThreadMin());
        consumer.setConsumeThreadMax(consumerProperties.consumeThreadMax());
        consumer.setConsumeMessageBatchMaxSize(consumerProperties.batchSize());
        consumer.setMaxReconsumeTimes(consumerProperties.maxReconsumeTimes());
        consumer.subscribe(properties.topics().documentIngestion(), "*");
        MessageListenerConcurrently listener = (messages, context) -> {
            for (MessageExt message : messages) {
                ConsumeConcurrentlyStatus status = consumeOne(
                        message,
                        objectMapper,
                        processor,
                        consumerProperties.maxReconsumeTimes()
                );
                if (ConsumeConcurrentlyStatus.RECONSUME_LATER == status) {
                    return status;
                }
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        };
        consumer.registerMessageListener(listener);
        consumer.start();
        return consumer;
    }

    private ConsumeConcurrentlyStatus consumeOne(
            MessageExt message,
            ObjectMapper objectMapper,
            DocumentIngestionProcessor processor,
            int maxReconsumeTimes
    ) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            processor.process(objectMapper.readValue(json, DocumentIngestionMessage.class));
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        } catch (Exception exception) {
            if (message.getReconsumeTimes() >= maxReconsumeTimes) {
                LOGGER.warn(
                        "文档摄取消息达到最大重试次数，停止继续投递。topic={}, msgId={}, reconsumeTimes={}",
                        message.getTopic(),
                        message.getMsgId(),
                        message.getReconsumeTimes(),
                        exception
                );
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }

            LOGGER.warn(
                    "文档摄取消息消费失败，等待 RocketMQ 重新投递。topic={}, msgId={}, reconsumeTimes={}",
                    message.getTopic(),
                    message.getMsgId(),
                    message.getReconsumeTimes(),
                    exception
            );
            return ConsumeConcurrentlyStatus.RECONSUME_LATER;
        }
    }
}
