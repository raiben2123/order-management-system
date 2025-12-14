package com.ruben.ruben.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruben.ruben.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventProducer.class);
    private static final String TOPIC = "order-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OrderEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendOrderCreatedEvent(OrderCreatedEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, event.getOrderNumber(), eventJson);
            logger.info("📤 Order event published - OrderNumber: {}, Items: {}",
                    event.getOrderNumber(),
                    event.getItems().size());
        } catch (Exception e) {
            logger.error("❌ Error publishing order event", e);
        }
    }
}