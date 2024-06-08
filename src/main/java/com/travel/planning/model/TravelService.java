package com.travel.planning.model;

import jakarta.persistence.*;

@Entity
@Table(name = "travel_services")
public class TravelService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
