-- TradeBridge Africa — Country Service seed data
-- Currencies, languages, and initial countries per spec §4.2 (EN, PT).
-- Countries: ZA (South Africa), PT (Portugal) as per spec.

-- ============================================================ CURRENCIES
INSERT INTO currencies (code, name, symbol, decimal_places, enabled) VALUES
    ('ZAR', 'South African Rand',     'R',  2, TRUE),
    ('USD', 'US Dollar',              '$',  2, TRUE),
    ('EUR', 'Euro',                   '€',  2, TRUE),
    ('GBP', 'British Pound',          '£',  2, TRUE),
    ('BRL', 'Brazilian Real',         'R$', 2, TRUE),
    ('MZN', 'Mozambican Metical',     'MT', 2, TRUE),
    ('AOA', 'Angolan Kwanza',         'Kz', 2, TRUE),
    ('NGN', 'Nigerian Naira',         '₦',  2, TRUE),
    ('KES', 'Kenyan Shilling',        'KSh',2, TRUE),
    ('GHS', 'Ghanaian Cedi',          '₵',  2, TRUE);

-- ============================================================ LANGUAGES
INSERT INTO languages (code, name, native_name, enabled) VALUES
    ('en', 'English',    'English',  TRUE),
    ('pt', 'Portuguese', 'Português', TRUE),
    ('fr', 'French',     'Français',  TRUE),
    ('sw', 'Swahili',    'Kiswahili', TRUE);

-- ============================================================ COUNTRIES

-- South Africa
INSERT INTO countries (
    id, iso2_code, iso3_code, name, region, calling_code,
    default_currency_code, default_language_code, enabled
) VALUES (
    gen_random_uuid(), 'ZA', 'ZAF', 'South Africa', 'AFRICA', '+27',
    'ZAR', 'EN', TRUE
);

INSERT INTO country_currencies (id, country_code, currency_code, is_default, enabled) VALUES
    (gen_random_uuid(), 'ZA', 'ZAR', TRUE, TRUE),
    (gen_random_uuid(), 'ZA', 'USD', FALSE, TRUE);

INSERT INTO country_languages (id, country_code, language_code, is_default, enabled) VALUES
    (gen_random_uuid(), 'ZA', 'en', TRUE, TRUE);

-- Portugal
INSERT INTO countries (
    id, iso2_code, iso3_code, name, region, calling_code,
    default_currency_code, default_language_code, enabled
) VALUES (
    gen_random_uuid(), 'PT', 'PRT', 'Portugal', 'EUROPE', '+351',
    'EUR', 'PT', TRUE
);

INSERT INTO country_currencies (id, country_code, currency_code, is_default, enabled) VALUES
    (gen_random_uuid(), 'PT', 'EUR', TRUE, TRUE);

INSERT INTO country_languages (id, country_code, language_code, is_default, enabled) VALUES
    (gen_random_uuid(), 'PT', 'pt', TRUE, TRUE),
    (gen_random_uuid(), 'PT', 'en', FALSE, TRUE);

-- ============================================================ INITIAL REQUIREMENTS
-- South Africa — organization requirements (basic set)

INSERT INTO organization_requirements (
    id, country_code, organization_type, requirement_code, description,
    required, validation_rule, enabled
) VALUES
    (gen_random_uuid(), 'ZA', 'COMPANY', 'REGISTRATION_NUMBER',
     'CIPC registration number', TRUE,
     '{"pattern":"^[0-9]{4}/[0-9]{6}/[0-9]{2}$"}'::jsonb, TRUE),
    (gen_random_uuid(), 'ZA', 'COMPANY', 'TAX_NUMBER',
     'SARS income tax number', TRUE, NULL, TRUE),
    (gen_random_uuid(), 'ZA', 'COMPANY', 'VAT_NUMBER',
     'SARS VAT registration number', FALSE, NULL, TRUE),
    (gen_random_uuid(), 'ZA', 'COMPANY', 'B-BBEE_CERTIFICATE',
     'Broad-Based Black Economic Empowerment certificate', FALSE, NULL, TRUE);

-- South Africa — KYC requirements (basic set for COMPANY customers)
INSERT INTO kyc_requirements (
    id, country_code, customer_type, document_type, requirement_code, description,
    required, allowed_mime_types, max_file_size_bytes, validity_days, enabled
) VALUES
    (gen_random_uuid(), 'ZA', 'COMPANY', 'DIRECTOR_ID',
     'DIRECTOR_ID_COPY', 'Certified copy of director ID', TRUE,
     '["application/pdf","image/jpeg","image/png"]'::jsonb, 10485760, 365, TRUE),
    (gen_random_uuid(), 'ZA', 'COMPANY', 'COMPANY_REGISTRATION',
     'COMPANY_REGISTRATION_CERT', 'CIPC company registration certificate', TRUE,
     '["application/pdf"]'::jsonb, 5242880, 365, TRUE),
    (gen_random_uuid(), 'ZA', 'COMPANY', 'PROOF_OF_ADDRESS',
     'PROOF_OF_ADDRESS', 'Proof of business address (3 months)', TRUE,
     '["application/pdf","image/jpeg","image/png"]'::jsonb, 5242880, 90, TRUE),
    (gen_random_uuid(), 'ZA', 'INDIVIDUAL', 'PASSPORT',
     'PASSPORT_COPY', 'Copy of passport bio page', TRUE,
     '["application/pdf","image/jpeg","image/png"]'::jsonb, 5242880, 3650, TRUE);

-- Portugal — organization requirements
INSERT INTO organization_requirements (
    id, country_code, organization_type, requirement_code, description,
    required, validation_rule, enabled
) VALUES
    (gen_random_uuid(), 'PT', 'COMPANY', 'NIPC',
     'Número de Identificação de Pessoa Coletiva', TRUE,
     '{"pattern":"^[0-9]{9}$"}'::jsonb, TRUE),
    (gen_random_uuid(), 'PT', 'COMPANY', 'NIF',
     'Número de Identificação Fiscal', TRUE,
     '{"pattern":"^[0-9]{9}$"}'::jsonb, TRUE);

-- Portugal — KYC requirements
INSERT INTO kyc_requirements (
    id, country_code, customer_type, document_type, requirement_code, description,
    required, allowed_mime_types, max_file_size_bytes, validity_days, enabled
) VALUES
    (gen_random_uuid(), 'PT', 'COMPANY', 'DIRECTOR_ID',
     'DIRECTOR_ID_COPY', 'Certified copy of director identification', TRUE,
     '["application/pdf","image/jpeg","image/png"]'::jsonb, 10485760, 365, TRUE),
    (gen_random_uuid(), 'PT', 'COMPANY', 'COMPANY_REGISTRATION',
     'CERTIDAO_PERMANENTE', 'Certidão Permanente', TRUE,
     '["application/pdf"]'::jsonb, 5242880, 90, TRUE);

-- ============================================================ INITIAL CONFIGURATION VERSION
-- Publish a v1 for both ZA and PT so onboarding can start immediately.
DO $$
DECLARE
    za_version_id UUID;
    pt_version_id UUID;
BEGIN
    -- ZA: publish v1 with a default set of configuration keys
    za_version_id := gen_random_uuid();
    INSERT INTO configuration_versions (id, country_code, version, status, change_summary, published_at)
    VALUES (za_version_id, 'ZA', 1, 'PUBLISHED', 'Initial configuration', now());

    INSERT INTO country_configurations (id, country_code, configuration_key, configuration_value, version, status, created_at) VALUES
        (gen_random_uuid(), 'ZA', 'onboarding.requires_phone_verification',
         '{"value": true}'::jsonb, 1, 'PUBLISHED', now()),
        (gen_random_uuid(), 'ZA', 'onboarding.min_directors',
         '{"value": 1}'::jsonb, 1, 'PUBLISHED', now()),
        (gen_random_uuid(), 'ZA', 'kyc.beneficial_ownership_threshold',
         '{"value": 25.0}'::jsonb, 1, 'PUBLISHED', now()),
        (gen_random_uuid(), 'ZA', 'kyc.requires_second_reviewer_above_risk',
         '{"value": "HIGH"}'::jsonb, 1, 'PUBLISHED', now());

    -- PT: same set of configuration keys for consistency
    pt_version_id := gen_random_uuid();
    INSERT INTO configuration_versions (id, country_code, version, status, change_summary, published_at)
    VALUES (pt_version_id, 'PT', 1, 'PUBLISHED', 'Initial configuration', now());

    INSERT INTO country_configurations (id, country_code, configuration_key, configuration_value, version, status, created_at) VALUES
        (gen_random_uuid(), 'PT', 'onboarding.requires_phone_verification',
         '{"value": true}'::jsonb, 1, 'PUBLISHED', now()),
        (gen_random_uuid(), 'PT', 'onboarding.min_directors',
         '{"value": 1}'::jsonb, 1, 'PUBLISHED', now()),
        (gen_random_uuid(), 'PT', 'kyc.beneficial_ownership_threshold',
         '{"value": 25.0}'::jsonb, 1, 'PUBLISHED', now()),
        (gen_random_uuid(), 'PT', 'kyc.requires_second_reviewer_above_risk',
         '{"value": "HIGH"}'::jsonb, 1, 'PUBLISHED', now());
END $$;