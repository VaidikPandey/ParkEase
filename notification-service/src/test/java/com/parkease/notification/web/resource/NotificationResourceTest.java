package com.parkease.notification.web.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkease.notification.security.HeaderAuthFilter;
import com.parkease.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationResource.class)
@Import({NotificationResourceTest.TestSecurity.class, HeaderAuthFilter.class})
class NotificationResourceTest {

    @TestConfiguration
    static class TestSecurity {
        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http, HeaderAuthFilter headerAuthFilter) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .addFilterBefore(headerAuthFilter, UsernamePasswordAuthenticationFilter.class)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getByRecipient_ShouldReturn200() throws Exception {
        when(notificationService.getByRecipient(anyLong(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/notifications/recipient/1")
                .header("X-User-Id", "1")
                .header("X-User-Role", "DRIVER"))
                .andExpect(status().isOk());
    }

    @Test
    void getUnreadCount_ShouldReturn200() throws Exception {
        when(notificationService.getUnreadCount(anyLong())).thenReturn(5L);

        mockMvc.perform(get("/api/v1/notifications/recipient/1/unread-count")
                .header("X-User-Id", "1")
                .header("X-User-Role", "DRIVER"))
                .andExpect(status().isOk());
    }

    @Test
    void getUnread_ShouldReturn200() throws Exception {
        when(notificationService.getUnread(anyLong(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/notifications/recipient/1/unread")
                .header("X-User-Id", "1")
                .header("X-User-Role", "DRIVER"))
                .andExpect(status().isOk());
    }

    @Test
    void markAsRead_ShouldReturn204() throws Exception {
        org.mockito.Mockito.doNothing().when(notificationService).markAsRead(anyLong(), anyLong(), anyBoolean());

        mockMvc.perform(patch("/api/v1/notifications/1/read")
                .header("X-User-Id", "1")
                .header("X-User-Role", "DRIVER"))
                .andExpect(status().isNoContent());
    }
}
