package com.travel.planning.repository;

import com.travel.planning.model.Services;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServicesRepository extends JpaRepository<Services,Long> {

}
