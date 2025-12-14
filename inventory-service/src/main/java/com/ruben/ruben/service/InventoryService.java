package com.ruben.ruben.service;

import com.ruben.ruben.exception.InsufficientStockException;
import com.ruben.ruben.exception.ReservationNotFoundException;
import com.ruben.ruben.model.InventoryReservation;
import com.ruben.ruben.model.Product;
import com.ruben.ruben.repository.InventoryReservationRepository;
import com.ruben.ruben.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class InventoryService {

    private final InventoryReservationRepository inventoryReservationRepository;
    private final ProductService productService;
    private final ProductRepository productRepository;

    public InventoryService(
            InventoryReservationRepository inventoryReservationRepository,
            ProductService productService,
            ProductRepository productRepository) {
        this.inventoryReservationRepository = inventoryReservationRepository;
        this.productService = productService;
        this.productRepository = productRepository;
    }

    @Transactional
    public InventoryReservation reserveStock(String orderNumber, String productId, Integer quantity) {
        Product product = productService.getProductByProductId(productId);

        int availableStock = product.getStockQuantity() - product.getReservedQuantity();

        if (availableStock < quantity) {
            throw new InsufficientStockException(
                    "Insufficient stock for product " + productId +
                            ". Available: " + availableStock +
                            ", Requested: " + quantity
            );
        }

        product.setReservedQuantity(product.getReservedQuantity() + quantity);
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);

        InventoryReservation reservation = new InventoryReservation();
        reservation.setReservationId(generateReservationId());
        reservation.setOrderNumber(orderNumber);
        reservation.setProductId(productId);
        reservation.setQuantity(quantity);
        reservation.setStatus("RESERVED");
        reservation.setCreatedAt(LocalDateTime.now());

        return inventoryReservationRepository.save(reservation);
    }

    @Transactional
    public void confirmReservation(String reservationId) {
        InventoryReservation reservation = inventoryReservationRepository
                .findByReservationId(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found: " + reservationId));

        reservation.setStatus("CONFIRMED");
        inventoryReservationRepository.save(reservation);

        Product product = productService.getProductByProductId(reservation.getProductId());
        product.setStockQuantity(product.getStockQuantity() - reservation.getQuantity());
        product.setReservedQuantity(product.getReservedQuantity() - reservation.getQuantity());
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);
    }

    @Transactional
    public void cancelReservation(String reservationId) {
        InventoryReservation reservation = inventoryReservationRepository
                .findByReservationId(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found: " + reservationId));

        reservation.setStatus("CANCELLED");
        inventoryReservationRepository.save(reservation);

        Product product = productService.getProductByProductId(reservation.getProductId());
        product.setReservedQuantity(product.getReservedQuantity() - reservation.getQuantity());
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);
    }

    private String generateReservationId() {
        return "RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}