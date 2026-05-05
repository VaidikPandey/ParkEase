package com.parkease.auth.web.resource;

import com.parkease.auth.service.AdminService;
import com.parkease.auth.web.dto.response.UserProfileResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminResource.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    @Test
    void getAllUsers_ShouldReturn200() throws Exception {
        when(adminService.getAllUsers()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk());
    }

    @Test
    void getUserById_ShouldReturn200() throws Exception {
        when(adminService.getUserById(anyLong()))
                .thenReturn(UserProfileResponse.builder().build());

        mockMvc.perform(get("/api/v1/admin/users/1"))
                .andExpect(status().isOk());
    }

    @Test
    void suspendUser_ShouldReturn204() throws Exception {
        doNothing().when(adminService).suspendUser(anyLong());

        mockMvc.perform(put("/api/v1/admin/users/1/suspend"))
                .andExpect(status().isNoContent());
    }

    @Test
    void reactivateUser_ShouldReturn204() throws Exception {
        doNothing().when(adminService).reactivateUser(anyLong());

        mockMvc.perform(put("/api/v1/admin/users/1/reactivate"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getUsersByRole_ShouldReturn200() throws Exception {
        when(adminService.getUsersByRole(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/users/role/DRIVER"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteUser_ShouldReturn204() throws Exception {
        doNothing().when(adminService).deleteUser(anyLong());

        mockMvc.perform(delete("/api/v1/admin/users/1"))
                .andExpect(status().isNoContent());
    }
}
