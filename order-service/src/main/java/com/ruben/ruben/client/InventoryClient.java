package com.ruben.ruben.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class InventoryClient {

    private static final Logger logger = LoggerFactory.getLogger(InventoryClient.class);
    private final RestTemplate restTemplate;

    public InventoryClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "checkStockFallback")
    @Retry(name = "inventoryService")
    public boolean checkStock(String productId) {
        logger.info("🔍 Checking stock for product: {}", productId);

        String url = "http://inventory-service:8082/api/products/" + productId;

        try {
            restTemplate.getForObject(url, Object.class);
            logger.info("✅ Product exists in inventory");
            return true;
        } catch (Exception e) {
            logger.error("❌ Error checking stock", e);
            throw new RuntimeException("Inventory service unavailable");
        }
    }

    public boolean checkStockFallback(String productId, Exception ex) {
        logger.warn("⚠️ Circuit breaker activated! Fallback response. Reason: {}", ex.getMessage());
        return false;
    }
}