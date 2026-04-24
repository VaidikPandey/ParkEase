package com.parkease.auth.service.impl;

import com.parkease.auth.domain.entity.Vehicle;
import com.parkease.auth.repository.VehicleRepository;
import com.parkease.auth.service.VehicleService;
import com.parkease.auth.web.dto.request.AddVehicleRequest;
import com.parkease.auth.web.dto.response.VehicleResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;

    @Override
    public VehicleResponse addVehicle(Long userId, AddVehicleRequest request) {
        String plate = request.getPlate().toUpperCase();

        if (vehicleRepository.existsByUserIdAndPlateIgnoreCase(userId, plate)) {
            throw new IllegalStateException("Vehicle with plate " + plate + " already registered");
        }

        Vehicle vehicle = Vehicle.builder()
                .userId(userId)
                .plate(plate)
                .vehicleType(request.getVehicleType())
                .isEv(request.isEv())
                .nickname(request.getNickname())
                .build();

        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> getMyVehicles(Long userId) {
        return vehicleRepository.findByUserId(userId)
                .stream()
                .map(VehicleResponse::from)
                .toList();
    }

    @Override
    public void deleteVehicle(Long userId, Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new EntityNotFoundException("Vehicle not found: id=" + vehicleId));

        if (!vehicle.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You do not own this vehicle");
        }

        vehicleRepository.delete(vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse getByPlate(String plate) {
        return vehicleRepository.findByPlateIgnoreCase(plate)
                .map(VehicleResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("No vehicle found with plate: " + plate));
    }
}
