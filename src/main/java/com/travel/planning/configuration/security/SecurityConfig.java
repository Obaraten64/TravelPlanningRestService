package com.travel.planning.configuration.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    String[] allRoles = Arrays.stream(Role.values()).map(Enum::name).toArray(String[]::new);

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/travel/all").hasAuthority(Role.ADMIN.toString())
                        .requestMatchers(HttpMethod.DELETE, "/travel/delete")
                            .hasAuthority(Role.ADMIN.toString())
                        .requestMatchers(HttpMethod.POST, "/services/add")
                            .hasAuthority(Role.ADMIN.toString())
                        .requestMatchers("/travel/**").hasAnyAuthority(allRoles)
                        .requestMatchers("/services/**").hasAnyAuthority(allRoles)
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .anyRequest().denyAll()
                )
                .httpBasic(Customizer.withDefaults())     //to send basic auth in http
                .formLogin(Customizer.withDefaults())    //for default login form
                .csrf(AbstractHttpConfigurer::disable); // for POST requests via Postman;

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
