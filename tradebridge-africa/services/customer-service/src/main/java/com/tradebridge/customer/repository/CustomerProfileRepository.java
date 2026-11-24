package com.tradebridge.customer.repository;

import com.tradebridge.customer.domain.CustomerProfile;
import com.tradebridge.customer.domain.enums.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, UUID> {

    Optional<CustomerProfile> findByIdentityUserId(UUID identityUserId);

    boolean existsByIdentityUserId(UUID identityUserId);

    Optional<CustomerProfile> findByCustomerNumber(String customerNumber);

    @Query("""
            SELECT c FROM CustomerProfile c
            WHERE (:query IS NULL OR LOWER(c.customerNumber) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:countryCode IS NULL OR c.countryCode = :countryCode)
              AND (:status IS NULL OR c.status = :status)
            """)
    Page<CustomerProfile> search(@Param("query") String query,
                                  @Param("countryCode") String countryCode,
                                  @Param("status") CustomerStatus status,
                                  Pageable pageable);
}