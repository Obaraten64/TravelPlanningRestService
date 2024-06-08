package com.travel.planning.configuration;

import com.travel.planning.dto.response.ServicesDTO;
import com.travel.planning.dto.response.TravelDTO;
import com.travel.planning.model.Services;
import com.travel.planning.model.Travel;

import java.util.List;

public class Mapper {
    public static TravelDTO mapToTravelDTO(Travel travel) {
        List<Services> services = travel.getServices();
        return TravelDTO.builder()
                .departure(travel.getDeparture().getName())
                .destination(travel.getDestination().getName())
                .travel_time(travel.getTravel_time())
                .services(services == null ? null : services.stream().map(Mapper::mapToServicesDTO).toList())
                .build();
    }

    public static ServicesDTO mapToServicesDTO(Services services) {
        return ServicesDTO.builder()
                .name(services.getName())
                .city(services.getCity().getName())
                .build();
    }
}
