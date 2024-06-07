package com.travel.planning.repository;

import com.travel.planning.model.Travel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TravelRepository extends JpaRepository<Travel,Long> {

}
