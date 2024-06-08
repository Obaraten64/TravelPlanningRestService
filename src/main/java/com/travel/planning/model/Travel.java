package com.travel.planning.model;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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
    @ManyToOne
    @JoinColumn(name = "departure", referencedColumnName = "name")
    private Cities departure;
    @ManyToOne
    @JoinColumn(name = "destination", referencedColumnName = "name")
    private Cities destination;
    private LocalDateTime travel_time;
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;
    @OneToMany
    @JoinColumn(name = "travel_id")
    private List<Services> services;
}
