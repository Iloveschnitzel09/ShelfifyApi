package com.lager.lagerappapi.repository;


import com.lager.lagerappapi.model.Produkte;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;


public interface ProduktRepository extends JpaRepository<Produkte, Long> {

    List<Produkte> findAll(Sort sort);

    List<Produkte> findByAblaufdatumBefore(LocalDate cutoffDate);

    List<Produkte> findByAblaufdatumBetween(LocalDate startDate, LocalDate endDate);

}
