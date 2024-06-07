package com.travel.planning.service;

import com.travel.planning.configuration.security.Role;
import com.travel.planning.configuration.security.UserAdapter;
import com.travel.planning.dto.request.RegistrationRequest;
import com.travel.planning.model.User;
import com.travel.planning.repository.UserRepository;

import lombok.AllArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@AllArgsConstructor
public class UserDetailsServiceImp implements UserDetailsService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public ResponseEntity<String> register(RegistrationRequest registrationRequest) {
        if (userRepository.findUserByEmail(registrationRequest.getEmail()).isPresent()) {
            return new ResponseEntity<>("Such a user already exists!",
                    HttpStatus.BAD_REQUEST);
        }

        if (Arrays.stream(Role.values()).noneMatch(x -> x.name().equals(registrationRequest.getRole().toUpperCase()))){
            return new ResponseEntity<>("Wrong role provided",
                    HttpStatus.BAD_REQUEST);
        }
        Role role = Role.valueOf((registrationRequest.getRole().toUpperCase()));

        User user = User.builder()
                .email(registrationRequest.getEmail())
                .password(passwordEncoder.encode(registrationRequest.getPassword()))
                .role(role)
                .build();
        userRepository.save(user);

        return new ResponseEntity<>("Successfully registered, your email is your username", HttpStatus.OK);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findUserByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Not found!"));

        return new UserAdapter(user);
    }
}
