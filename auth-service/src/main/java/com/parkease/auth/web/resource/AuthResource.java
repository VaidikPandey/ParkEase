package com.parkease.auth.web.resource;

import com.parkease.auth.security.JwtUtil;
import com.parkease.auth.service.AuthService;
import com.parkease.auth.web.dto.request.*;
import com.parkease.auth.web.dto.response.*;
import com.parkease.auth.web.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, Login, Token management, Profile CRUD")
public class AuthResource {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    // ── Register ──────────────────────────────────────────────────────────────

    @PostMapping("/register")
    @Operation(summary = "Register a new user (DRIVER or MANAGER)")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User registered successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Email already exists",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @PostMapping("/login")
    @Operation(summary = "Login with email and password — returns JWT tokens")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @PostMapping("/logout")
    @Operation(
        summary = "Logout and blacklist the current access token",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        authService.logout(authHeader.substring(7));
        return ResponseEntity.noContent().build();
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @PostMapping("/refresh")
    @Operation(summary = "Get a new access token using your refresh token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "New tokens issued"),
        @ApiResponse(responseCode = "401", description = "Refresh token invalid or expired",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> refresh(
        @RequestHeader("X-Refresh-Token") String refreshToken
    ) {
        return ResponseEntity.ok(authService.refresh(refreshToken));
    }

    // ── Get Profile ───────────────────────────────────────────────────────────

    @GetMapping("/profile")
    @Operation(
        summary = "Get current user's profile",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<UserProfileResponse> getProfile(
        @RequestHeader("Authorization") String authHeader
    ) {
        Long userId = extractUserId(authHeader);
        return ResponseEntity.ok(authService.getProfile(userId));
    }

    // ── Update Profile ────────────────────────────────────────────────────────

    @PutMapping("/profile")
    @Operation(
        summary = "Update profile — fullName, phone, profilePicUrl, vehiclePlate",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<UserProfileResponse> updateProfile(
        @RequestHeader("Authorization") String authHeader,
        @RequestBody UpdateProfileRequest request
    ) {
        Long userId = extractUserId(authHeader);
        return ResponseEntity.ok(authService.updateProfile(userId, request));
    }

    // ── Change Password ───────────────────────────────────────────────────────

    @PutMapping("/change-password")
    @Operation(
        summary = "Change password",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Password changed"),
        @ApiResponse(responseCode = "400", description = "Passwords don't match",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Current password wrong",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> changePassword(
        @RequestHeader("Authorization") String authHeader,
        @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(extractUserId(authHeader), request);
        return ResponseEntity.noContent().build();
    }

    // ── Deactivate Account ────────────────────────────────────────────────────

    @DeleteMapping("/account")
    @Operation(
        summary = "Deactivate own account (soft delete)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Void> deactivate(
        @RequestHeader("Authorization") String authHeader
    ) {
        authService.deactivateAccount(extractUserId(authHeader));
        return ResponseEntity.noContent().build();
    }

    // ── Select Role (OAuth onboarding) ────────────────────────────────────────

    @PatchMapping("/role")
    @Operation(
        summary = "Set role for new OAuth users — DRIVER or MANAGER only",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<AuthResponse> selectRole(
        @RequestHeader("Authorization") String authHeader,
        @RequestBody java.util.Map<String, String> body
    ) {
        Long userId = extractUserId(authHeader);
        return ResponseEntity.ok(authService.selectRole(userId, body.get("role")));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Long extractUserId(String authHeader) {
        return jwtUtil.extractClaims(authHeader.substring(7))
                      .get("userId", Long.class);
    }
}
