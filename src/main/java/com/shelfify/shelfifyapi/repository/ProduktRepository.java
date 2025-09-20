package com.shelfify.shelfifyapi.repository;


import com.shelfify.shelfifyapi.model.Products;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;


public interface ProduktRepository extends JpaRepository<Products, Long> {

    List<Products> findAll(Sort sort);

    List<Products> findByAblaufdatumBefore(LocalDate cutoffDate);

    List<Products> findByAblaufdatumBetween(LocalDate startDate, LocalDate endDate);

}
