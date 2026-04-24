package com.parkease.auth.service;

import com.parkease.auth.web.dto.request.AddVehicleRequest;
import com.parkease.auth.web.dto.response.VehicleResponse;

import java.util.List;

public interface VehicleService {

    VehicleResponse addVehicle(Long userId, AddVehicleRequest request);

    List<VehicleResponse> getMyVehicles(Long userId);

    void deleteVehicle(Long userId, Long vehicleId);

    VehicleResponse getByPlate(String plate);
}
