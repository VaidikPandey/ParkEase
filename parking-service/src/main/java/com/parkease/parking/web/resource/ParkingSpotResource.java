package com.parkease.parking.web.resource;

import com.parkease.parking.domain.entity.ParkingSpot;
import com.parkease.parking.security.JwtUtil;
import com.parkease.parking.service.ParkingSpotService;
import com.parkease.parking.web.dto.request.BulkCreateSpotRequest;
import com.parkease.parking.web.dto.request.CreateSpotRequest;
import com.parkease.parking.web.dto.request.UpdateSpotRequest;
import com.parkease.parking.web.dto.response.ParkingSpotResponse;
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

@RestController
@RequestMapping("/api/v1/parking")
@RequiredArgsConstructor
@Tag(name = "Parking Spots", description = "Spot management — add, bulk add, update, delete")
public class ParkingSpotResource {

    private final ParkingSpotService spotService;
    private final JwtUtil jwtUtil;

    // ── Public

    @GetMapping("/lots/{lotId}/spots")
    @Operation(summary = "Get all spots for a lot — public")
    public ResponseEntity<List<ParkingSpotResponse>> getSpots(@PathVariable Long lotId) {
        return ResponseEntity.ok(spotService.getSpotsByLot(lotId));
    }

    @GetMapping("/lots/{lotId}/spots/available")
    @Operation(summary = "Get available spots for a lot — public")
    public ResponseEntity<List<ParkingSpotResponse>> getAvailableSpots(
        @PathVariable Long lotId
    ) {
        return ResponseEntity.ok(spotService.getAvailableSpots(lotId));
    }

    @GetMapping("/lots/{lotId}/spots/type/{type}")
    @Operation(summary = "Filter spots by type — public")
    public ResponseEntity<List<ParkingSpotResponse>> getByType(
        @PathVariable Long lotId,
        @PathVariable ParkingSpot.SpotType type
    ) {
        return ResponseEntity.ok(spotService.getSpotsByType(lotId, type));
    }

    @GetMapping("/spots/{spotId}")
    @Operation(summary = "Get spot by ID — public")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Spot found"),
        @ApiResponse(responseCode = "404", description = "Spot not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ParkingSpotResponse> getSpotById(@PathVariable Long spotId) {
        return ResponseEntity.ok(spotService.getSpotById(spotId));
    }

    // ── Manager

    @PostMapping("/manager/lots/{lotId}/spots")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Add a single spot to a lot — MANAGER only",
        security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Spot created"),
        @ApiResponse(responseCode = "409", description = "Spot number already exists",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ParkingSpotResponse> addSpot(
        @RequestHeader("Authorization") String authHeader,
        @PathVariable Long lotId,
        @Valid @RequestBody CreateSpotRequest request
    ) {
        Long managerId = extractUserId(authHeader);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(spotService.addSpot(lotId, managerId, request));
    }

    @PostMapping("/manager/lots/{lotId}/spots/bulk")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Bulk add spots to a lot — MANAGER only",
        security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<ParkingSpotResponse>> bulkAddSpots(
        @RequestHeader("Authorization") String authHeader,
        @PathVariable Long lotId,
        @Valid @RequestBody BulkCreateSpotRequest request
    ) {
        Long managerId = extractUserId(authHeader);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(spotService.bulkAddSpots(lotId, managerId, request));
    }

    @PutMapping("/manager/spots/{spotId}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Update spot details — MANAGER only",
        security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ParkingSpotResponse> updateSpot(
        @RequestHeader("Authorization") String authHeader,
        @PathVariable Long spotId,
        @RequestBody UpdateSpotRequest request
    ) {
        Long managerId = extractUserId(authHeader);
        return ResponseEntity.ok(spotService.updateSpot(spotId, managerId, request));
    }

    @DeleteMapping("/manager/spots/{spotId}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Delete a spot — MANAGER only",
        security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Spot deleted"),
        @ApiResponse(responseCode = "404", description = "Spot not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteSpot(
        @RequestHeader("Authorization") String authHeader,
        @PathVariable Long spotId
    ) {
        Long managerId = extractUserId(authHeader);
        spotService.deleteSpot(spotId, managerId);
        return ResponseEntity.noContent().build();
    }

    // ── Helper

    private Long extractUserId(String authHeader) {
        return jwtUtil.extractUserId(authHeader.substring(7));
    }
}
