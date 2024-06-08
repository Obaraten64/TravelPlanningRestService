package com.travel.planning.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TravelDTO {
    private String departure;
    private String destination;
    private LocalDateTime travel_time;
    private List<ServicesDTO> services;
}
