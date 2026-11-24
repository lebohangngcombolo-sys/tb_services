-- Align fixed-width code columns with the VARCHAR mappings used by JPA entities.
ALTER TABLE countries
    ALTER COLUMN iso2_code TYPE VARCHAR(2),
    ALTER COLUMN iso3_code TYPE VARCHAR(3),
    ALTER COLUMN default_currency_code TYPE VARCHAR(3);

ALTER TABLE currencies
    ALTER COLUMN code TYPE VARCHAR(3);

ALTER TABLE country_currencies
    ALTER COLUMN country_code TYPE VARCHAR(2),
    ALTER COLUMN currency_code TYPE VARCHAR(3);

ALTER TABLE country_languages
    ALTER COLUMN country_code TYPE VARCHAR(2);

ALTER TABLE country_configurations
    ALTER COLUMN country_code TYPE VARCHAR(2);

ALTER TABLE organization_requirements
    ALTER COLUMN country_code TYPE VARCHAR(2);

ALTER TABLE kyc_requirements
    ALTER COLUMN country_code TYPE VARCHAR(2);

ALTER TABLE configuration_versions
    ALTER COLUMN country_code TYPE VARCHAR(2);
