package com.parkease.auth.web.resource;

import com.parkease.auth.security.JwtCookieFilter;
import com.parkease.auth.service.AuthService;
import com.parkease.auth.util.CookieUtil;
import com.parkease.auth.web.dto.request.*;
import com.parkease.auth.web.dto.response.*;
import com.parkease.auth.web.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, Login, Token management, Profile CRUD")
public class AuthResource {

    private final AuthService authService;
    private final CookieUtil cookieUtil;

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
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        AuthResponse auth = authService.register(request);
        setAuthCookies(response, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(auth);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @PostMapping("/login")
    @Operation(summary = "Login with email and password — sets HttpOnly auth cookies")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        AuthResponse auth = authService.login(request);
        setAuthCookies(response, auth);
        return ResponseEntity.ok(auth);
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @PostMapping("/logout")
    @Operation(
        summary = "Logout — blacklists the current access token and clears auth cookies",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = JwtCookieFilter.extractFromCookie(request, "access_token");
        if (token != null) {
            authService.logout(token);
        }
        response.addHeader("Set-Cookie", cookieUtil.clearCookie("access_token").toString());
        response.addHeader("Set-Cookie", cookieUtil.clearCookie("refresh_token").toString());
        return ResponseEntity.noContent().build();
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @PostMapping("/refresh")
    @Operation(summary = "Get a new access token using the refresh_token cookie")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "New tokens issued"),
        @ApiResponse(responseCode = "401", description = "Refresh token invalid or expired",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = JwtCookieFilter.extractFromCookie(request, "refresh_token");
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        AuthResponse auth = authService.refresh(refreshToken);
        setAuthCookies(response, auth);
        return ResponseEntity.ok(auth);
    }

    // ── Get Profile ───────────────────────────────────────────────────────────

    @GetMapping("/profile")
    @Operation(
        summary = "Get current user's profile",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<UserProfileResponse> getProfile() {
        return ResponseEntity.ok(authService.getProfile(currentUserId()));
    }

    // ── Update Profile ────────────────────────────────────────────────────────

    @PutMapping("/profile")
    @Operation(
        summary = "Update profile — fullName, phone, profilePicUrl, vehiclePlate",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<UserProfileResponse> updateProfile(
            @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(authService.updateProfile(currentUserId(), request));
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
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(currentUserId(), request);
        return ResponseEntity.noContent().build();
    }

    // ── Deactivate Account ────────────────────────────────────────────────────

    @DeleteMapping("/account")
    @Operation(
        summary = "Deactivate own account (soft delete)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Void> deactivate() {
        authService.deactivateAccount(currentUserId());
        return ResponseEntity.noContent().build();
    }

    // ── Select Role (OAuth onboarding) ────────────────────────────────────────

    @PatchMapping("/role")
    @Operation(
        summary = "Set role for new OAuth users — DRIVER or MANAGER only",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<AuthResponse> selectRole(
            @RequestBody java.util.Map<String, String> body,
            HttpServletResponse response) {
        AuthResponse auth = authService.selectRole(currentUserId(), body.get("role"));
        setAuthCookies(response, auth);
        return ResponseEntity.ok(auth);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Long currentUserId() {
        return Long.parseLong(
            (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal()
        );
    }

    private void setAuthCookies(HttpServletResponse response, AuthResponse auth) {
        response.addHeader("Set-Cookie", cookieUtil.createAccessTokenCookie(auth.getAccessToken()).toString());
        response.addHeader("Set-Cookie", cookieUtil.createRefreshTokenCookie(auth.getRefreshToken()).toString());
    }
}
