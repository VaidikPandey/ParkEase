package com.parkease.parking.service;

import com.parkease.parking.domain.entity.ParkingSpot;
import com.parkease.parking.web.dto.request.BulkCreateSpotRequest;
import com.parkease.parking.web.dto.request.CreateSpotRequest;
import com.parkease.parking.web.dto.request.UpdateSpotRequest;
import com.parkease.parking.web.dto.response.ParkingSpotResponse;

import java.util.List;

public interface ParkingSpotService {
    ParkingSpotResponse addSpot(Long lotId, Long managerId, CreateSpotRequest request);
    List<ParkingSpotResponse> bulkAddSpots(Long lotId, Long managerId, BulkCreateSpotRequest request);
    ParkingSpotResponse updateSpot(Long spotId, Long managerId, UpdateSpotRequest request);
    void deleteSpot(Long spotId, Long managerId);
    List<ParkingSpotResponse> getSpotsByLot(Long lotId);
    List<ParkingSpotResponse> getAvailableSpots(Long lotId);
    List<ParkingSpotResponse> getSpotsByType(Long lotId, ParkingSpot.SpotType type);
    ParkingSpotResponse getSpotById(Long spotId);
}
