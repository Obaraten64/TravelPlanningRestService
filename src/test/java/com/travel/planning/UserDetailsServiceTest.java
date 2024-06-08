package com.travel.planning;

import com.travel.planning.configuration.security.UserAdapter;
import com.travel.planning.dto.request.RegistrationRequest;
import com.travel.planning.model.User;
import com.travel.planning.repository.UserRepository;
import com.travel.planning.service.UserDetailsServiceImp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserDetailsServiceTest {
    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    UserDetailsServiceImp userDetailsService;

    @Test
    void testRegistration() {
        ResponseEntity<String> expect = new ResponseEntity<>("Welcome! Your email is your username",
                HttpStatus.CREATED);
        RegistrationRequest registrationRequest = new RegistrationRequest(
                "misha@gmail.com",
                "1234",
                "traveler"
        );

        assertThat(userDetailsService.register(registrationRequest)).isEqualTo(expect);
    }

    @Test
    void testRegistration_AlreadyRegistered() {
        ResponseEntity<String> expect = new ResponseEntity<>("Such a user already exists!",
                HttpStatus.BAD_REQUEST);
        RegistrationRequest registrationRequest = new RegistrationRequest(
                "misha@gmail.com",
                "1234",
                "traveler"
        );

        when(userRepository.findUserByEmail(registrationRequest.getEmail()))
                .thenReturn(Optional.of(new User()));

        assertThat(userDetailsService.register(registrationRequest)).isEqualTo(expect);
    }

    @Test
    void testRegistration_WrongRole() {
        ResponseEntity<String> expect = new ResponseEntity<>("Wrong role provided",
                HttpStatus.BAD_REQUEST);
        RegistrationRequest registrationRequest = new RegistrationRequest(
                "misha@gmail.com",
                "1234",
                "megauser"
        );

        assertThat(userDetailsService.register(registrationRequest)).isEqualTo(expect);
    }

    @Test
    void testAuthentication() {
        String email = "misha@gmail.com";
        User user = new User();
        UserAdapter expect = new UserAdapter(user);

        when(userRepository.findUserByEmail(email))
                .thenReturn(Optional.of(user));

        assertThat(userDetailsService.loadUserByUsername(email)).isEqualTo(expect);
    }

    @Test
    void testAuthentication_NoUser() {
        String email = "misha@gmail.com";

        when(userRepository.findUserByEmail(email))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(email))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Not found!");
    }
}
