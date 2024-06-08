package com.travel.planning;

import com.travel.planning.dto.request.*;
import com.travel.planning.dto.response.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(locations = {"classpath:testApp.properties"})
@Testcontainers
class TravelPlanningIT {
	@Container
	private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:latest");

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper mapper;

	final RequestPostProcessor postProcessor = SecurityMockMvcRequestPostProcessors
			.httpBasic("misha@gmail.com", "1234");
	final String createTraveler = "INSERT INTO user(user_id, email, password, role) " +
			"VALUES (1, 'misha@gmail.com', '$2a$10$Hzdg8upvCxY8wqZAyq79Ou1szV6sS6Xy55GmDyOqgz8ZKbMsklZ1C', 0)";
	final String createAdmin =  "INSERT INTO user(user_id, email, password, role) " +
			"VALUES (1, 'misha@gmail.com', '$2a$10$Hzdg8upvCxY8wqZAyq79Ou1szV6sS6Xy55GmDyOqgz8ZKbMsklZ1C', 1)";

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry){
		registry.add("spring.datasource.url", mysql::getJdbcUrl);
		registry.add("spring.datasource.username", mysql::getUsername);
		registry.add("spring.datasource.password", mysql::getPassword);
	}

	@Test
	void testRegisterEndpoint() throws Exception {
		var registrationRequest = new RegistrationRequest(
				"misha@gmail.com",
				"1234",
				"traveler"
		);

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

		var requestBuilder = post("/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(registrationRequest));
		mockMvc.perform(requestBuilder)
				.andExpect(status().isBadRequest())
				.andExpect(content().string("Wrong role provided"));
	}

	@Test
	@Sql(statements = createTraveler)
	void testRegisterEndpoint_AlreadyRegistered() throws Exception {
		var registrationRequest = new RegistrationRequest(
				"misha@gmail.com",
				"1234",
				"traveler"
		);

		var requestBuilder = post("/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(registrationRequest));
		mockMvc.perform(requestBuilder)
				.andExpect(status().isBadRequest())
				.andExpect(content().string("Such a user already exists!"));
	}

	@Test
	@Sql(statements = {createTraveler, "INSERT INTO cities(id, name) VALUES (1, 'Kiev'), (2, 'Berlin')"})
	void testCreateTravelEndpoint() throws Exception {
		var time = LocalDateTime.of(2020, 12, 12, 12, 12, 12);
		var travelRequest = new TravelRequest("Kiev", "Berlin", time);

		var expect = TravelDTO.builder().departure("Kiev").destination("Berlin").travel_time(time).build();

		var requestBuilder = post("/travel/create")
				.with(postProcessor)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(travelRequest));
		mockMvc.perform(requestBuilder)
				.andExpect(status().isCreated())
				.andExpect(content().string((mapper.writeValueAsString(expect))));
	}

	@Test
	@Sql(statements = {createTraveler, "INSERT INTO cities(id, name) VALUES (1, 'Kiev'), (2, 'Berlin')",
			"INSERT INTO travel(id, user_id, travel_time, departure, destination) " +
					"VALUES (1, 1, '2020-12-12 12:12:12', 'Kiev', 'Berlin')"})
	void testCreateTravelEndpoint_AlreadyPlanned() throws Exception {
		var time = LocalDateTime.of(2020, 12, 12, 12, 12, 12);
		var travelRequest = new TravelRequest("Kiev", "Berlin", time);

		var requestBuilder = post("/travel/create")
				.with(postProcessor)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(travelRequest));
		mockMvc.perform(requestBuilder)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("You have already planned your travel"));
	}

	@Test
	@Sql(statements = {createTraveler, "INSERT INTO cities(id, name) VALUES (1, 'Kiev')"})
	void testCreateTravelEndpoint_NoCity() throws Exception {
		var time = LocalDateTime.of(2020, 12, 12, 12, 12, 12);
		var travelRequest = new TravelRequest("Kiev", "Berlin", time);

		var requestBuilder = post("/travel/create")
				.with(postProcessor)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(travelRequest));
		mockMvc.perform(requestBuilder)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("We cannot pick you up from your city" +
						" or deliver you to your destination"));
	}

	@Test
	@Sql(statements = {createAdmin, "INSERT INTO cities(id, name) VALUES (1, 'Berlin')",
			"INSERT INTO services(id, name, city) VALUES (1, 'Hotel', 'Berlin'), (2, 'Park', 'Berlin')"})
	void testGetServices() throws Exception {
		var expect = List.of(ServicesDTO.builder().city("Berlin").name("Hotel").build(),
				ServicesDTO.builder().city("Berlin").name("Park").build());

		var requestBuilder = get("/services").with(postProcessor);
		mockMvc.perform(requestBuilder)
				.andExpect(status().isOk())
				.andExpect(content().string((mapper.writeValueAsString(expect))));
	}

	@Test
	@Sql(statements = createAdmin)
	void testGetServices_NoServices() throws Exception {
		var requestBuilder = get("/services").with(postProcessor);
		mockMvc.perform(requestBuilder)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("No services in the city"));
	}

	@Test
	@Sql(statements = {createTraveler, "INSERT INTO cities(id, name) VALUES (1, 'Kiev'), (2, 'Berlin')",
			"INSERT INTO travel(id, user_id, travel_time, departure, destination) " +
					"VALUES (1, 1, '2020-12-12 12:12:12', 'Berlin', 'Kiev')",
			"INSERT INTO services(id, name, city) VALUES (1, 'Hotel', 'Kiev'), (2, 'Park', 'Kiev')",
			"INSERT INTO travel_services(id, travel_id, service_id) VALUES (1, 1, 2)"})
	void testBookServiceEndpoint() throws Exception {
		var serviceRequest = new ServiceRequest("Hotel");
		var time = LocalDateTime.of(2020, 12, 12, 12, 12, 12);
		var expect = TravelDTO.builder().departure("Berlin").destination("Kiev").travel_time(time)
				.services(List.of(ServicesDTO.builder().name("Park").city("Kiev").build(),
						ServicesDTO.builder().name("Hotel").city("Kiev").build())).build();

		var requestBuilder = post("/services/book")
				.with(postProcessor)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(serviceRequest));
		mockMvc.perform(requestBuilder)
				.andExpect(status().isOk())
				.andExpect(content().string((mapper.writeValueAsString(expect))));
	}

	@Test
	@Sql(statements = createTraveler)
	void testBookServiceEndpoint_NoTravel() throws Exception {
		var serviceRequest = new ServiceRequest("Hotel");

		var requestBuilder = post("/services/book")
				.with(postProcessor)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(serviceRequest));
		mockMvc.perform(requestBuilder)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("You haven't planned a travel"));
	}

	@Test
	@Sql(statements = {createTraveler, "INSERT INTO cities(id, name) VALUES (1, 'Kiev'), (2, 'Berlin')",
			"INSERT INTO travel(id, user_id, travel_time, departure, destination) " +
					"VALUES (1, 1, '2020-12-12 12:12:12', 'Berlin', 'Kiev')",
			"INSERT INTO services(id, name, city) VALUES (2, 'Park', 'Kiev')",
			"INSERT INTO travel_services(id, travel_id, service_id) VALUES (1, 1, 2)"})
	void testBookServiceEndpoint_NoService() throws Exception {
		var serviceRequest = new ServiceRequest("Hotel");

		var requestBuilder = post("/services/book")
				.with(postProcessor)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(serviceRequest));
		mockMvc.perform(requestBuilder)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("There is no service with that name"));
	}

	@Test
	@Sql(statements = {createTraveler, "INSERT INTO cities(id, name) VALUES (1, 'Kiev'), (2, 'Berlin')",
			"INSERT INTO travel(id, user_id, travel_time, departure, destination) " +
					"VALUES (1, 1, '2020-12-12 12:12:12', 'Berlin', 'Kiev')"})
	void testCompleteTravel() throws Exception {
		var requestBuilder = post("/travel/complete").with(postProcessor);
		mockMvc.perform(requestBuilder)
				.andExpect(status().isOk())
				.andExpect(content().string("What a beautiful trip"));
	}

	@Test
	@Sql(statements = createTraveler)
	void testCompleteTravel_NoTravel() throws Exception {
		var requestBuilder = post("/travel/complete").with(postProcessor);
		mockMvc.perform(requestBuilder)
				.andExpect(status().isBadRequest())
				.andExpect(content().string("You haven't planned a travel"));
	}

	@Test
	@Sql(statements = {createAdmin, "INSERT INTO cities(id, name) VALUES (1, 'Kiev'), (2, 'Berlin')",
			"INSERT INTO user(user_id, email, password, role) " +
					"VALUES (2, 'misha2@gmail.com', '$2a$10$Hzdg8upvCxY8wqZAyq79Ou1szV6sS6Xy55GmDyOqgz8ZKbMsklZ1C', 0)",
			"INSERT INTO travel(id, user_id, travel_time, departure, destination) " +
					"VALUES (1, 1, '2020-12-12 12:12:12', 'Berlin', 'Kiev'), (2, 2, '2020-12-12 12:12:12', 'Kiev', 'Kiev')",
			"INSERT INTO services(id, name, city) VALUES (1, 'Hotel', 'Kiev')",
			"INSERT INTO travel_services(id, travel_id, service_id) VALUES (1, 1, 1), (2, 2, 1)"})
	void testGetTravels() throws Exception {
		var time = LocalDateTime.of(2020, 12, 12, 12, 12, 12);
		var services = List.of(ServicesDTO.builder().city("Kiev").name("Hotel").build());

		var expect = List.of(TravelDTO.builder().destination("Kiev").departure("Berlin")
						.travel_time(time).services(services).build(),
				TravelDTO.builder().destination("Kiev").departure("Kiev").travel_time(time).services(services).build());

		var requestBuilder = get("/travel/all").with(postProcessor);
		mockMvc.perform(requestBuilder)
				.andExpect(status().isOk())
				.andExpect(content().string((mapper.writeValueAsString(expect))));
	}

	@Test
	@Sql(statements = {createAdmin, "INSERT INTO cities(id, name) VALUES (1, 'Kiev'), (2, 'Berlin')",
			"INSERT INTO user(user_id, email, password, role) " +
					"VALUES (2, 'misha2@gmail.com', '$2a$10$Hzdg8upvCxY8wqZAyq79Ou1szV6sS6Xy55GmDyOqgz8ZKbMsklZ1C', 0)",
			"INSERT INTO travel(id, user_id, travel_time, departure, destination) " +
					"VALUES (1, 1, '2020-12-12 12:12:12', 'Berlin', 'Kiev'), (2, 2, '2020-12-12 12:12:12', 'Kiev', 'Kiev')",
			"INSERT INTO services(id, name, city) VALUES (1, 'Hotel', 'Kiev')",
			"INSERT INTO travel_services(id, travel_id, service_id) VALUES (1, 1, 1), (2, 2, 1)"})
	void testDeleteTrips() throws Exception {
		var deleteRequest = new DeleteRequest("Kiev", "Kiev");
		var time = LocalDateTime.of(2020, 12, 12, 12, 12, 12);
		var services = List.of(ServicesDTO.builder().city("Kiev").name("Hotel").build());

		var expect = List.of(TravelDTO.builder().destination("Kiev").departure("Kiev")
						.travel_time(time).services(services).build(),
				TravelDTO.builder().destination("Kiev").departure("Berlin").travel_time(time).services(services).build());

		var requestBuilder = delete("/travel/delete")
				.with(postProcessor)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(deleteRequest));
		mockMvc.perform(requestBuilder)
				.andExpect(status().isOk())
				.andExpect(content().string((mapper.writeValueAsString(expect))));
	}

	@Test
	@Sql(statements = createAdmin)
	void testAddService() throws Exception {
		var addService = new AddServiceRequest("Hotel", "Kiev");

		var expect = ServicesDTO.builder().name("Hotel").city("Kiev").build();

		var requestBuilder = post("/services/add")
				.with(postProcessor)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(addService));
		mockMvc.perform(requestBuilder)
				.andExpect(status().isOk())
				.andExpect(content().string((mapper.writeValueAsString(expect))));
	}

	@Test
	@Sql(statements = {createAdmin, "INSERT INTO cities(id, name) VALUES (1, 'Kiev')",
			"INSERT INTO services(id, name, city) VALUES (1, 'Hotel', 'Kiev')"})
	void testAddService_AlreadyExists() throws Exception {
		var addService = new AddServiceRequest("Hotel", "Kiev");

		var requestBuilder = post("/services/add")
				.with(postProcessor)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(addService));
		mockMvc.perform(requestBuilder)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("The service already exists"));
	}
}
