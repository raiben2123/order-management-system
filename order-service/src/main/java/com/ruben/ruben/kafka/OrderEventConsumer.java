package com.ruben.ruben.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruben.ruben.event.InventoryReservedEvent;
import com.ruben.ruben.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class OrderEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventConsumer.class);
    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public OrderEventConsumer(OrderService orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "inventory-events", groupId = "order-service-group")
    public void consumeInventoryEvent(String message) {
        try {
            InventoryReservedEvent event = objectMapper.readValue(message, InventoryReservedEvent.class);

            if (event.getSuccess()) {
                logger.info("✅ All items reserved - OrderNumber: {}", event.getOrderNumber());
                orderService.updateOrderStatus(event.getOrderNumber(), "CONFIRMED");

            } else {
                logger.warn("⚠️ Reservation failed - OrderNumber: {}, Reason: {}",
                        event.getOrderNumber(),
                        event.getMessage());
                orderService.updateOrderStatus(event.getOrderNumber(), "CANCELLED");
            }

        } catch (Exception e) {
            logger.error("❌ Error processing inventory event", e);
        }
    }
}