package com.ruben.ruben.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic inventoryEventsTopic() {
        return new NewTopic("inventory-events", 1, (short) 1);
    }
}