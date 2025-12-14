package com.ruben.ruben.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruben.ruben.event.InventoryReservedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class InventoryEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(InventoryEventProducer.class);
    private static final String TOPIC = "inventory-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public InventoryEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishInventoryReservedEvent(InventoryReservedEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, event.getOrderNumber(), eventJson);

            logger.info("📤 Inventory event published - OrderNumber: {}, Success: {}",
                    event.getOrderNumber(),
                    event.getSuccess());

        } catch (Exception e) {
            logger.error("❌ Error publishing inventory event", e);
        }
    }
}