-- TradeBridge Africa — Notification Service seed templates
-- Generated from templates/*.yaml. Only English is seeded; additional
-- languages can be added through the admin API.

INSERT INTO notification_templates (
    id, template_code, channel, language_code, subject_template, body_template,
    variables_schema, version, enabled
) VALUES

-- identity.verification — email verification OTP
(gen_random_uuid(), 'identity.verification', 'EMAIL', 'en',
 'Verify your TradeBridge Africa account',
 '<p>Hi,</p><p>Your verification code is:</p>'
  || '<p><b style="font-size:24px;letter-spacing:4px;">${otpCode}</b></p>'
  || '<p>This code expires in ${expiryMinutes} minutes.</p>'
  || '<p>If you did not request this code, you can ignore this email.</p>'
  || '<p>— TradeBridge Africa</p>',
 '{"otpCode":"string","expiryMinutes":"string"}'::jsonb,
 1, TRUE),

-- identity.password.reset — password reset OTP
(gen_random_uuid(), 'identity.password.reset', 'EMAIL', 'en',
 'Reset your TradeBridge Africa password',
 '<p>Hi,</p><p>Your password reset code is:</p>'
  || '<p><b style="font-size:24px;letter-spacing:4px;">${otpCode}</b></p>'
  || '<p>This code expires in ${expiryMinutes} minutes.</p>'
  || '<p>If you did not request a reset, please secure your account immediately.</p>'
  || '<p>— TradeBridge Africa</p>',
 '{"otpCode":"string","expiryMinutes":"string"}'::jsonb,
 1, TRUE),

-- identity.mfa.otp — login MFA email OTP
(gen_random_uuid(), 'identity.mfa.otp', 'EMAIL', 'en',
 'Your TradeBridge Africa login code',
 '<p>Your TradeBridge Africa login verification code is:</p>'
  || '<p><b style="font-size:24px;letter-spacing:4px;">${otpCode}</b></p>'
  || '<p>This code expires in ${expiryMinutes} minutes. Never share it with anyone.</p>',
 '{"otpCode":"string","expiryMinutes":"string"}'::jsonb,
 1, TRUE),

-- identity.account.activated — welcome/activation email
(gen_random_uuid(), 'identity.account.activated', 'EMAIL', 'en',
 'Welcome to TradeBridge Africa',
 '<p>Hi,</p><p>${message}</p>'
  || '<p>You can now sign in and continue your onboarding.</p>'
  || '<p>— TradeBridge Africa</p>',
 '{"message":"string"}'::jsonb,
 1, TRUE),

-- identity.password.changed — security alert
(gen_random_uuid(), 'identity.password.changed', 'EMAIL', 'en',
 'Your TradeBridge Africa password was changed',
 '<p>Hi,</p><p>${message}</p>'
  || '<p>If you did not make this change, please reset your password immediately '
  || 'and contact support.</p>'
  || '<p>— TradeBridge Africa</p>',
 '{"message":"string"}'::jsonb,
 1, TRUE),

-- customer.kyc.approved — KYC approval
(gen_random_uuid(), 'customer.kyc.approved', 'EMAIL', 'en',
 'Your KYC application has been approved',
 '<p>Hi,</p><p>${message}</p>'
  || '<p>Reference: case ${kycCaseId}, decision ${decisionId}.</p>'
  || '<p>— TradeBridge Africa Compliance</p>',
 '{"message":"string","kycCaseId":"string","decisionId":"string"}'::jsonb,
 1, TRUE),

-- customer.kyc.rejected — KYC rejection
(gen_random_uuid(), 'customer.kyc.rejected', 'EMAIL', 'en',
 'Update on your KYC application',
 '<p>Hi,</p><p>${message}</p>'
  || '<p>Reference: case ${kycCaseId}, decision ${decisionId}.</p>'
  || '<p>— TradeBridge Africa Compliance</p>',
 '{"message":"string","kycCaseId":"string","decisionId":"string"}'::jsonb,
 1, TRUE);