package com.travel.planning;

import com.travel.planning.configuration.security.Role;
import com.travel.planning.configuration.security.SecurityConfig;
import com.travel.planning.configuration.security.UserAdapter;
import com.travel.planning.controller.TravelPlanningController;
import com.travel.planning.dto.request.*;
import com.travel.planning.dto.response.*;
import com.travel.planning.exception.ServicesException;
import com.travel.planning.exception.TravelException;
import com.travel.planning.model.User;
import com.travel.planning.service.TravelPlanningService;
import com.travel.planning.service.UserDetailsServiceImp;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TravelPlanningController.class)
@Import(SecurityConfig.class)
public class TravelPlanningControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    TravelPlanningService travelPlanningService;
    @MockBean
    UserDetailsServiceImp userDetailsService;

    @Autowired
    ObjectMapper mapper;

    @Test
    void testRegisterEndpoint() throws Exception {
        var registrationRequest = new RegistrationRequest(
                "misha@gmail.com",
                "1234",
                "traveler"
        );
        var responseEntity = new ResponseEntity<>("Welcome! Your email is your username",
                HttpStatus.CREATED);

        when(userDetailsService.register(registrationRequest))
                .thenReturn(responseEntity);

        var requestBuilder = post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(registrationRequest));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isCreated())
                .andExpect(content().string("Welcome! Your email is your username"));
    }

    @Test
    void testRegisterEndpoint_WrongRole() throws Exception {
        var registrationRequest = new RegistrationRequest(
                "misha@gmail.com",
                "1234",
                "provider"
        );
        var responseEntity = new ResponseEntity<>("Wrong role provided",
                HttpStatus.BAD_REQUEST);

        when(userDetailsService.register(registrationRequest))
                .thenReturn(responseEntity);

        var requestBuilder = post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(registrationRequest));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Wrong role provided"));
    }

    @Test
    void testRegisterEndpoint_AlreadyRegistered() throws Exception {
        var registrationRequest = new RegistrationRequest(
                "misha@gmail.com",
                "1234",
                "traveler"
        );
        var responseEntity = new ResponseEntity<>("Such a user already exists!",
                HttpStatus.BAD_REQUEST);

        when(userDetailsService.register(registrationRequest))
                .thenReturn(responseEntity);

        var requestBuilder = post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(registrationRequest));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Such a user already exists!"));
    }

    @Test
    void testCreateTravelEndpoint() throws Exception {
        var time = LocalDateTime.of(2020, 12, 12, 12, 12, 12);
        var travelRequest = new TravelRequest("Kiev", "Berlin", time);
        var user = User.builder()
                .email("misha@gmail.com")
                .password("1234")
                .role(Role.TRAVELER)
                .build();

        var expect = TravelDTO.builder().departure("Kiev").destination("Berlin").travel_time(time).build();

        when(travelPlanningService.createTravel(travelRequest, user)).thenReturn(expect);
        SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
                new UserAdapter(user), null, List.of(new SimpleGrantedAuthority(user.getRole().toString()))
        ));

        var requestBuilder = post("/travel/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(travelRequest));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isCreated())
                .andExpect(content().string((mapper.writeValueAsString(expect))));
    }

    @Test
    void testCreateTravelEndpoint_AlreadyPlanned() throws Exception {
        var time = LocalDateTime.of(2020, 12, 12, 12, 12, 12);
        var travelRequest = new TravelRequest("Kiev", "Berlin", time);
        var user = User.builder()
                .email("misha@gmail.com")
                .password("1234")
                .role(Role.TRAVELER)
                .build();

        when(travelPlanningService.createTravel(travelRequest, user))
                .thenThrow(new TravelException("You have already planned your travel"));
        SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
                new UserAdapter(user), null, List.of(new SimpleGrantedAuthority(user.getRole().toString()))
        ));

        var requestBuilder = post("/travel/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(travelRequest));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("You have already planned your travel"));
    }

    @Test
    void testCreateTravelEndpoint_NoCity() throws Exception {
        var time = LocalDateTime.of(2020, 12, 12, 12, 12, 12);
        var travelRequest = new TravelRequest("Kiev", "Berlin", time);
        var user = User.builder()
                .email("misha@gmail.com")
                .password("1234")
                .role(Role.TRAVELER)
                .build();

        when(travelPlanningService.createTravel(travelRequest, user))
                .thenThrow(new TravelException("We cannot pick you up from your city or deliver you to your destination"));
        SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
                new UserAdapter(user), null, List.of(new SimpleGrantedAuthority(user.getRole().toString()))
        ));

        var requestBuilder = post("/travel/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(travelRequest));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("We cannot pick you up from your city" +
                        " or deliver you to your destination"));
    }

    @Test
    void testGetServices() throws Exception {
        var user = User.builder()
                .email("misha@gmail.com")
                .password("1234")
                .role(Role.TRAVELER)
                .build();

        var expect = List.of(ServicesDTO.builder().city("Berlin").name("Hotel").build(),
                ServicesDTO.builder().city("Berlin").name("Park").build());

        when(travelPlanningService.getServices(user)).thenReturn(expect);
        SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
                new UserAdapter(user), null, List.of(new SimpleGrantedAuthority(user.getRole().toString()))
        ));

        var requestBuilder = get("/services");
        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().string((mapper.writeValueAsString(expect))));
    }

    @Test
    void testGetServices_NoServices() throws Exception {
        var user = User.builder()
                .email("misha@gmail.com")
                .password("1234")
                .role(Role.TRAVELER)
                .build();

        when(travelPlanningService.getServices(user))
                .thenThrow(new ServicesException("No services in the city"));
        SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
                new UserAdapter(user), null, List.of(new SimpleGrantedAuthority(user.getRole().toString()))
        ));

        var requestBuilder = get("/services");
        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No services in the city"));
    }

    @Test
    void testBookServiceEndpoint() throws Exception {
        var serviceRequest = new ServiceRequest("Hotel");
        var user = User.builder()
                .email("misha@gmail.com")
                .password("1234")
                .role(Role.TRAVELER)
                .build();
        var time = LocalDateTime.of(2020, 12, 12, 12, 12, 12);
        var expect = TravelDTO.builder().departure("Berlin").destination("Kiev").travel_time(time)
                .services(List.of(ServicesDTO.builder().name("Park").city("Kiev").build(),
                        ServicesDTO.builder().name("Hotel").city("Kiev").build())).build();

        when(travelPlanningService.bookService(serviceRequest, user))
                .thenReturn(expect);
        SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
                new UserAdapter(user), null, List.of(new SimpleGrantedAuthority(user.getRole().toString()))
        ));

        var requestBuilder = post("/services/book")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(serviceRequest));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().string((mapper.writeValueAsString(expect))));
    }

    @Test
    void testBookServiceEndpoint_NoTravel() throws Exception {
        var serviceRequest = new ServiceRequest("Hotel");
        var user = User.builder()
                .email("misha@gmail.com")
                .password("1234")
                .role(Role.TRAVELER)
                .build();

        when(travelPlanningService.bookService(serviceRequest, user))
                .thenThrow(new TravelException("You haven't planned a travel"));
        SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
                new UserAdapter(user), null, List.of(new SimpleGrantedAuthority(user.getRole().toString()))
        ));

        var requestBuilder = post("/services/book")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(serviceRequest));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("You haven't planned a travel"));
    }

    @Test
    void testBookServiceEndpoint_NoService() throws Exception {
        var serviceRequest = new ServiceRequest("Hotel");
        var user = User.builder()
                .email("misha@gmail.com")
                .password("1234")
                .role(Role.TRAVELER)
                .build();

        when(travelPlanningService.bookService(serviceRequest, user))
                .thenThrow(new ServicesException("There is no service with that name"));
        SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
                new UserAdapter(user), null, List.of(new SimpleGrantedAuthority(user.getRole().toString()))
        ));

        var requestBuilder = post("/services/book")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(serviceRequest));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("There is no service with that name"));
    }

    @Test
    void testCompleteTravel() throws Exception {
        var user = User.builder()
                .email("misha@gmail.com")
                .password("1234")
                .role(Role.TRAVELER)
                .build();

        when(travelPlanningService.completeTravel(user)).thenReturn(true);
        SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
                new UserAdapter(user), null, List.of(new SimpleGrantedAuthority(user.getRole().toString()))
        ));

        var requestBuilder = post("/travel/complete");
        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().string("What a beautiful trip"));
    }

    @Test
    void testCompleteTravel_NoTravel() throws Exception {
        var user = User.builder()
                .email("misha@gmail.com")
                .password("1234")
                .role(Role.TRAVELER)
                .build();

        when(travelPlanningService.completeTravel(user)).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
                new UserAdapter(user), null, List.of(new SimpleGrantedAuthority(user.getRole().toString()))
        ));

        var requestBuilder = post("/travel/complete");
        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andExpect(content().string("You haven't planned a travel"));
    }

    @Test
    @WithMockUser(username = "misha@gmail.com", password = "1234", authorities = "ADMIN")
    void testGetTravels() throws Exception {
        var time = LocalDateTime.of(2020, 12, 12, 12, 12, 12);
        var services = List.of(ServicesDTO.builder().city("Kiev").name("Hotel").build());

        var expect = List.of(TravelDTO.builder().destination("Kiev").departure("Berlin")
                        .travel_time(time).services(services).build(),
                TravelDTO.builder().destination("Kiev").departure("Kiev").travel_time(time).services(services).build());

        when(travelPlanningService.getTravels()).thenReturn(expect);

        var requestBuilder = get("/travel/all");
        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().string((mapper.writeValueAsString(expect))));
    }

    @Test
    @WithMockUser(username = "misha@gmail.com", password = "1234", authorities = "ADMIN")
    void testDeleteTrips() throws Exception {
        var deleteRequest = new DeleteRequest("Kiev", "Kiev");
        var time = LocalDateTime.of(2020, 12, 12, 12, 12, 12);
        var services = List.of(ServicesDTO.builder().city("Kiev").name("Hotel").build());

        var expect = List.of(TravelDTO.builder().destination("Kiev").departure("Berlin")
                        .travel_time(time).services(services).build(),
                TravelDTO.builder().destination("Kiev").departure("Kiev").travel_time(time).services(services).build());

        when(travelPlanningService.deleteTrips(deleteRequest))
                .thenReturn(expect);

        var requestBuilder = delete("/travel/delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(deleteRequest));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().string((mapper.writeValueAsString(expect))));
    }

    @Test
    @WithMockUser(username = "misha@gmail.com", password = "1234", authorities = "ADMIN")
    void testAddService() throws Exception {
        var addService = new AddServiceRequest("Hotel", "Kiev");

        var expect = ServicesDTO.builder().name("Hotel").city("Kiev").build();

        when(travelPlanningService.addService(addService)).thenReturn(expect);

        var requestBuilder = post("/services/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(addService));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().string((mapper.writeValueAsString(expect))));
    }

    @Test
    @WithMockUser(username = "misha@gmail.com", password = "1234", authorities = "ADMIN")
    void testAddService_AlreadyExists() throws Exception {
        var addService = new AddServiceRequest("Hotel", "Kiev");

        when(travelPlanningService.addService(addService))
                .thenThrow(new ServicesException("The service already exists"));

        var requestBuilder = post("/services/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(addService));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("The service already exists"));
    }

    @Test
    @WithMockUser(username = "email@gmail.com", password = "1234", authorities = "ADMIN")
    void testValidation() throws Exception {
        var requestNoService = post("/services/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"city\":\"Kiev\"}");
        mockMvc.perform(requestNoService)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Write down the name of service!"));

        var requestNoRole = post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"misha@gmail.com\",\"password\":\"1234\"}");
        mockMvc.perform(requestNoRole)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Write down your role!"));

        var requestNoTime = post("/travel/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"departure\":\"Kiev\",\"destination\":\"Kiev\"}");
        mockMvc.perform(requestNoTime)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Choose the date and time for your travel!"));
    }
}
