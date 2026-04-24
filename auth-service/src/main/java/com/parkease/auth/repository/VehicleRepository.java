package com.parkease.auth.repository;

import com.parkease.auth.domain.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByUserId(Long userId);

    Optional<Vehicle> findByPlateIgnoreCase(String plate);

    boolean existsByUserIdAndPlateIgnoreCase(Long userId, String plate);
}
