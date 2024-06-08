package com.travel.planning;

import com.travel.planning.configuration.security.Role;
import com.travel.planning.dto.request.AddServiceRequest;
import com.travel.planning.dto.request.DeleteRequest;
import com.travel.planning.dto.request.ServiceRequest;
import com.travel.planning.dto.request.TravelRequest;
import com.travel.planning.dto.response.*;
import com.travel.planning.exception.ServicesException;
import com.travel.planning.exception.TravelException;
import com.travel.planning.model.Cities;
import com.travel.planning.model.Services;
import com.travel.planning.model.Travel;
import com.travel.planning.model.User;
import com.travel.planning.repository.CitiesRepository;
import com.travel.planning.repository.ServicesRepository;
import com.travel.planning.repository.TravelRepository;
import com.travel.planning.service.TravelPlanningService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TravelPlanningServiceTest {
    @Mock
    CitiesRepository citiesRepository;
    @Mock
    ServicesRepository servicesRepository;
    @Mock
    TravelRepository travelRepository;

    @InjectMocks
    TravelPlanningService travelPlanningService;

    @Test
    void testCreateTravel() {
        var city = "Kiev";
        var time = LocalDateTime.of(2020, 12, 12, 12, 12, 12);
        var travelRequest = new TravelRequest(city, city, time);
        var user = User.builder()
                .user_id(1L)
                .email("misha@gmail.com")
                .password("1234")
                .role(Role.TRAVELER)
                .build();

        var expect = TravelDTO.builder().departure(city).destination(city).travel_time(time).build();

        when(citiesRepository.findCitiesByName(city))
                .thenReturn(Optional.ofNullable(Cities.builder().name(city).build()));

        assertThat(travelPlanningService.createTravel(travelRequest, user))
                .isEqualTo(expect);
    }

    @Test
    void testReplenishAccount_AlreadyPlanned() {
        var city = "Kiev";
        var time = LocalDateTime.of(2020, 12, 12, 12, 12, 12);
        var travelRequest = new TravelRequest(city, city, time);
        var user = User.builder()
                .user_id(1L)
                .email("misha@gmail.com")
                .password("1234")
                .role(Role.TRAVELER)
                .build();

        when(travelRepository.findTravelByUser(user))
                .thenReturn(Optional.of(new Travel()));

        assertThatThrownBy(() -> travelPlanningService.createTravel(travelRequest, user))
                .isInstanceOf(TravelException.class)
                .hasMessage("You have already planned your travel");
    }

    @Test
    void testReplenishAccount_NoCity() {
        var city = "Kiev";
        var time = LocalDateTime.of(2020, 12, 12, 12, 12, 12);
        var travelRequest = new TravelRequest(city, city, time);
        var user = User.builder()
                .user_id(1L)
                .email("misha@gmail.com")
                .password("1234")
                .role(Role.TRAVELER)
                .build();

        assertThatThrownBy(() -> travelPlanningService.createTravel(travelRequest, user))
                .isInstanceOf(TravelException.class)
                .hasMessage("We cannot pick you up from your city or deliver you to your destination");
    }

    @Test
    void testGetServices() {
        var user = User.builder()
                .user_id(1L)
                .email("misha@gmail.com")
                .password("1234")
                .role(Role.TRAVELER)
                .build();
        var city = Cities.builder().name("Kiev").build();
        var serviceOne = ServicesDTO.builder()
                .name("Hotel")
                .city(city.getName())
                .build();
        var serviceTwo = ServicesDTO.builder()
                .name("Park")
                .city(city.getName())
                .build();

        var expect = List.of(serviceOne, serviceTwo);

        when(travelRepository.findTravelByUser(user))
                .thenReturn(Optional.ofNullable(Travel.builder().destination(city).build()));
        when(servicesRepository.findAllByCity(city))
                .thenReturn(List.of(Services.builder().name("Hotel").city(city).build(),
                        Services.builder().name("Park").city(city).build()));

        assertThat(travelPlanningService.getServices(user))
                .isEqualTo(expect);
    }

    @Test
    void testGetServices_NoServices() {
        var user = User.builder()
                .user_id(1L)
                .email("misha@gmail.com")
                .password("1234")
                .role(Role.TRAVELER)
                .build();

        when(servicesRepository.findAll())
                .thenReturn(List.of());

        assertThatThrownBy(() -> travelPlanningService.getServices(user))
                .isInstanceOf(ServicesException.class)
                .hasMessage("No services in the city");
    }

    @Test
    void testBookService() {
        var serviceRequest = new ServiceRequest("Park");
        var user = User.builder()
                .user_id(1L)
                .email("misha@gmail.com")
                .password("1234")
                .role(Role.TRAVELER)
                .build();
        var cityB = Cities.builder().name("Berlin").build();
        var cityK = Cities.builder().name("Kiev").build();
        var time = LocalDateTime.of(2020, 12, 12, 12, 12, 12);

        var expect = TravelDTO.builder()
                        .departure(cityK.getName()).destination(cityB.getName()).travel_time(time)
                        .services(List.of(ServicesDTO.builder().city(cityB.getName()).name("Hotel").build(),
                                ServicesDTO.builder().city(cityB.getName()).name("Park").build()))
                        .build();

        when(travelRepository.findTravelByUser(user))
                .thenReturn(Optional.ofNullable(
                        Travel.builder()
                                .departure(cityK).destination(cityB).travel_time(time).user(user)
                                .services(new LinkedList<>(List.of(Services.builder().city(cityB).name("Hotel").build())))
                                .build()));
        when(servicesRepository.findByName("Park"))
                .thenReturn(Optional.ofNullable(Services.builder().city(cityB).name("Park").build()));

        assertThat(travelPlanningService.bookService(serviceRequest, user))
                .isEqualTo(expect);
    }

    @Test
    void testBookService_NoTrip() {
        var serviceRequest = new ServiceRequest("Hotel");
        var user = User.builder()
                .user_id(1L)
                .email("misha@gmail.com")
                .password("1234")
                .role(Role.TRAVELER)
                .build();

        assertThatThrownBy(() -> travelPlanningService.bookService(serviceRequest, user))
                .isInstanceOf(TravelException.class)
                .hasMessage("You haven't planned a travel");
    }

    @Test
    void testBookService_NoService() {
        var serviceRequest = new ServiceRequest("Hotel");
        var user = User.builder()
                .user_id(1L)
                .email("misha@gmail.com")
                .password("1234")
                .role(Role.TRAVELER)
                .build();

        when(travelRepository.findTravelByUser(user))
                .thenReturn(Optional.of(new Travel()));

        assertThatThrownBy(() -> travelPlanningService.bookService(serviceRequest, user))
                .isInstanceOf(ServicesException.class)
                .hasMessage("There is no service with that name");
    }

    @Test
    void testCompleteTravel() {
        var user = User.builder()
                .user_id(1L)
                .email("misha@gmail.com")
                .password("1234")
                .role(Role.TRAVELER)
                .build();

        when(travelRepository.findTravelByUser(user))
                .thenReturn(Optional.of(new Travel()));

        assertThat(travelPlanningService.completeTravel(user))
                .isEqualTo(true);
    }

    @Test
    void testCompleteTravel_NoTravel() {
        var user = User.builder()
                .user_id(1L)
                .email("misha@gmail.com")
                .password("1234")
                .role(Role.TRAVELER)
                .build();

        assertThat(travelPlanningService.completeTravel(user))
                .isEqualTo(false);
    }

    @Test
    void testDeleteTrips() {
        var deleteRequest = new DeleteRequest("Kiev", "Kiev");
        var user = User.builder()
                .user_id(1L)
                .email("misha@gmail.com")
                .password("1234")
                .role(Role.TRAVELER)
                .build();
        var cityK = Cities.builder().name("Kiev").build();
        var cityB = Cities.builder().name("Berlin").build();
        var time = LocalDateTime.of(2020, 12, 12, 12, 12, 12);

        var expect = List.of(TravelDTO.builder()
                .departure(cityK.getName()).destination(cityB.getName()).travel_time(time)
                .services(List.of(ServicesDTO.builder().city(cityB.getName()).name("Hotel").build()))
                .build());

        when(travelRepository.findAllByDeparture(cityK))
                .thenReturn(List.of(Travel.builder()
                        .departure(cityK).destination(cityB).user(user).travel_time(time)
                        .services(List.of(Services.builder().city(cityB).name("Hotel").build()))
                        .build()));
        when(travelRepository.findAllByDestination(cityK))
                .thenReturn(List.of());

        assertThat(travelPlanningService.deleteTrips(deleteRequest))
                .isEqualTo(expect);
    }

    @Test
    void testAddService() {
        var addService = new AddServiceRequest("Park", "Kiev");
        var city = Cities.builder().name("Kiev").build();
        var service = Services.builder().name("Park").city(city).build();
        var expect = ServicesDTO.builder().name("Park").city("Kiev").build();

        when(citiesRepository.save(city))
                .thenReturn(city);
        when(servicesRepository.save(service))
                .thenReturn(service);

        assertThat(travelPlanningService.addService(addService))
                .isEqualTo(expect);
    }

    @Test
    void testAddService_AlreadyExists() {
        var addService = new AddServiceRequest("Park", "Kiev");
        var city = Cities.builder().name("Kiev").build();

        when(citiesRepository.save(city))
                .thenReturn(city);
        when(servicesRepository.findByNameAndCity("Park", city))
                .thenReturn(Optional.of(new Services()));

        assertThatThrownBy(() -> travelPlanningService.addService(addService))
                .isInstanceOf(ServicesException.class)
                .hasMessage("The service already exists");
    }
}
