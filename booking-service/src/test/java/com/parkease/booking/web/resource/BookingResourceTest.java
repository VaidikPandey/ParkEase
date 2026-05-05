package com.parkease.booking.web.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkease.booking.domain.entity.Booking;
import com.parkease.booking.service.BookingService;
import com.parkease.booking.web.dto.request.CancelBookingRequest;
import com.parkease.booking.web.dto.request.CreateBookingRequest;
import com.parkease.booking.web.dto.request.ExtendBookingRequest;
import com.parkease.booking.web.dto.response.BookingResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingResource.class)
@AutoConfigureMockMvc(addFilters = false)
class BookingResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createBooking_ShouldReturn201() throws Exception {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setSpotId(1L);
        request.setLotId(1L);
        request.setSpotNumber("A1");
        request.setBookingType(com.parkease.booking.domain.entity.Booking.BookingType.PRE_BOOKING);
        request.setPricePerHour(10.0);
        request.setVehiclePlate("MH12AB1234");
        request.setDriverEmail("driver@test.com");
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(2));

        when(bookingService.createBooking(anyLong(), any())).thenReturn(BookingResponse.builder().build());

        mockMvc.perform(post("/api/v1/bookings")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void getMyBookings_ShouldReturn200() throws Exception {
        when(bookingService.getBookingsByDriver(anyLong())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/bookings/my")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void checkIn_ShouldReturn200() throws Exception {
        when(bookingService.checkIn(anyLong(), anyLong())).thenReturn(BookingResponse.builder().build());

        mockMvc.perform(put("/api/v1/bookings/1/checkin")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void checkOut_ShouldReturn200() throws Exception {
        when(bookingService.checkOut(anyLong(), anyLong())).thenReturn(BookingResponse.builder().build());

        mockMvc.perform(put("/api/v1/bookings/1/checkout")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void cancelBooking_ShouldReturn200() throws Exception {
        CancelBookingRequest request = new CancelBookingRequest();
        request.setReason(CancelBookingRequest.CancellationReason.CHANGE_OF_PLANS);

        when(bookingService.cancelBooking(anyLong(), anyLong(), any()))
                .thenReturn(BookingResponse.builder().build());

        mockMvc.perform(put("/api/v1/bookings/1/cancel")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void extendBooking_ShouldReturn200() throws Exception {
        ExtendBookingRequest request = new ExtendBookingRequest();
        request.setNewEndTime(LocalDateTime.now().plusHours(2));

        when(bookingService.extendBooking(anyLong(), anyLong(), any()))
                .thenReturn(BookingResponse.builder().build());

        mockMvc.perform(put("/api/v1/bookings/1/extend")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void getBookingById_ShouldReturn200() throws Exception {
        when(bookingService.getBookingById(anyLong(), anyLong()))
                .thenReturn(BookingResponse.builder().build());

        mockMvc.perform(get("/api/v1/bookings/1")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void getLotBookings_ShouldReturn200() throws Exception {
        when(bookingService.getBookingsByLot(anyLong())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/bookings/manager/lot/1")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllBookings_ShouldReturn200() throws Exception {
        when(bookingService.getAllBookings()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/bookings/admin/all"))
                .andExpect(status().isOk());
    }

    @Test
    void forceCheckout_ShouldReturn200() throws Exception {
        when(bookingService.forceCheckout(anyLong())).thenReturn(BookingResponse.builder().build());

        mockMvc.perform(put("/api/v1/bookings/admin/1/force-checkout"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteBookingsByDriver_ShouldReturn204() throws Exception {
        doNothing().when(bookingService).deleteBookingsByDriver(anyLong());

        mockMvc.perform(delete("/api/v1/bookings/admin/driver/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteBookingsByLot_ShouldReturn204() throws Exception {
        doNothing().when(bookingService).deleteBookingsByLot(anyLong());

        mockMvc.perform(delete("/api/v1/bookings/admin/lot/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void createBooking_ShouldReturn400_WhenServiceThrowsIllegalArgument() throws Exception {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setSpotId(1L); request.setLotId(1L); request.setBookingType(Booking.BookingType.PRE_BOOKING);
        request.setPricePerHour(10.0); request.setDriverEmail("d@t.com");
        request.setStartTime(LocalDateTime.now().plusHours(2));
        request.setEndTime(LocalDateTime.now().plusHours(1));

        when(bookingService.createBooking(anyLong(), any()))
                .thenThrow(new IllegalArgumentException("End time must be after start time"));

        mockMvc.perform(post("/api/v1/bookings")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelBooking_ShouldReturn409_WhenAlreadyCheckedIn() throws Exception {
        CancelBookingRequest request = new CancelBookingRequest();
        request.setReason(CancelBookingRequest.CancellationReason.CHANGE_OF_PLANS);

        when(bookingService.cancelBooking(anyLong(), anyLong(), any()))
                .thenThrow(new IllegalStateException("Cannot cancel a checked-in booking"));

        mockMvc.perform(put("/api/v1/bookings/1/cancel")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
