package com.travel.planning.service;

import com.travel.planning.configuration.Mapper;
import com.travel.planning.dto.request.AddServiceRequest;
import com.travel.planning.dto.request.DeleteRequest;
import com.travel.planning.dto.request.ServiceRequest;
import com.travel.planning.dto.request.TravelRequest;
import com.travel.planning.dto.response.ServicesDTO;
import com.travel.planning.dto.response.TravelDTO;
import com.travel.planning.exception.ServicesException;
import com.travel.planning.exception.TravelException;
import com.travel.planning.model.Cities;
import com.travel.planning.model.Services;
import com.travel.planning.model.Travel;
import com.travel.planning.model.User;
import com.travel.planning.repository.CitiesRepository;
import com.travel.planning.repository.ServicesRepository;
import com.travel.planning.repository.TravelRepository;

import lombok.AllArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@AllArgsConstructor
public class TravelPlanningService {
    private final CitiesRepository citiesRepository;
    private final ServicesRepository servicesRepository;
    private final TravelRepository travelRepository;

    @Transactional
    public TravelDTO createTravel(TravelRequest travelRequest, User user) {
        if (travelRepository.findTravelByUser(user).isPresent()) {
            throw new TravelException("You have already planned your travel");
        }

        Optional<Cities> departure = citiesRepository.findCitiesByName(travelRequest.getDeparture());
        Optional<Cities> destination = citiesRepository.findCitiesByName(travelRequest.getDestination());
        if (departure.isEmpty() || destination.isEmpty()) {
            throw new TravelException("We cannot pick you up from your city or deliver you to your destination");
        }

        Travel travel = Travel.builder()
                .departure(departure.get())
                .destination(destination.get())
                .travel_time(travelRequest.getTravel_time())
                .user(user)
                .services(null)
                .build();
        travelRepository.save(travel);

        return Mapper.mapToTravelDTO(travel);
    }

    public List<ServicesDTO> getServices(User user) {
        Travel travel = travelRepository.findTravelByUser(user).orElseGet(Travel::new);

        List<Services> services;
        if (Optional.ofNullable(travel.getDestination()).isPresent()) {
            services = servicesRepository.findAllByCity(travel.getDestination());
        } else {
            services = servicesRepository.findAll();
        }

        if (services.isEmpty()) {
            throw new ServicesException("No services in the city");
        }
        return services.stream()
                .map(Mapper::mapToServicesDTO)
                .toList();
    }

    @Transactional
    public TravelDTO bookService(ServiceRequest serviceRequest, User user) {
        Travel travel = travelRepository.findTravelByUser(user)
                .orElseThrow(() -> new TravelException("You haven't planned a travel"));
        Services service = servicesRepository.findByName(serviceRequest.getName())
                .orElseThrow(() -> new ServicesException("There is no service with that name"));

        if (Optional.ofNullable(travel.getServices()).isEmpty()) {
            travel.setServices(new LinkedList<>());
        }
        travel.getServices().add(service);
        travelRepository.save(travel);

        return Mapper.mapToTravelDTO(travel);
    }

    @Transactional
    public boolean completeTravel(User user) {
        Optional<Travel> travel = travelRepository.findTravelByUser(user);
        if (travel.isEmpty()) {
            return false;
        }

        travelRepository.delete(travel.get());
        return true;
    }

    public List<TravelDTO> getTravels() {
        return travelRepository.findAll().stream()
                .map(Mapper::mapToTravelDTO)
                .toList();
    }

    @Transactional
    public List<TravelDTO> deleteTrips(DeleteRequest deleteRequest) {
        Set<Travel> travels = new HashSet<>();
        travels.addAll(travelRepository.findAllByDeparture(
                Cities.builder().name(deleteRequest.getDeparture()).build()));
        travels.addAll(travelRepository.findAllByDestination(
                Cities.builder().name(deleteRequest.getDestination()).build()));

        List<TravelDTO> deleted = travels.stream()
                .map(Mapper::mapToTravelDTO)
                .toList();
        travelRepository.deleteAll(travels);

        return deleted;
    }

    @Transactional
    public ServicesDTO addService(AddServiceRequest addServiceRequest) {
        Cities city = citiesRepository.findCitiesByName(addServiceRequest.getCity())
                .orElseGet(() -> citiesRepository.save(Cities.builder().name(addServiceRequest.getCity()).build()));

        if (servicesRepository.findByNameAndCity(addServiceRequest.getName(), city).isPresent()) {
            throw new ServicesException("The service already exists");
        }
        Services service = servicesRepository.save(Services.builder().name(addServiceRequest.getName())
                .city(city).build());

        return Mapper.mapToServicesDTO(service);
    }
}
