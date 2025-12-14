package com.ruben.ruben.service;

import com.ruben.ruben.exception.ProductNotFoundException;
import com.ruben.ruben.model.Product;
import com.ruben.ruben.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public Product createProduct(String name, String description, BigDecimal price, Integer stockQuantity) {
        Product product = new Product();
        product.setProductId(generateProductId());
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setStockQuantity(stockQuantity);
        product.setReservedQuantity(0);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());

        return productRepository.save(product);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductByProductId(String productId) {
        return productRepository.findByProductId(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + productId));
    }

    @Transactional
    public Product updateStock(String productId, Integer quantity) {
        Product product = getProductByProductId(productId);
        int newStockQuantity = product.getStockQuantity() + quantity;

        int availableStock = newStockQuantity - product.getReservedQuantity();
        if (availableStock < 0) {
            throw new IllegalArgumentException(
                    "Cannot reduce stock below reserved quantity. Available: " +
                            (product.getStockQuantity() - product.getReservedQuantity()) +
                            ", Trying to reduce by: " + Math.abs(quantity)
            );
        }

        product.setStockQuantity(newStockQuantity);
        product.setUpdatedAt(LocalDateTime.now());
        return productRepository.save(product);
    }

    private String generateProductId() {
        return "PROD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}