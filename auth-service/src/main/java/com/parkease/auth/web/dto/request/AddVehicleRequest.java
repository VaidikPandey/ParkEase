package com.parkease.auth.web.dto.request;

import com.parkease.auth.domain.entity.Vehicle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddVehicleRequest {

    @NotBlank
    @Size(max = 20)
    private String plate;

    @NotNull
    private Vehicle.VehicleType vehicleType;

    private boolean isEv;

    @Size(max = 50)
    private String nickname;
}
