package com.ruben.ruben.service;

import com.ruben.ruben.client.InventoryClient;
import com.ruben.ruben.dto.CreateOrderRequest;
import com.ruben.ruben.event.OrderCreatedEvent;
import com.ruben.ruben.exception.OrderNotFoundException;
import com.ruben.ruben.kafka.OrderEventProducer;
import com.ruben.ruben.model.Order;
import com.ruben.ruben.model.OrderItem;
import com.ruben.ruben.repository.OrderItemRepository;
import com.ruben.ruben.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderEventProducer eventProducer;
    private final InventoryClient inventoryClient;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        OrderEventProducer eventProducer, InventoryClient inventoryClient) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.eventProducer = eventProducer;
        this.inventoryClient = inventoryClient;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {

        for (CreateOrderRequest.OrderItemRequest item : request.getItems()) {
            boolean stockAvailable = inventoryClient.checkStock(item.getProductId());

            if (!stockAvailable) {
                throw new IllegalStateException(
                        "Cannot create order: Product " + item.getProductId() +
                                " is not available (Inventory service down or out of stock)"
                );
            }
        }

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setCustomerId(request.getCustomerId());
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        BigDecimal totalAmount = request.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        List<OrderItem> orderItems = request.getItems().stream()
                .map(itemReq -> {
                    OrderItem item = new OrderItem();
                    item.setOrderNumber(savedOrder.getOrderNumber());
                    item.setProductId(itemReq.getProductId());
                    item.setQuantity(itemReq.getQuantity());
                    item.setUnitPrice(itemReq.getUnitPrice());
                    item.setTotalPrice(itemReq.getUnitPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())));
                    return item;
                })
                .collect(Collectors.toList());

        orderItemRepository.saveAll(orderItems);

        List<OrderCreatedEvent.OrderItemEvent> itemEvents = orderItems.stream()
                .map(item -> new OrderCreatedEvent.OrderItemEvent(
                        item.getProductId(),
                        item.getQuantity(),
                        item.getUnitPrice()
                ))
                .collect(Collectors.toList());

        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getOrderNumber(),
                savedOrder.getCustomerId(),
                itemEvents,
                savedOrder.getTotalAmount(),
                savedOrder.getStatus(),
                savedOrder.getCreatedAt()
        );

        eventProducer.sendOrderCreatedEvent(event);

        return savedOrder;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderNumber));
    }

    @Transactional
    public Order updateOrderStatus(String orderNumber, String newStatus) {
        Order order = getOrderByNumber(orderNumber);
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}