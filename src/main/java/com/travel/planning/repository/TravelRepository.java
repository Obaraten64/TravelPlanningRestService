package com.travel.planning.repository;

import com.travel.planning.model.Cities;
import com.travel.planning.model.Travel;
import com.travel.planning.model.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TravelRepository extends JpaRepository<Travel,Long> {
    Optional<Travel> findTravelByUser(User user);
    List<Travel> findAllByDeparture(Cities departure);
    List<Travel> findAllByDestination(Cities destination);
}
