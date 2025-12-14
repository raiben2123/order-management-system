package com.ruben.ruben.service;

import com.ruben.ruben.dto.CreateOrderRequest;
import com.ruben.ruben.kafka.OrderEventProducer;
import com.ruben.ruben.model.Order;
import com.ruben.ruben.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventProducer eventProducer;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setOrderNumber("ORD-12345678");
        testOrder.setCustomerId("CUST-001");
        testOrder.setTotalAmount(new BigDecimal("99.99"));
        testOrder.setStatus("PENDING");
    }

    @Test
    void createOrder_ShouldReturnSaveOrder() {
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId("CUST-001");

        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId("PROD-001");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("99.99"));

        request.setItems(Arrays.asList(item));

        Order order = orderService.createOrder(request);

        assertNotNull(order);
        assertEquals("CUST-001", order.getCustomerId());
        assertEquals(new BigDecimal("99.99"), order.getTotalAmount());
        assertEquals("PENDING", order.getStatus());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void getAllOrders_ShouldReturnListOfOrders() {
        //Arrange
        List<Order> orders = Arrays.asList(testOrder);
        when(orderRepository.findAll()).thenReturn(orders);

        //Act
        List<Order> result = orderService.getAllOrders();

        //Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    void getOrderByNumber_WhenExists_ShouldReturnOrder() {
        //Arrange
        when(orderRepository.findByOrderNumber("ORD-12345678")).thenReturn(Optional.of(testOrder));

        //Act
        Order result = orderService.getOrderByNumber("ORD-12345678");

        //Assert
        assertNotNull(result);
        assertEquals("ORD-12345678", result.getOrderNumber());
        verify(orderRepository, times(1)).findByOrderNumber("ORD-12345678");
    }

    @Test
    void getOrderByNumber_WhenNotExists_ShouldThrowException() {
        //Arrange
        when(orderRepository.findByOrderNumber("ORD-99999999")).thenReturn(Optional.empty());

        //Act & Assert
        assertThrows(RuntimeException.class, () -> {
            orderService.getOrderByNumber("ORD-99999999");
        });
        verify(orderRepository, times(1)).findByOrderNumber("ORD-99999999");
    }
}
