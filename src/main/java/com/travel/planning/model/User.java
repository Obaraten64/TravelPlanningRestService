package com.travel.planning.model;

import com.travel.planning.configuration.security.Role;

import jakarta.persistence.*;
import jakarta.persistence.Table;

import lombok.*;

@Entity
@Table(name = "user")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long user_id;
    private String email;
    private String password;
    private Role role;
}
