package com.ruben.ruben.repository;

import com.ruben.ruben.model.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {
    Optional<InventoryReservation> findByReservationId(String reservationId);
    List<InventoryReservation> findByOrderNumber(String reservationId);
    List<InventoryReservation> findByStatus(String status);
}
