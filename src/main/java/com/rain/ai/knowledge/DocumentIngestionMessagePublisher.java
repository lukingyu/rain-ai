package com.rain.ai.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rain.ai.mq.RocketMqProperties;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class DocumentIngestionMessagePublisher {

    private final DefaultMQProducer producer;
    private final RocketMqProperties properties;
    private final ObjectMapper objectMapper;

    public DocumentIngestionMessagePublisher(
            DefaultMQProducer producer,
            RocketMqProperties properties,
            ObjectMapper objectMapper
    ) {
        this.producer = producer;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public void publish(DocumentIngestionMessage message) {
        try {
            byte[] body = objectMapper.writeValueAsString(message).getBytes(StandardCharsets.UTF_8);
            Message rocketMessage = new Message(
                    properties.topics().documentIngestion(),
                    "DOCUMENT_UPLOADED",
                    message.documentId().toString(),
                    body
            );
            producer.send(rocketMessage);
        } catch (Exception exception) {
            throw new IllegalStateException("投递文档摄取消息失败", exception);
        }
    }
}
