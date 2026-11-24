package com.tradebridge.notification.service;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Classifies templates that must bypass user preferences (spec §5.4:
 * "Security-critical messages ... must not be disabled by ordinary
 * marketing or notification preferences").
 *
 * <p>The set is intentionally narrow — the OTP, account activation,
 * security alerts, and KYC decisions. Marketing and reminders are never
 * mandatory.
 */
@Component
public class MandatoryNotificationClassifier {

    /** Template codes that are always sent, regardless of user preference. */
    private static final Set<String> MANDATORY_TEMPLATE_CODES = Set.of(
            "identity.verification",
            "identity.password.reset",
            "identity.mfa.otp",
            "identity.account.activated",
            "identity.password.changed",
            "customer.organization.submitted",
            "customer.kyc.approved",
            "customer.kyc.rejected"
    );

    public boolean isMandatory(String templateCode) {
        return templateCode != null && MANDATORY_TEMPLATE_CODES.contains(templateCode);
    }
}