-- TradeBridge Africa — Customer Service additional indexes & constraints.

-- Partial index for active KYC cases per organization (used by
-- `findFirstByOrganizationIdAndStatusInOrderByCreatedAtDesc`).
CREATE INDEX idx_kyc_cases_active_per_org
    ON kyc_cases (organization_id, created_at DESC)
    WHERE status IN ('SUBMITTED', 'IN_REVIEW', 'INFORMATION_REQUESTED', 'RESUBMITTED');

-- Case number generation — allow NULL but enforce uniqueness only on
-- non-null values (already enforced by the UNIQUE constraint).
-- Speed up lookup by case number for analyst search.
CREATE INDEX idx_kyc_cases_case_number_lookup ON kyc_cases (case_number);

-- Beneficial owner sum computation is done in Java but this partial
-- index speeds up the count(*) call.
CREATE INDEX idx_beneficial_owners_percentage
    ON beneficial_owners (organization_id)
    WHERE control_type IN ('DIRECT', 'INDIRECT') AND ownership_percentage IS NOT NULL;

-- Document retention lookups (spec §6.4 "Expired evidence must be flagged").
CREATE INDEX idx_kyc_documents_expiry
    ON kyc_documents (expires_at)
    WHERE expires_at IS NOT NULL AND document_status = 'ACCEPTED';