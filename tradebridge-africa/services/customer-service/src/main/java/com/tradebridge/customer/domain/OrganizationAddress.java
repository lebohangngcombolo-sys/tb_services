package com.tradebridge.customer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Organization address (spec §6.2 {@code organization_addresses}).
 */
@Entity
@Table(name = "organization_addresses")
public class OrganizationAddress {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "address_type", nullable = false, length = 30)
    private String addressType;

    @Column(name = "address_line_1", nullable = false, length = 255)
    private String addressLine1;

    @Column(name = "address_line_2", length = 255)
    private String addressLine2;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "postal_code", length = 30)
    private String postalCode;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OrganizationAddress() { }

    public OrganizationAddress(UUID id, UUID organizationId, String addressType,
                               String addressLine1, String addressLine2, String city,
                               String region, String postalCode, String countryCode,
                               boolean isPrimary) {
        this.id = Objects.requireNonNull(id, "id");
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId");
        this.addressType = Objects.requireNonNull(addressType, "addressType");
        this.addressLine1 = Objects.requireNonNull(addressLine1, "addressLine1");
        this.addressLine2 = addressLine2;
        this.city = Objects.requireNonNull(city, "city");
        this.region = region;
        this.postalCode = postalCode;
        this.countryCode = Objects.requireNonNull(countryCode, "countryCode").toUpperCase();
        this.isPrimary = isPrimary;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public String getAddressType() { return addressType; }
    public String getAddressLine1() { return addressLine1; }
    public String getAddressLine2() { return addressLine2; }
    public String getCity() { return city; }
    public String getRegion() { return region; }
    public String getPostalCode() { return postalCode; }
    public String getCountryCode() { return countryCode; }
    public boolean isPrimary() { return isPrimary; }
    public Instant getCreatedAt() { return createdAt; }
}