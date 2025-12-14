package com.ruben.ruben.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservedEvent {
    private String orderNumber;
    private Boolean success;
    private String message;
    private List<ReservationDetail> reservations;
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservationDetail {
        private String reservationId;
        private String productId;
        private Integer quantity;
    }
}