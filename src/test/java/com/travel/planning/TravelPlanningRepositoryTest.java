package com.travel.planning;

import com.travel.planning.configuration.security.Role;
import com.travel.planning.model.Cities;
import com.travel.planning.model.Services;
import com.travel.planning.model.Travel;
import com.travel.planning.model.User;
import com.travel.planning.repository.CitiesRepository;
import com.travel.planning.repository.ServicesRepository;
import com.travel.planning.repository.TravelRepository;
import com.travel.planning.repository.UserRepository;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(locations = {"classpath:testApp.properties"}) //for tests, it is better to use H2 db
@Testcontainers
public class TravelPlanningRepositoryTest {
    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:latest");

    @Autowired
    UserRepository userRepository;
    @Autowired
    CitiesRepository citiesRepository;
    @Autowired
    TravelRepository travelRepository;
    @Autowired
    ServicesRepository servicesRepository;


    @Autowired
    TestEntityManager entityManager;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry){
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Test
    void userRepository_SaveTest() {
        User expect = User.builder()
                .email("misha@gmail.com")
                .password("1234")
                .role(Role.ADMIN)
                .build();
        userRepository.save(expect);

        entityManager.clear(); //to clear cache

        Optional<User> actual = userRepository.findById(expect.getUser_id());
        assertThat(actual)
                .isPresent()
                .get()
                .isEqualTo(expect);
    }

    @Test
    @Sql(statements = {"INSERT INTO user(user_id, email, password, role) VALUES (1, 'misha@gmail.com', '1234', 1)"})
    void userRepository_FindByEmailTest() {
        User expect = User.builder()
                .user_id(1L)
                .email("misha@gmail.com")
                .password("1234")
                .role(Role.ADMIN)
                .build();

        Optional<User> actual = userRepository.findUserByEmail("misha@gmail.com");
        assertThat(actual)
                .isPresent()
                .get()
                .isEqualTo(expect);
    }

    @Test
    void citiesRepository_SaveTest() {
        Cities expect = Cities.builder()
                .name("Kiev")
                .build();
        citiesRepository.save(expect);

        entityManager.clear(); //to clear cache

        Optional<Cities> actual = citiesRepository.findById(expect.getId());
        assertThat(actual)
                .isPresent()
                .get()
                .isEqualTo(expect);
    }

    @Test
    @Sql(statements = {"INSERT INTO cities(id, name) VALUES (1, 'Kiev')"})
    void citiesRepository_FindCitiesByName() {
        Cities expect = Cities.builder()
                .id(1L)
                .name("Kiev")
                .build();

        Optional<Cities> actual = citiesRepository.findCitiesByName("Kiev");
        assertThat(actual)
                .isPresent()
                .get()
                .isEqualTo(expect);
    }

    @Test
    @Sql(statements = {"INSERT INTO cities(id, name) VALUES (1, 'Kiev')"})
    void servicesRepository_SaveTest() {
        Services expect = Services.builder()
                .name("Hotel")
                .city(Cities.builder().id(1L).name("Kiev").build())
                .build();
        servicesRepository.save(expect);

        entityManager.clear(); //to clear cache

        Optional<Services> actual = servicesRepository.findById(expect.getId());
        assertThat(actual)
                .isPresent()
                .get()
                .usingRecursiveComparison()
                .ignoringFields("travels") //because hibernate uses its own type of list
                .isEqualTo(expect);
        assertThat(actual.get().getTravels()).isEmpty();
    }

    @Test
    @Sql(statements = {"INSERT INTO cities(id, name) VALUES (1, 'Kiev')",
            "INSERT INTO user(user_id, email, password, role) VALUES (1, 'misha@gmail.com', '1234', 0)",
            "INSERT INTO travel(id, travel_time, user_id, departure, destination) " +
                    "VALUES (1, '2020-12-12 12:12:12', 1, 'Kiev', 'Kiev')",
            "INSERT INTO services(id, city, name) VALUES (1, 'Kiev', 'Hotel'), (2, 'Kiev', 'Park')",
            "INSERT INTO travel_services(id, service_id, travel_id) VALUES (1, 1, 1)"})
    void servicesRepository_FindAllByCity() {
        Cities city = Cities.builder().id(1L).name("Kiev").build();
        User user = User.builder()
                .user_id(1L)
                .email("misha@gmail.com")
                .password("1234")
                .role(Role.TRAVELER)
                .build();
        Travel travel = Travel.builder()
                .id(1L)
                .travel_time(LocalDateTime.of(2020, 12, 12, 12, 12, 12))
                .user(user)
                .destination(city)
                .departure(city)
                .build();

        Services expectOne = Services.builder()
                .id(1L)
                .name("Hotel")
                .city(city)
                .travels(List.of(travel))
                .build();
        Services expectTwo = Services.builder()
                .id(2L)
                .name("Park")
                .city(city)
                .build();
        List<Services> expect = List.of(expectOne, expectTwo);

        List<Services> actual = servicesRepository.findAllByCity(city);

        assertThat(actual)
                .hasOnlyElementsOfType(Services.class)
                .hasSize(2)
                .usingRecursiveComparison()
                .ignoringFields("travels")
                .isEqualTo(expect);

        assertThat(actual.get(1).getTravels()).isEmpty();

        assertThat(actual.get(0).getTravels())
                .usingRecursiveComparison()
                .ignoringFields("services")//stackOverflow
                .isEqualTo(List.of(travel));
    }

    @Test
    @Sql(statements = {"INSERT INTO cities(id, name) VALUES (1, 'Kiev')",
            "INSERT INTO services(id, city, name) VALUES (1, 'Kiev', 'Hotel'), (2, 'Kiev', 'Park')"})
    void servicesRepository_FindByName() {
        Services expect = Services.builder()
                .id(1L)
                .name("Hotel")
                .city(Cities.builder().id(1L).name("Kiev").build())
                .build();

        Optional<Services> actual = servicesRepository.findByName("Hotel");
        assertThat(actual)
                .isPresent()
                .get()
                .usingRecursiveComparison()
                .ignoringFields("travels") //because hibernate uses its own type of list
                .isEqualTo(expect);
        assertThat(actual.get().getTravels()).isEmpty();
    }

    @Test
    @Sql(statements = {"INSERT INTO cities(id, name) VALUES (1, 'Kiev')",
            "INSERT INTO services(id, city, name) VALUES (1, 'Kiev', 'Hotel'), (2, 'Kiev', 'Park')"})
    void servicesRepository_FindByNameAndCity() {
        Cities city = Cities.builder().id(1L).name("Kiev").build();
        Services expect = Services.builder()
                .id(1L)
                .name("Hotel")
                .city(city)
                .build();

        Optional<Services> actual = servicesRepository.findByNameAndCity("Hotel", city);
        assertThat(actual)
                .isPresent()
                .get()
                .usingRecursiveComparison()
                .ignoringFields("travels") //because hibernate uses its own type of list
                .isEqualTo(expect);
        assertThat(actual.get().getTravels()).isEmpty();
    }

    @Test
    @Sql(statements = {"INSERT INTO cities(id, name) VALUES (1, 'Kiev')",
            "INSERT INTO user(user_id, email, password, role) VALUES (1, 'misha@gmail.com', '1234', 0)"})
    void travelRepository_SaveTest() {
        Cities city = Cities.builder().id(1L).name("Kiev").build();
        Travel expect = Travel.builder()
                .travel_time(LocalDateTime.of(2020, 12, 12, 12, 12, 12))
                .departure(city)
                .destination(city)
                .user(User.builder().user_id(1L).email("misha@gmail.com")
                        .password("1234").role(Role.TRAVELER).build())
                .build();
        travelRepository.save(expect);

        entityManager.clear(); //to clear cache

        Optional<Travel> actual = travelRepository.findById(expect.getId());
        assertThat(actual)
                .isPresent()
                .get()
                .usingRecursiveComparison()
                .ignoringFields("services") //because hibernate uses its own type of list
                .isEqualTo(expect);
        assertThat(actual.get().getServices()).isEmpty();
    }

    @Test
    @Sql(statements = {"INSERT INTO cities(id, name) VALUES (1, 'Kiev')",
            "INSERT INTO user(user_id, email, password, role) VALUES (1, 'misha@gmail.com', '1234', 0), " +
                    "(2, 'misha2@gmail.com', '1234', 1)",
            "INSERT INTO travel(id, travel_time, user_id, departure, destination) " +
                    "VALUES (1, '2020-12-12 12:12:12', 1, 'Kiev', 'Kiev'), (2, '2020-12-12 12:12:12', 2, 'Kiev', 'Kiev')",
            "INSERT INTO services(id, city, name) VALUES (1, 'Kiev', 'Hotel'), (2, 'Kiev', 'Park')",
            "INSERT INTO travel_services(id, service_id, travel_id) VALUES (1, 1, 1)"})
    void travelRepository_FindTravelByUser() {
        Cities city = Cities.builder().id(1L).name("Kiev").build();
        User user = User.builder()
                .user_id(1L)
                .email("misha@gmail.com")
                .password("1234")
                .role(Role.TRAVELER)
                .build();
        Services service = Services.builder()
                .id(1L)
                .name("Hotel")
                .city(city)
                .build();

        Travel expect = Travel.builder()
                .id(1L)
                .travel_time(LocalDateTime.of(2020, 12, 12, 12, 12, 12))
                .user(user)
                .destination(city)
                .departure(city)
                .services(List.of(service))
                .build();

        Optional<Travel> actual = travelRepository.findTravelByUser(user);

        assertThat(actual)
                .isPresent()
                .get()
                .usingRecursiveComparison()
                .ignoringFields("services")
                .isEqualTo(expect);

        assertThat(actual.get().getServices())
                .usingRecursiveComparison()
                .ignoringFields("travels")//stackOverflow
                .isEqualTo(List.of(service));
    }

    @Test
    @Sql(statements = {"INSERT INTO cities(id, name) VALUES (1, 'Kiev')",
            "INSERT INTO user(user_id, email, password, role) VALUES (1, 'misha@gmail.com', '1234', 0), " +
                    "(2, 'misha2@gmail.com', '1234', 1)",
            "INSERT INTO travel(id, travel_time, user_id, departure, destination) " +
                    "VALUES (1, '2020-12-12 12:12:12', 1, 'Kiev', 'Kiev'), (2, '2020-12-12 12:12:12', 2, 'Kiev', 'Kiev')"})
    void travelRepository_FindAllByDeparture() {
        Cities city = Cities.builder().id(1L).name("Kiev").build();

        Travel expectOne = Travel.builder()
                .id(1L)
                .travel_time(LocalDateTime.of(2020, 12, 12, 12, 12, 12))
                .user(User.builder().user_id(1L).email("misha@gmail.com")
                        .password("1234").role(Role.TRAVELER).build())
                .destination(city)
                .departure(city)
                .build();
        Travel expectTwo = Travel.builder()
                .id(2L)
                .travel_time(LocalDateTime.of(2020, 12, 12, 12, 12, 12))
                .user(User.builder().user_id(2L).email("misha2@gmail.com")
                        .password("1234").role(Role.ADMIN).build())
                .destination(city)
                .departure(city)
                .build();
        List<Travel> expect = List.of(expectOne, expectTwo);

        List<Travel> actual = travelRepository.findAllByDeparture(city);

        assertThat(actual)
                .hasOnlyElementsOfType(Travel.class)
                .hasSize(2)
                .usingRecursiveComparison()
                .ignoringFields("services")
                .isEqualTo(expect);
        assertThat(actual.get(0).getServices()).isEmpty();
        assertThat(actual.get(1).getServices()).isEmpty();
    }

    @Test
    @Sql(statements = {"INSERT INTO cities(id, name) VALUES (1, 'Kiev'), (2, 'Warsaw')",
            "INSERT INTO user(user_id, email, password, role) VALUES (1, 'misha@gmail.com', '1234', 0), " +
                    "(2, 'misha2@gmail.com', '1234', 1)",
            "INSERT INTO travel(id, travel_time, user_id, departure, destination) " +
                    "VALUES (1, '2020-12-12 12:12:12', 1, 'Kiev', 'Warsaw'), (2, '2020-12-12 12:12:12', 2, 'Kiev', 'Kiev')"})
    void travelRepository_FindAllByDestination() {
        Cities city = Cities.builder().id(2L).name("Warsaw").build();
        Travel expectOne = Travel.builder()
                .id(1L)
                .travel_time(LocalDateTime.of(2020, 12, 12, 12, 12, 12))
                .user(User.builder().user_id(1L).email("misha@gmail.com")
                        .password("1234").role(Role.TRAVELER).build())
                .destination(city)
                .departure(Cities.builder().id(1L).name("Kiev").build())
                .build();
        List<Travel> expect = List.of(expectOne);

        List<Travel> actual = travelRepository.findAllByDestination(city);

        assertThat(actual)
                .hasOnlyElementsOfType(Travel.class)
                .hasSize(1)
                .usingRecursiveComparison()
                .ignoringFields("services")
                .isEqualTo(expect);
        assertThat(actual.get(0).getServices()).isEmpty();
    }
}
