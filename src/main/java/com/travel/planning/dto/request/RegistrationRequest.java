package com.travel.planning.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegistrationRequest {
    @Schema(example = "misha@gmail.com")
    @NotBlank(message = "Write down your email!")
    private String email;
    @Schema(example = "1234")
    @NotBlank(message = "Write down your password!")
    private String password;
    @Schema(example = "traveler")
    @NotBlank(message = "Write down your role!")
    private String role;
}
