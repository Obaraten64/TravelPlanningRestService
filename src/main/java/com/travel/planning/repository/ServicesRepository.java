package com.travel.planning.repository;

import com.travel.planning.model.Cities;
import com.travel.planning.model.Services;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServicesRepository extends JpaRepository<Services,Long> {
    List<Services> findAllByCity(Cities city);
    Optional<Services> findByName(String name);
    Optional<Services> findByNameAndCity(String name, Cities city);
}