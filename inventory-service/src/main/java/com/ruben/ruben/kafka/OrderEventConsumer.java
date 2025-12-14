package com.ruben.ruben.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruben.ruben.event.InventoryReservedEvent;
import com.ruben.ruben.event.OrderCreatedEvent;
import com.ruben.ruben.exception.InsufficientStockException;
import com.ruben.ruben.model.InventoryReservation;
import com.ruben.ruben.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final InventoryService inventoryService;
    private final InventoryEventProducer inventoryEventProducer;
    private final ObjectMapper objectMapper;

    public OrderEventConsumer(
            InventoryService inventoryService,
            InventoryEventProducer inventoryEventProducer,
            ObjectMapper objectMapper) {
        this.inventoryService = inventoryService;
        this.inventoryEventProducer = inventoryEventProducer;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "order-events", groupId = "inventory-service-group")
    public void consumeOrderEvent(String message) {
        try {
            OrderCreatedEvent orderEvent = objectMapper.readValue(message, OrderCreatedEvent.class);

            logger.info("📦 Order received - OrderNumber: {}, Items: {}",
                    orderEvent.getOrderNumber(),
                    orderEvent.getItems().size());

            List<InventoryReservedEvent.ReservationDetail> reservations = new ArrayList<>();
            boolean allReserved = true;
            String failureMessage = null;

            // Intentar reservar cada item
            for (OrderCreatedEvent.OrderItemEvent item : orderEvent.getItems()) {
                try {
                    InventoryReservation reservation = inventoryService.reserveStock(
                            orderEvent.getOrderNumber(),
                            item.getProductId(),
                            item.getQuantity()
                    );

                    reservations.add(new InventoryReservedEvent.ReservationDetail(
                            reservation.getReservationId(),
                            item.getProductId(),
                            item.getQuantity()
                    ));

                    logger.info("✅ Reserved {} units of {} - ReservationId: {}",
                            item.getQuantity(),
                            item.getProductId(),
                            reservation.getReservationId());

                } catch (InsufficientStockException e) {
                    allReserved = false;
                    failureMessage = e.getMessage();
                    logger.warn("⚠️ Insufficient stock for {}: {}", item.getProductId(), e.getMessage());

                    // Cancelar reservas anteriores
                    for (InventoryReservedEvent.ReservationDetail detail : reservations) {
                        inventoryService.cancelReservation(detail.getReservationId());
                        logger.info("🔄 Cancelled reservation: {}", detail.getReservationId());
                    }

                    break;
                }
            }

            // Publicar resultado
            InventoryReservedEvent event = new InventoryReservedEvent(
                    orderEvent.getOrderNumber(),
                    allReserved,
                    allReserved ? "All items reserved successfully" : failureMessage,
                    allReserved ? reservations : null,
                    LocalDateTime.now()
            );

            inventoryEventProducer.publishInventoryReservedEvent(event);

        } catch (Exception e) {
            logger.error("❌ Error processing order event: {}", message, e);
        }
    }
}