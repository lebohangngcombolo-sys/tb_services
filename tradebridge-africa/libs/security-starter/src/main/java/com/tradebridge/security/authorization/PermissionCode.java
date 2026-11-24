package com.tradebridge.security.authorization;

/**
 * Central permission/role code catalogue referenced by
 * {@code @RequirePermission} and {@code @RequireRole} across services.
 *
 * <p>Keep this list aligned with the role table seeded in
 * {@code tb_identity_db.V2__seed_roles_permissions.sql} (§3.2). New codes
 * must be added both here and in the seed migration.
 */
public final class PermissionCode {

    private PermissionCode() { }

    // --------------------------------- Roles (coarse)
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_KYC_ANALYST = "KYC_ANALYST";
    public static final String ROLE_KYC_SUPERVISOR = "KYC_SUPERVISOR";
    public static final String ROLE_COMPLIANCE_OFFICER = "COMPLIANCE_OFFICER";
    public static final String ROLE_SUPPORT = "SUPPORT";
    public static final String ROLE_SERVICE = "SERVICE";

    // --------------------------------- Identity permissions
    public static final String IDENTITY_USER_READ = "identity.user.read";
    public static final String IDENTITY_USER_ADMIN = "identity.user.admin";
    public static final String IDENTITY_ROLE_READ = "identity.role.read";
    public static final String IDENTITY_ROLE_WRITE = "identity.role.write";
    public static final String IDENTITY_PERMISSION_READ = "identity.permission.read";

    // --------------------------------- Country permissions
    public static final String COUNTRY_READ = "country.read";
    public static final String COUNTRY_ADMIN = "country.admin";
    public static final String COUNTRY_CONFIG_PUBLISH = "country.config.publish";
    public static final String COUNTRY_CONFIG_ROLLBACK = "country.config.rollback";

    // --------------------------------- Customer permissions
    public static final String CUSTOMER_READ = "customer.read";
    public static final String CUSTOMER_ADMIN = "customer.admin";
    public static final String ORGANIZATION_READ = "organization.read";
    public static final String ORGANIZATION_MANAGE = "organization.manage";
    public static final String ORGANIZATION_SUBMIT = "organization.submit";

    // --------------------------------- KYC permissions
    public static final String KYC_CASE_READ = "kyc.case.read";
    public static final String KYC_CASE_REVIEW = "kyc.case.review";
    public static final String KYC_CASE_APPROVE = "kyc.case.approve";
    public static final String KYC_CASE_REJECT = "kyc.case.reject";
    public static final String KYC_DECISION_VIEW = "kyc.decision.view";

    // --------------------------------- Notification permissions
    public static final String NOTIFICATION_SUBMIT = "notification.submit";
    public static final String NOTIFICATION_READ = "notification.read";
    public static final String NOTIFICATION_TEMPLATE_ADMIN = "notification.template.admin";
    public static final String NOTIFICATION_DELIVERY_ADMIN = "notification.delivery.admin";

    // --------------------------------- Service identities
    public static final String SERVICE_IDENTITY = "service.identity";
    public static final String SERVICE_NOTIFICATION = "service.notification";
    public static final String SERVICE_COUNTRY = "service.country";
    public static final String SERVICE_CUSTOMER = "service.customer";
}
