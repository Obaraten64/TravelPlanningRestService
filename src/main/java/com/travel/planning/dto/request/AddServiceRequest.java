package com.travel.planning.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddServiceRequest {
    @Schema(example = "Hotel")
    @NotBlank(message = "Write down the name of service!")
    private String name;
    @Schema(example = "Kiev")
    @NotBlank(message = "Write down the name of the city where the service is located")
    private String city;
}
