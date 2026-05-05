package com.parkease.auth.web.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkease.auth.service.AuthService;
import com.parkease.auth.util.CookieUtil;
import com.parkease.auth.web.dto.request.*;
import com.parkease.auth.web.dto.response.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthResource.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthResourceTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AuthService authService;
    @MockBean private CookieUtil cookieUtil;
    @Autowired private ObjectMapper objectMapper;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void setAuth(String userId) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null,
            List.of(new SimpleGrantedAuthority("ROLE_DRIVER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private ResponseCookie dummy(String name) {
        return ResponseCookie.from(name, "v").build();
    }

    @Test
    void register_ShouldReturn201() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("a@b.com"); req.setPassword("pass1234"); req.setFullName("A B");
        req.setRole(com.parkease.auth.domain.entity.User.Role.DRIVER);

        AuthResponse resp = AuthResponse.builder().accessToken("tok").refreshToken("ref")
            .user(UserProfileResponse.builder().build()).build();
        when(authService.register(any())).thenReturn(resp);
        when(cookieUtil.createAccessTokenCookie(any())).thenReturn(dummy("access_token"));
        when(cookieUtil.createRefreshTokenCookie(any())).thenReturn(dummy("refresh_token"));

        mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated());
    }

    @Test
    void login_ShouldReturn200() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("a@b.com"); req.setPassword("pass");

        AuthResponse resp = AuthResponse.builder().accessToken("tok").refreshToken("ref").build();
        when(authService.login(any())).thenReturn(resp);
        when(cookieUtil.createAccessTokenCookie(any())).thenReturn(dummy("access_token"));
        when(cookieUtil.createRefreshTokenCookie(any())).thenReturn(dummy("refresh_token"));

        mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk());
    }

    @Test
    void logout_ShouldReturn204() throws Exception {
        when(cookieUtil.clearCookie(anyString())).thenReturn(dummy("c"));
        mockMvc.perform(post("/api/v1/auth/logout"))
            .andExpect(status().isNoContent());
    }

    @Test
    void refresh_ShouldReturn401_WhenNoCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getProfile_ShouldReturn200() throws Exception {
        setAuth("1");
        when(authService.getProfile(1L)).thenReturn(UserProfileResponse.builder().userId(1L).build());

        mockMvc.perform(get("/api/v1/auth/profile"))
            .andExpect(status().isOk());
    }

    @Test
    void updateProfile_ShouldReturn200() throws Exception {
        setAuth("1");
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("New Name");
        when(authService.updateProfile(anyLong(), any())).thenReturn(UserProfileResponse.builder().build());

        mockMvc.perform(put("/api/v1/auth/profile")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk());
    }

    @Test
    void changePassword_ShouldReturn204() throws Exception {
        setAuth("1");
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("old"); req.setNewPassword("newpass1"); req.setConfirmPassword("newpass1");
        doNothing().when(authService).changePassword(anyLong(), any());

        mockMvc.perform(put("/api/v1/auth/change-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isNoContent());
    }

    @Test
    void deactivate_ShouldReturn204() throws Exception {
        setAuth("1");
        doNothing().when(authService).deactivateAccount(anyLong());

        mockMvc.perform(delete("/api/v1/auth/account"))
            .andExpect(status().isNoContent());
    }

    @Test
    void selectRole_ShouldReturn200() throws Exception {
        setAuth("1");
        AuthResponse resp = AuthResponse.builder().accessToken("tok").refreshToken("ref").build();
        when(authService.selectRole(anyLong(), anyString())).thenReturn(resp);
        when(cookieUtil.createAccessTokenCookie(any())).thenReturn(dummy("access_token"));
        when(cookieUtil.createRefreshTokenCookie(any())).thenReturn(dummy("refresh_token"));

        mockMvc.perform(patch("/api/v1/auth/role")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"role\":\"DRIVER\"}"))
            .andExpect(status().isOk());
    }
}
