package com.shelfify.shelfifyapi.ean;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EanMappingRepository extends JpaRepository<EanMapping, Long> {
    Optional<EanMapping> findByEan(String ean);
    // AndDatagroup(String ean, String datagroup);
}

