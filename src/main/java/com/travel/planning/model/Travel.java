package com.travel.planning.model;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "travel")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Travel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String departure;
    private String destination;
    private LocalDateTime travel_time;
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;
}
