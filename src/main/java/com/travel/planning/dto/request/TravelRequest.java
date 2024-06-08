package com.travel.planning.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TravelRequest {
    @Schema(example = "Kiev")
    @NotBlank(message = "Write down the city of departure!")
    private String departure;
    @Schema(example = "Warsaw")
    @NotBlank(message = "Write down the destination city!")
    private String destination;
    @Schema(example = "2024-12-12T12:12:12")
    @NotNull(message = "Choose the date and time for your travel!")
    private LocalDateTime travel_time;
}
