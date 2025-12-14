package com.ruben.ruben.controller;

import com.ruben.ruben.dto.CreateOrderRequest;
import com.ruben.ruben.dto.OrderResponse;
import com.ruben.ruben.mapper.OrderMapper;
import com.ruben.ruben.model.Order;
import com.ruben.ruben.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private OrderMapper orderMapper;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setOrderNumber("ORD-12345678");
        testOrder.setCustomerId("CUST-001");
        testOrder.setTotalAmount(new BigDecimal("99.99"));
        testOrder.setStatus("PENDING");
        testOrder.setCreatedAt(LocalDateTime.now());
        testOrder.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void createOrder_ShouldReturnCreatedOrder() throws Exception {
        // Arrange
        CreateOrderRequest.OrderItemRequest itemRequest = new CreateOrderRequest.OrderItemRequest();
        itemRequest.setProductId("PROD-001");
        itemRequest.setQuantity(1);
        itemRequest.setUnitPrice(new BigDecimal("99.99"));

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId("CUST-001");
        request.setItems(Arrays.asList(itemRequest));

        OrderResponse response = new OrderResponse(
                "ORD-12345678",
                "CUST-001",
                new BigDecimal("99.99"),
                "PENDING",
                LocalDateTime.now()
        );

        when(orderService.createOrder(any(CreateOrderRequest.class)))
                .thenReturn(testOrder);
        when(orderMapper.toResponse(testOrder)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value("ORD-12345678"))
                .andExpect(jsonPath("$.customerId").value("CUST-001"))
                .andExpect(jsonPath("$.totalAmount").value(99.99))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(orderService, times(1)).createOrder(any(CreateOrderRequest.class));
        verify(orderMapper, times(1)).toResponse(testOrder);
    }

    @Test
    void getAllOrders_ShouldReturnListOfOrders() throws Exception {
        // Arrange
        OrderResponse response = new OrderResponse(
                "ORD-12345678",
                "CUST-001",
                new BigDecimal("99.99"),
                "PENDING",
                LocalDateTime.now()
        );

        when(orderService.getAllOrders()).thenReturn(Arrays.asList(testOrder));
        when(orderMapper.toResponseList(Arrays.asList(testOrder)))
                .thenReturn(Arrays.asList(response));

        // Act & Assert
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderNumber").value("ORD-12345678"))
                .andExpect(jsonPath("$[0].customerId").value("CUST-001"));

        verify(orderService, times(1)).getAllOrders();
        verify(orderMapper, times(1)).toResponseList(any());
    }

    @Test
    void getOrderByNumber_ShouldReturnOrder() throws Exception {
        // Arrange
        OrderResponse response = new OrderResponse(
                "ORD-12345678",
                "CUST-001",
                new BigDecimal("99.99"),
                "PENDING",
                LocalDateTime.now()
        );

        when(orderService.getOrderByNumber("ORD-12345678")).thenReturn(testOrder);
        when(orderMapper.toResponse(testOrder)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/orders/ORD-12345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("ORD-12345678"))
                .andExpect(jsonPath("$.customerId").value("CUST-001"));

        verify(orderService, times(1)).getOrderByNumber("ORD-12345678");
        verify(orderMapper, times(1)).toResponse(testOrder);
    }
}