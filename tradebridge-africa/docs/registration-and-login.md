# Registration and Login: End-to-End Guide

This guide documents how a person registers and authenticates with TradeBridge Africa, including the services involved, the API calls, tokens and sessions, and the current implementation limitations.

> **Implementation status:** The password-based registration and login endpoints are implemented. The current notification OTP path deliberately does not send verification or password-reset messages because it does not have a safe way to obtain the recipient's unmasked address. MFA setup is implemented, but completing an MFA-enabled login is not currently wired end to end. These gaps are identified below so they are not mistaken for client or SMTP configuration errors.

## 1. Services involved

| Component | Local address | Responsibility in this flow |
|---|---|---|
| API Gateway | `http://localhost:8080` | Public entry point; routes API calls, applies authentication and rate limits, and forwards correlation IDs. |
| Identity Service | `http://localhost:8081` | Validates registration, stores identity and password credentials, verifies email challenges, authenticates users, issues tokens, and manages sessions. |
| Country Service | `http://localhost:8082` | Confirms that the submitted country exists and is enabled for onboarding. |
| Notification Service | `http://localhost:8083` | Consumes identity notification events and delivers messages through configured providers, including verification and reset codes. |
| Customer Service | `http://localhost:8084` | Creates a pending customer profile association after it consumes the registration event and activates that profile after it consumes the activation event. |
| PostgreSQL | configured by each service | Stores identity, credentials, verification challenges, sessions, refresh-token hashes, and customer data in service-owned databases. |
| Kafka | configured by each service | Carries identity events, including notification requests and registration/activation events. |

Use the Gateway as the API base URL in local examples:

```text
http://localhost:8080
```

For applications launched directly from the IDE, Identity Service now calls Country Service at `http://localhost:8082` by default, and Kafka clients use `localhost:9092`. Start the Country Service and make sure the Kafka broker is running and healthy before registering users. If Identity Service runs inside Docker instead, set `COUNTRY_SERVICE_URL=http://country-service:8082` and configure Kafka to use the Docker-network listener `kafka:29092`; `localhost` inside a container refers to that container itself.

The identity endpoints use the `/api/v1/identity` prefix. Calls to protected endpoints must include an access token:

```http
Authorization: Bearer <access-token>
```

## 2. High-level lifecycle

```text
Client
  |
  | POST /api/v1/identity/register
  v
API Gateway --------------------------> Identity Service
                                           |
                                           | Save pending user + password hash,
                                           | default CUSTOMER role, challenge hash
                                           |
                                           +--> identity.user.registered.v1
                                           |       `--> Customer Service creates
                                           |            a pending customer profile
                                           |
                                           +--> identity.user.email.verification.requested.v1
                                                   `--> Notification Service tries
                                                        to deliver verification OTP

Client submits email + six-digit code
  |
  | POST /api/v1/identity/verify-email
  v
Identity Service: consume challenge, mark email verified, set ACTIVE
  |
  +--> identity.user.activated.v1
          `--> Customer Service activates the pending profile

Client submits identifier + password
  |
  | POST /api/v1/identity/login
  v
Identity Service: check status, password, lockout; create session
  |
  +--> Return short-lived access JWT + opaque refresh token
```

Registration and activation events are published using the identity service's transactional outbox. Kafka consumers process them independently, so customer-profile creation and activation are asynchronous and can happen shortly after the identity operation succeeds.

Identity and Notification Services authenticate the internal challenge-code callback with the same `X-Internal-Secret` value. They share a local-development fallback; deployments should set `INTERNAL_SERVICE_SECRET` to the same strong, rotated secret in both services. The legacy `IDENTITY_INTERNAL_SECRET` and `NOTIFICATION_INTERNAL_SECRET` variables are also accepted when the shared variable is unset.

## 3. Register an account

### Request

```http
POST /api/v1/identity/register
Content-Type: application/json
```

```json
{
  "firstName": "Ada",
  "lastName": "Example",
  "email": "ada@example.com",
  "phoneNumber": "+27123456789",
  "password": "ReplaceThis!Pass123",
  "countryCode": "ZA",
  "preferredLanguage": "EN",
  "termsAccepted": "true"
}
```

Fields and checks:

| Field | Expected value |
|---|---|
| `firstName`, `lastName` | Non-blank; at most 100 characters. |
| `email` | Valid email address; normalized before storage. |
| `phoneNumber` | Valid E.164 number, for example `+27123456789`; normalized before storage. |
| `password` | 12–128 characters; must include uppercase, lowercase, a digit, and a symbol. It must not contain configured forbidden substrings such as `password`, `tradebridge`, or `123456`. |
| `countryCode` | Enabled ISO alpha-2 country. Identity Service checks Country Service; if that service is unavailable, registration fails closed. |
| `preferredLanguage` | Valid language code; the request uses the configured code format, for example `EN`. |
| `termsAccepted` | The request contract expects the string `"true"`. Current validation rejects a non-null value other than `"true"`, but the field is not marked required, so an omitted/null value may pass request validation. |

### What Identity Service does

On a valid request, Identity Service:

1. Normalizes the email, phone number, country code, and language.
2. Rejects duplicate email addresses or phone numbers.
3. Checks that the country is enabled.
4. Creates a user in `PENDING_VERIFICATION` status.
5. Hashes the password using the configured Spring Security password encoder; the raw password is not stored.
6. Assigns the default `ROLE_CUSTOMER` role when that role exists.
7. Creates an email-verification challenge. The stored challenge contains a SHA-256 hash of a randomly generated six-digit code, not the raw code.
8. Publishes a registration event and a separate email-verification-request event.

The successful response uses HTTP `201 Created`:

```json
{
  "success": true,
  "message": "Registration accepted",
  "data": {
    "userId": "00000000-0000-7000-8000-000000000000",
    "email": "ada@example.com",
    "status": "PENDING_VERIFICATION",
    "emailVerificationRequired": true
  },
  "timestamp": "2026-01-01T12:00:00Z",
  "correlationId": "..."
}
```

The UUID and timestamp above are illustrative. The registration response does **not** contain the OTP or any token. A pending account cannot log in.

## 4. Verify the email

### Request

```http
POST /api/v1/identity/verify-email
Content-Type: application/json
```

```json
{
  "email": "ada@example.com",
  "code": "123456"
}
```

The code must be six digits. The current defaults are a ten-minute challenge lifetime, a maximum of five verification attempts, and a 60-second resend cooldown.

If the code matches an active challenge, Identity Service consumes the challenge, marks the email verified, changes the user to `ACTIVE`, and publishes `identity.user.activated.v1`. Customer Service consumes that event and activates the pending customer profile if it has already been created.

Successful verification returns a standard success response:

```json
{
  "success": true,
  "message": "Email verified",
  "data": null,
  "timestamp": "2026-01-01T12:00:00Z",
  "correlationId": "..."
}
```

### Resend a code

```http
POST /api/v1/identity/resend-verification
Content-Type: application/json
```

```json
{
  "email": "ada@example.com"
}
```

The endpoint returns a generic success message whether or not an account exists. This avoids revealing account existence. Requests inside the resend cooldown do not issue another challenge.

### Check verification status

After authenticating, a user can query:

```http
GET /api/v1/identity/verification-status
Authorization: Bearer <access-token>
```

The response includes `userId`, `emailVerified`, `phoneVerified`, `status`, and `mfaEnabled`.

## 5. Log in

Login accepts an email address or E.164 phone number as `identifier`.

```http
POST /api/v1/identity/login
Content-Type: application/json
```

```json
{
  "identifier": "ada@example.com",
  "password": "ReplaceThis!Pass123",
  "deviceName": "Ada's laptop",
  "userAgent": "TradeBridge Web"
}
```

`deviceName` and `userAgent` are optional session metadata.

Identity Service:

1. Finds the user by normalized email or phone number.
2. Rejects suspended or non-active users. A user must complete email verification first.
3. Checks the stored password hash.
4. Applies database-backed failed-login lockout. Defaults are five failed attempts and a 15-minute lock duration.
5. Resets the failed-attempt count after a successful password check.
6. Creates a user session and issues tokens, unless the user has MFA enabled.

The Gateway separately rate-limits login requests (default: ten per minute).

For a user without MFA, the response data is shaped like this:

```json
{
  "success": true,
  "message": "Authentication successful",
  "data": {
    "accessToken": "<signed-jwt>",
    "tokenType": "Bearer",
    "expiresInSeconds": 900,
    "refreshToken": "<opaque-refresh-token>",
    "refreshExpiresInSeconds": 2592000,
    "mfaRequired": false,
    "mfaChallengeId": null,
    "user": {
      "id": "<user-uuid>",
      "email": "ada@example.com",
      "firstName": "Ada",
      "lastName": "Example",
      "roles": ["ROLE_CUSTOMER"]
    }
  },
  "timestamp": "...",
  "correlationId": "..."
}
```

Token lifetimes shown are the current defaults (15 minutes for access and 30 days for refresh); deployments can configure these values. Treat both tokens as secrets. The access JWT contains the user ID as `sub`, session ID as `sid`, email, role codes, and standard issue/expiry claims. It is signed by Identity Service. Clients can obtain the public signing key set at `GET /.well-known/jwks.json`.

Use the access token on protected API calls:

```http
GET /api/v1/identity/me
Authorization: Bearer <access-token>
```

## 6. Refresh tokens and sessions

When the access token expires, exchange the refresh token:

```http
POST /api/v1/identity/refresh
Content-Type: application/json
```

```json
{
  "refreshToken": "<opaque-refresh-token>"
}
```

Refresh tokens are random opaque values. Identity Service stores only their SHA-256 hashes and rotates the token on each successful refresh. Replace the old refresh token with the new one returned in the response. Reuse of an already-revoked token triggers reuse detection and revokes all of the user's sessions and refresh tokens.

By default, a user may have up to ten active sessions; a new session beyond that limit revokes the oldest. Each session has a 30-day absolute expiry by default.

Authenticated session endpoints:

| Method and path | Behavior |
|---|---|
| `GET /api/v1/identity/sessions` | List the current user's active sessions. |
| `DELETE /api/v1/identity/sessions/{sessionId}` | Revoke one of the current user's sessions. |
| `POST /api/v1/identity/logout` | Revoke the current session. |
| `POST /api/v1/identity/logout-all` | Revoke all sessions and refresh tokens for the current user. |

Logout requires a valid access token and stops future refreshes for the revoked session. Clients should also discard their local access and refresh tokens.

## 7. Optional MFA

An authenticated user can set up TOTP:

1. `POST /api/v1/identity/mfa/setup/totp` with the current password.
2. Add the returned `provisioningUri` to an authenticator app.
3. `POST /api/v1/identity/mfa/verify-setup` with a current six-digit TOTP code.
4. Store the recovery codes returned by setup verification securely; they are shown once.

Example setup request:

```http
POST /api/v1/identity/mfa/setup/totp
Authorization: Bearer <access-token>
Content-Type: application/json
```

```json
{
  "currentPassword": "ReplaceThis!Pass123"
}
```

Other authenticated MFA endpoints:

| Method and path | Behavior |
|---|---|
| `GET /api/v1/identity/mfa/status` | Read whether TOTP MFA is enabled and how many recovery codes remain. |
| `POST /api/v1/identity/mfa/disable` | Disable MFA with a valid TOTP or recovery code. |
| `POST /api/v1/identity/mfa/recovery-codes/regenerate` | Replace recovery codes after TOTP verification. |
| `POST /api/v1/identity/mfa/challenge` | Request an email OTP challenge for an already authenticated user. |
| `POST /api/v1/identity/mfa/verify` | Complete an MFA-enabled login with its challenge ID and a TOTP or recovery code. This route is public but rate-limited; the challenge is single-use and expires after five minutes. |
| `POST /api/v1/identity/mfa/challenge/verify` | Verify an email OTP challenge for an already authenticated user. |

### Complete an MFA-enabled login

When MFA is enabled, password login returns `mfaRequired: true`, a `mfaChallengeId`, and no access or refresh tokens. Complete the login by posting the challenge ID and either a six-digit TOTP code or an unused recovery code:

```http
POST /api/v1/identity/mfa/verify
Content-Type: application/json
```

```json
{
  "challengeId": "01a2139c-feff-7ed9-b8b2-614ef5f4d08f",
  "code": "123456"
}
```

On success, the response contains the access and refresh tokens. The five-minute challenge is bound to the temporary session created during password login; successful verification revokes that session and issues tokens for a new session. Failed or expired challenges do not issue tokens. The endpoint is public only for this explicit POST route and remains covered by the Gateway's OTP rate limit. Do not use the challenge ID as a bearer token.

## 8. Password recovery and change

### Request reset

```http
POST /api/v1/identity/password/forgot
Content-Type: application/json
```

```json
{
  "email": "ada@example.com"
}
```

The response is deliberately generic: “If the account exists, a reset email has been sent.”

### Complete reset

After receiving a reset token, submit:

```http
POST /api/v1/identity/password/reset
Content-Type: application/json
```

```json
{
  "token": "<reset-token>",
  "newPassword": "A different replacement!Pass123"
}
```

Reset tokens expire after one hour by default. A successful reset invalidates all existing sessions and refresh tokens and applies the same password policy as registration.

An authenticated user may change their password using:

```http
POST /api/v1/identity/password/change
Authorization: Bearer <access-token>
Content-Type: application/json
```

```json
{
  "currentPassword": "ReplaceThis!Pass123",
  "newPassword": "A different replacement!Pass123"
}
```

## 9. Errors and operational behavior

Successful API responses use the common envelope:

```json
{
  "success": true,
  "message": "...",
  "data": {},
  "timestamp": "...",
  "correlationId": "..."
}
```

Errors use a corresponding error envelope with `success: false`, a machine-readable `code`, a `message`, optional field-level `errors`, a timestamp, and a correlation ID. Examples of expected failures include:

- `VALIDATION_ERROR`: invalid request data or password policy failure.
- `DUPLICATE_RESOURCE`: email address or phone number is already registered.
- `INVALID_CREDENTIALS`: unknown identifier or incorrect password.
- `INVALID_STATE`: account has not been activated, or a required challenge does not exist.
- `ACCOUNT_LOCKED`: failed-login lockout is active.
- `ACCOUNT_SUSPENDED`: the account is suspended.
- `TOKEN_INVALID` / `TOKEN_EXPIRED`: invalid, revoked, reused, or expired token.

The Gateway's public-route allowlist includes registration, email verification, resend, login, refresh, MFA login completion, forgot-password, and reset-password. Other identity routes require a valid access JWT. Identity Service also enforces authentication on protected routes. The Gateway has separate rate limits for registration, OTP-related requests, login, and password reset.

For support and troubleshooting, keep the response `correlationId` and use it to trace logs across the Gateway and services. Never log or share raw passwords, access tokens, refresh tokens, recovery codes, or OTPs.

## 10. Current limitations

These are implementation constraints in the current source, not expected client configuration:

1. **Notification delivery depends on provider configuration.** OTPs are sent through Notification Service using a single-use internal handoff for both the code and destination; the raw values are not placed in Kafka. Configure a working SMTP/SMS provider and matching notification templates for delivery to succeed. For local Gmail SMTP, set `SMTP_USER` and a newly generated Google App Password in `SMTP_PASSWORD`; credentials have no source-controlled defaults.
2. **JWT issuer configuration must be consistent.** Access tokens use the issuer and audience configured under `tradebridge.security.jwt`; the Gateway validates against its corresponding `tradebridge.gateway.jwt` settings. Keep `JWT_ISSUER` and `JWT_AUDIENCE` aligned across Identity Service and the Gateway. Existing tokens issued with an earlier issuer value will be rejected and users must log in again after configuration changes.
3. **Terms acceptance is not fully enforced.** The registration DTO checks that a supplied non-null value is `"true"`, but does not require the field to be present, and the registration service does not check it separately.

Consequently, production setup requires configured notification providers and a consistent JWT issuer.

## 11. Source code map

| Concern | Source |
|---|---|
| Registration and email verification endpoints | [RegistrationController.java](../services/identity-service/src/main/java/com/tradebridge/identity/api/RegistrationController.java) |
| Login, refresh, logout, current user | [AuthController.java](../services/identity-service/src/main/java/com/tradebridge/identity/api/AuthController.java) |
| Registration validation and persistence | [RegistrationService.java](../services/identity-service/src/main/java/com/tradebridge/identity/service/RegistrationService.java) |
| Email challenge verification | [EmailVerificationService.java](../services/identity-service/src/main/java/com/tradebridge/identity/service/EmailVerificationService.java) |
| Password authentication and lockout | [AuthenticationService.java](../services/identity-service/src/main/java/com/tradebridge/identity/service/AuthenticationService.java) |
| JWT and refresh-token issuance | [TokenService.java](../services/identity-service/src/main/java/com/tradebridge/identity/service/TokenService.java) |
| Session lifecycle | [SessionService.java](../services/identity-service/src/main/java/com/tradebridge/identity/service/SessionService.java) |
| MFA endpoints and behavior | [MfaController.java](../services/identity-service/src/main/java/com/tradebridge/identity/api/MfaController.java) and [MfaService.java](../services/identity-service/src/main/java/com/tradebridge/identity/service/MfaService.java) |
| Gateway routes and public endpoints | [Gateway application.yml](../services/api-gateway/src/main/resources/application.yml) and [PublicRouteRegistry.java](../services/api-gateway/src/main/java/com/tradebridge/gateway/security/PublicRouteRegistry.java) |
| Identity configuration | [Identity application.yml](../services/identity-service/src/main/resources/application.yml) |
| Notification event handling | [NotificationEventHooks.java](../services/notification-service/src/main/java/com/tradebridge/notification/service/NotificationEventHooks.java) |
| Customer profile registration/activation | [CustomerEventHooks.java](../services/customer-service/src/main/java/com/tradebridge/customer/service/CustomerEventHooks.java) |
