package com.tradebridge.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Identity-service specific configuration.
 *
 * <pre>
 * tradebridge.identity:
 *   producer-service-name: identity-service
 *   password:
 *     min-length: 12
 *     require-uppercase: true
 *     require-lowercase: true
 *     require-digit: true
 *     require-symbol: true
 *     max-length: 128
 *     forbidden-substrings: [ "password", "tradebridge" ]
 *   access-token:
 *     ttl: 15m
 *   refresh-token:
 *     ttl: 30d
 *     reuse-window: 30s
 *   session:
 *     max-active-per-user: 10
 *     absolute-ttl: 30d
 *   verification:
 *     otp-ttl: 10m
 *     max-attempts: 5
 *     resend-cooldown: 60s
 *   mfa:
 *     totp-issuer: "TradeBridge Africa"
 *     totp-period: 30s
 *     totp-digits: 6
 *     recovery-code-count: 10
 *   lockout:
 *     threshold: 5
 *     window: 15m
 *     lock-duration: 15m
 *   rate-limit:
 *     login-per-minute: 10
 *     password-reset-per-minute: 5
 * </pre>
 */
@ConfigurationProperties(prefix = "tradebridge.identity")
public class IdentityProperties {

    private String producerServiceName = "identity-service";
    private final Password password = new Password();
    private final AccessToken accessToken = new AccessToken();
    private final RefreshToken refreshToken = new RefreshToken();
    private final Session session = new Session();
    private final Verification verification = new Verification();
    private final Mfa mfa = new Mfa();
    private final Lockout lockout = new Lockout();
    private final RateLimit rateLimit = new RateLimit();

    public String getProducerServiceName() { return producerServiceName; }
    public void setProducerServiceName(String v) { this.producerServiceName = v; }
    public Password getPassword() { return password; }
    public AccessToken getAccessToken() { return accessToken; }
    public RefreshToken getRefreshToken() { return refreshToken; }
    public Session getSession() { return session; }
    public Verification getVerification() { return verification; }
    public Mfa getMfa() { return mfa; }
    public Lockout getLockout() { return lockout; }
    public RateLimit getRateLimit() { return rateLimit; }

    public static class Password {
        private int minLength = 12;
        private int maxLength = 128;
        private boolean requireUppercase = true;
        private boolean requireLowercase = true;
        private boolean requireDigit = true;
        private boolean requireSymbol = true;
        private List<String> forbiddenSubstrings = List.of("password", "tradebridge");

        public int getMinLength() { return minLength; }
        public void setMinLength(int v) { this.minLength = v; }
        public int getMaxLength() { return maxLength; }
        public void setMaxLength(int v) { this.maxLength = v; }
        public boolean isRequireUppercase() { return requireUppercase; }
        public void setRequireUppercase(boolean v) { this.requireUppercase = v; }
        public boolean isRequireLowercase() { return requireLowercase; }
        public void setRequireLowercase(boolean v) { this.requireLowercase = v; }
        public boolean isRequireDigit() { return requireDigit; }
        public void setRequireDigit(boolean v) { this.requireDigit = v; }
        public boolean isRequireSymbol() { return requireSymbol; }
        public void setRequireSymbol(boolean v) { this.requireSymbol = v; }
        public List<String> getForbiddenSubstrings() { return forbiddenSubstrings; }
        public void setForbiddenSubstrings(List<String> v) { this.forbiddenSubstrings = v; }
    }

    public static class AccessToken {
        private Duration ttl = Duration.ofMinutes(15);
        public Duration getTtl() { return ttl; }
        public void setTtl(Duration d) { this.ttl = d; }
    }

    public static class RefreshToken {
        private Duration ttl = Duration.ofDays(30);
        private Duration reuseWindow = Duration.ofSeconds(30);
        public Duration getTtl() { return ttl; }
        public void setTtl(Duration d) { this.ttl = d; }
        public Duration getReuseWindow() { return reuseWindow; }
        public void setReuseWindow(Duration d) { this.reuseWindow = d; }
    }

    public static class Session {
        private int maxActivePerUser = 10;
        private Duration absoluteTtl = Duration.ofDays(30);
        public int getMaxActivePerUser() { return maxActivePerUser; }
        public void setMaxActivePerUser(int v) { this.maxActivePerUser = v; }
        public Duration getAbsoluteTtl() { return absoluteTtl; }
        public void setAbsoluteTtl(Duration d) { this.absoluteTtl = d; }
    }

    public static class Verification {
        private Duration otpTtl = Duration.ofMinutes(10);
        private int maxAttempts = 5;
        private Duration resendCooldown = Duration.ofSeconds(60);
        public Duration getOtpTtl() { return otpTtl; }
        public void setOtpTtl(Duration d) { this.otpTtl = d; }
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int v) { this.maxAttempts = v; }
        public Duration getResendCooldown() { return resendCooldown; }
        public void setResendCooldown(Duration d) { this.resendCooldown = d; }
    }

    public static class Mfa {
        private String totpIssuer = "TradeBridge Africa";
        private Duration totpPeriod = Duration.ofSeconds(30);
        private int totpDigits = 6;
        private int recoveryCodeCount = 10;
        public String getTotpIssuer() { return totpIssuer; }
        public void setTotpIssuer(String v) { this.totpIssuer = v; }
        public Duration getTotpPeriod() { return totpPeriod; }
        public void setTotpPeriod(Duration d) { this.totpPeriod = d; }
        public int getTotpDigits() { return totpDigits; }
        public void setTotpDigits(int v) { this.totpDigits = v; }
        public int getRecoveryCodeCount() { return recoveryCodeCount; }
        public void setRecoveryCodeCount(int v) { this.recoveryCodeCount = v; }
    }

    public static class Lockout {
        private int threshold = 5;
        private Duration window = Duration.ofMinutes(15);
        private Duration lockDuration = Duration.ofMinutes(15);
        public int getThreshold() { return threshold; }
        public void setThreshold(int v) { this.threshold = v; }
        public Duration getWindow() { return window; }
        public void setWindow(Duration d) { this.window = d; }
        public Duration getLockDuration() { return lockDuration; }
        public void setLockDuration(Duration d) { this.lockDuration = d; }
    }

    public static class RateLimit {
        private int loginPerMinute = 10;
        private int passwordResetPerMinute = 5;
        public int getLoginPerMinute() { return loginPerMinute; }
        public void setLoginPerMinute(int v) { this.loginPerMinute = v; }
        public int getPasswordResetPerMinute() { return passwordResetPerMinute; }
        public void setPasswordResetPerMinute(int v) { this.passwordResetPerMinute = v; }
    }
}
