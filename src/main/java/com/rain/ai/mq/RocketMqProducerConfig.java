package com.rain.ai.mq;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RocketMqProperties.class)
public class RocketMqProducerConfig {

    @Bean(destroyMethod = "shutdown")
    public DefaultMQProducer defaultMQProducer(RocketMqProperties properties) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer(properties.producerGroup());
        producer.setNamesrvAddr(properties.nameServer());
        producer.setVipChannelEnabled(false);
        producer.setSendMsgTimeout(5000);
        producer.start();
        return producer;
    }
}
