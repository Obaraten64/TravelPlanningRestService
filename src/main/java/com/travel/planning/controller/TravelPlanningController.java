package com.travel.planning.controller;

import com.travel.planning.configuration.security.UserAdapter;
import com.travel.planning.dto.request.*;
import com.travel.planning.dto.response.TravelDTO;
import com.travel.planning.service.TravelPlanningService;
import com.travel.planning.service.UserDetailsServiceImp;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

import lombok.AllArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @Operation(summary = "Create new travel, authorization required", security = @SecurityRequirement(name = "basicAuth"))
    @ApiResponse(responseCode = "201", description = "Created travel", content = @Content(
            schema = @Schema(implementation = TravelDTO.class),
            examples = @ExampleObject(value = "{\"departure\":\"Kiev\",\"destination\":\"Warsaw\"," +
                            "\"travel_time\":\"2024-12-12T12:12:12\"}")))
    @ApiResponse(responseCode = "400", description = "No city", content = @Content)
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)

    @PostMapping("/travel/create")
    public ResponseEntity<TravelDTO> createTravel(@Valid @RequestBody TravelRequest travelRequest,
                                               @AuthenticationPrincipal UserAdapter userAdapter) {
        return new ResponseEntity<>(travelPlanningService.createTravel(travelRequest, userAdapter.getUser()),
                HttpStatus.CREATED);
    }

    // http://localhost:8080/swagger-ui/index.html to access swagger
}
