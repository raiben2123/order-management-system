package com.ruben.ruben.controller;

import com.ruben.ruben.dto.CreateProductRequest;
import com.ruben.ruben.dto.ProductResponse;
import com.ruben.ruben.dto.UpdateStockRequest;
import com.ruben.ruben.mapper.ProductMapper;
import com.ruben.ruben.model.Product;
import com.ruben.ruben.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final ProductMapper productMapper;

    public ProductController(ProductService productService, ProductMapper productMapper) {
        this.productService = productService;
        this.productMapper = productMapper;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        Product product = productService.createProduct(
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getStockQuantity()
        );
        ProductResponse response = productMapper.toResponse(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        List<Product> products = productService.getAllProducts();
        List<ProductResponse> response = productMapper.toResponseList(products);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable String productId) {
        Product product = productService.getProductByProductId(productId);
        ProductResponse response = productMapper.toResponse(product);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{productId}/stock")
    public ResponseEntity<ProductResponse> updateStock(
            @PathVariable String productId,
            @Valid @RequestBody UpdateStockRequest request) {

        Product product = productService.updateStock(productId, request.getQuantity());
        ProductResponse response = productMapper.toResponse(product);
        return ResponseEntity.ok(response);
    }
}