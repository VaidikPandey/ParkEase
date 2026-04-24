package com.parkease.auth.web.resource;

import com.parkease.auth.service.VehicleService;
import com.parkease.auth.web.dto.request.AddVehicleRequest;
import com.parkease.auth.web.dto.response.VehicleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/vehicles")
@RequiredArgsConstructor
@Tag(name = "Vehicles", description = "Driver vehicle management")
public class VehicleResource {

    private final VehicleService vehicleService;

    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Register a new vehicle")
    public ResponseEntity<VehicleResponse> addVehicle(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AddVehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vehicleService.addVehicle(userId, request));
    }

    @GetMapping
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "List all vehicles for the current driver")
    public ResponseEntity<List<VehicleResponse>> getMyVehicles(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(vehicleService.getMyVehicles(userId));
    }

    @DeleteMapping("/{vehicleId}")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Delete a vehicle (own only)")
    public ResponseEntity<Void> deleteVehicle(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long vehicleId) {
        vehicleService.deleteVehicle(userId, vehicleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/plate/{plate}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Look up a vehicle by plate number (admin/manager only)")
    public ResponseEntity<VehicleResponse> getByPlate(@PathVariable String plate) {
        return ResponseEntity.ok(vehicleService.getByPlate(plate));
    }
}
