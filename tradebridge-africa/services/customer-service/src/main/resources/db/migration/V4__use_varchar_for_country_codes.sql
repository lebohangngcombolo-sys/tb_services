-- Align country-code columns with the VARCHAR mappings used by JPA entities.
ALTER TABLE customer_profiles
    ALTER COLUMN country_code TYPE VARCHAR(2);

ALTER TABLE organizations
    ALTER COLUMN country_code TYPE VARCHAR(2);

ALTER TABLE organization_addresses
    ALTER COLUMN country_code TYPE VARCHAR(2);

ALTER TABLE beneficial_owners
    ALTER COLUMN nationality_country_code TYPE VARCHAR(2),
    ALTER COLUMN residence_country_code TYPE VARCHAR(2);

ALTER TABLE kyc_cases
    ALTER COLUMN country_code TYPE VARCHAR(2);
