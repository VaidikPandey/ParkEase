package com.parkease.auth.web.resource;

import com.parkease.auth.domain.entity.User;
import com.parkease.auth.service.AdminService;
import com.parkease.auth.web.dto.response.UserProfileResponse;
import com.parkease.auth.web.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin-only user management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminResource {

    private final AdminService adminService;

    // ── Get All Users

    @GetMapping("/users")
    @Operation(summary = "Get all users — drivers and managers")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of all users"),
            @ApiResponse(responseCode = "403", description = "Admin access required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    // ── Get Users By Role

    @GetMapping("/users/role/{role}")
    @Operation(summary = "Get users filtered by role — DRIVER or MANAGER")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Filtered list of users"),
            @ApiResponse(responseCode = "403", description = "Admin access required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<UserProfileResponse>> getUsersByRole(
            @PathVariable User.Role role
    ) {
        return ResponseEntity.ok(adminService.getUsersByRole(role));
    }

    // ── Get User By ID

    @GetMapping("/users/{id}")
    @Operation(summary = "Get a specific user by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Admin access required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserProfileResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserById(id));
    }

    // ── Suspend User

    @PutMapping("/users/{id}/suspend")
    @Operation(summary = "Suspend a user account — blocks login")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User suspended"),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "User already suspended",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Admin access required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> suspendUser(@PathVariable Long id) {
        adminService.suspendUser(id);
        return ResponseEntity.noContent().build();
    }

    // ── Reactivate User

    @PutMapping("/users/{id}/reactivate")
    @Operation(summary = "Reactivate a suspended user account")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User reactivated"),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "User already active",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Admin access required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> reactivateUser(@PathVariable Long id) {
        adminService.reactivateUser(id);
        return ResponseEntity.noContent().build();
    }

    // ── Delete User

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Permanently delete a user account")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted"),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Cannot delete admin account",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Admin access required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}