package com.parkease.parking.web.resource;

import com.parkease.parking.service.AvailabilityCounterService;
import com.parkease.parking.service.ParkingLotService;
import com.parkease.parking.web.dto.request.CreateLotRequest;
import com.parkease.parking.web.dto.request.UpdateLotRequest;
import com.parkease.parking.web.dto.response.ParkingLotResponse;
import com.parkease.parking.web.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/parking")
@RequiredArgsConstructor
@Tag(name = "Parking Lots", description = "Lot management — create, update, search, approve")
public class ParkingLotResource {

    private final ParkingLotService lotService;
    private final AvailabilityCounterService counterService;

    // ── Public

    @GetMapping("/lots/search")
    @Operation(summary = "Search approved lots — public; optional city filter")
    public ResponseEntity<List<ParkingLotResponse>> searchByCity(
            @RequestParam(required = false) String city
    ) {
        if (city == null || city.isBlank()) {
            return ResponseEntity.ok(lotService.getApprovedLots());
        }
        return ResponseEntity.ok(lotService.getLotsByCity(city));
    }

    @GetMapping("/lots/nearby")
    @Operation(summary = "Find nearby lots by GPS coordinates — public")
    public ResponseEntity<List<ParkingLotResponse>> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5.0") double radius
    ) {
        return ResponseEntity.ok(lotService.getNearbyLots(lat, lng, radius));
    }

    @GetMapping("/lots/{id}")
    @Operation(summary = "Get lot details by ID — public")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lot found"),
            @ApiResponse(responseCode = "404", description = "Lot not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ParkingLotResponse> getLotById(@PathVariable Long id) {
        return ResponseEntity.ok(lotService.getLotById(id));
    }

    @GetMapping("/lots/{id}/availability")
    @Operation(summary = "Get real-time available spot count from Redis — public")
    public ResponseEntity<Map<String, Object>> getAvailability(@PathVariable Long id) {
        int count = counterService.getAvailableCount(id);
        boolean available = counterService.isAvailable(id);
        return ResponseEntity.ok(Map.of(
                "lotId", id,
                "availableSpots", count,
                "isAvailable", available
        ));
    }

    // ── Manager

    @PostMapping("/manager/lots")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Create a new parking lot — MANAGER only",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Lot created, status PENDING"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ParkingLotResponse> createLot(
            @RequestHeader("X-User-Id") Long managerId,
            @Valid @RequestBody CreateLotRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lotService.createLot(managerId, request));
    }

    @PutMapping("/manager/lots/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Update lot details — MANAGER only",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ParkingLotResponse> updateLot(
            @RequestHeader("X-User-Id") Long managerId,
            @PathVariable Long id,
            @RequestBody UpdateLotRequest request
    ) {
        return ResponseEntity.ok(lotService.updateLot(id, managerId, request));
    }

    @PutMapping("/manager/lots/{id}/toggle")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Toggle lot open/closed — MANAGER only",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ParkingLotResponse> toggleStatus(
            @RequestHeader("X-User-Id") Long managerId,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(lotService.toggleLotStatus(id, managerId));
    }

    @GetMapping("/manager/lots")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Get all lots owned by current manager",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<ParkingLotResponse>> getMyLots(
            @RequestHeader("X-User-Id") Long managerId
    ) {
        return ResponseEntity.ok(lotService.getLotsByManager(managerId));
    }

    // ── Admin

    @GetMapping("/admin/lots/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all lots regardless of status — ADMIN only",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<ParkingLotResponse>> getAllLots() {
        return ResponseEntity.ok(lotService.getAllLots());
    }

    @GetMapping("/admin/lots/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all pending lot registrations — ADMIN only",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<ParkingLotResponse>> getPendingLots() {
        return ResponseEntity.ok(lotService.getPendingLots());
    }

    @PutMapping("/admin/lots/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve a lot registration — ADMIN only",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> approveLot(@PathVariable Long id) {
        lotService.approveLot(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/admin/lots/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject a lot registration — ADMIN only",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> rejectLot(@PathVariable Long id) {
        lotService.rejectLot(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/manager/{managerId}/lots")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete all lots (and their spots) belonging to a manager — ADMIN only",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> deleteLotsByManager(@PathVariable Long managerId) {
        lotService.deleteLotsByManager(managerId);
        return ResponseEntity.noContent().build();
    }

}