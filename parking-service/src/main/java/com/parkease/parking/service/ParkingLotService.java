package com.parkease.parking.service;

import com.parkease.parking.web.dto.request.CreateLotRequest;
import com.parkease.parking.web.dto.request.UpdateLotRequest;
import com.parkease.parking.web.dto.response.ParkingLotResponse;

import java.util.List;

public interface ParkingLotService {
    ParkingLotResponse createLot(Long managerId, CreateLotRequest request);
    ParkingLotResponse updateLot(Long lotId, Long managerId, UpdateLotRequest request);
    ParkingLotResponse getLotById(Long lotId);
    List<ParkingLotResponse> getLotsByManager(Long managerId);
    List<ParkingLotResponse> getLotsByCity(String city);
    List<ParkingLotResponse> getNearbyLots(double lat, double lng, double radiusKm);
    ParkingLotResponse toggleLotStatus(Long lotId, Long managerId);
    void approveLot(Long lotId);
    void rejectLot(Long lotId);
    List<ParkingLotResponse> getPendingLots();
    List<ParkingLotResponse> getAllLots();
    List<ParkingLotResponse> getApprovedLots();
}
