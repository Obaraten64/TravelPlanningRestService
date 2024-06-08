package com.travel.planning.controller;

import com.travel.planning.dto.request.*;
import com.travel.planning.service.TravelPlanningService;
import com.travel.planning.service.UserDetailsServiceImp;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import jakarta.validation.Valid;

import lombok.AllArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
public class TravelPlanningController {
    private final TravelPlanningService travelPlanningService;
    private final UserDetailsServiceImp userDetailsService;

    @Operation(summary = "Register new user")
    @ApiResponse(responseCode = "201", description = "User registered", content = @Content)
    @ApiResponse(responseCode = "400", description = "Wrong role or user already registered", content = @Content)

    @PostMapping("/user/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegistrationRequest registrationRequest) {
        return userDetailsService.register(registrationRequest);
    }

    // http://localhost:8080/swagger-ui/index.html to access swagger
}
