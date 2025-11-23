package com.shelfify.shelfifyapi.repository;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import com.shelfify.shelfifyapi.model.Products;


public interface ProduktRepository extends JpaRepository<Products, Long> {

    List<Products> findByDatagroup(String datagroup, Sort sort);

    List<Products> findByAblaufdatumBeforeAndDatagroup(LocalDate cutoffDate, String datagroup, Sort sort);

    List<Products> findByAblaufdatumBetweenAndDatagroup(LocalDate startDate, LocalDate endDate, String datagroup);

    List<Products> findByEanAndDatagroupOrderByAblaufdatumAsc(String ean, String datagroup);

    Optional<Products> findByEanAndAblaufdatumAndDatagroup(String ean, LocalDate ablaufdatum, String datagroup);

    void deleteByDatagroup(String datagroup);
}
