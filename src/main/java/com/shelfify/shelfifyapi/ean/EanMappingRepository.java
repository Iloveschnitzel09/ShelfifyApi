package com.shelfify.shelfifyapi.ean;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EanMappingRepository extends JpaRepository<EanMapping, Long> {
    Optional<EanMapping> findByEanAndDatagroupIsNull(String ean);
    Optional<EanMapping> findByEanAndDatagroup(String ean, String datagroup);
}

