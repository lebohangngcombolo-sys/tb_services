package com.tradebridge.customer.repository;

import com.tradebridge.customer.domain.OrganizationAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrganizationAddressRepository extends JpaRepository<OrganizationAddress, UUID> {

    List<OrganizationAddress> findByOrganizationId(UUID organizationId);

    void deleteByOrganizationId(UUID organizationId);
}