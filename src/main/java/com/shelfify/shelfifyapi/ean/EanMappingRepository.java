package com.shelfify.shelfifyapi.ean;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

@Repository
public interface EanMappingRepository extends JpaRepository<EanMapping, Long> {
    Optional<EanMapping> findByEanAndDatagroupIsNull(String ean);

    Optional<EanMapping> findByEanAndDatagroup(String ean, String datagroup);

    Optional<EanMapping> findByProductNameAndDatagroup(String name, String datagroup);

    Optional<EanMapping> findByProductNameAndDatagroupIsNull(String name);
}

