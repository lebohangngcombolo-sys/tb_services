package com.tradebridge.security.jwt;

import com.tradebridge.security.config.SecurityProperties;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Validates the standard JWT claims required by spec §2.5 and §3.4:
 * {@code iss}, {@code aud}, {@code exp}, {@code nbf}, {@code iat},
 * plus a mandatory {@code kid} on the header.
 *
 * <p>Not an {@code OAuth2TokenValidator} because we don't want Spring
 * Security's default issuer validation kicking in — we validate explicitly
 * to keep behaviour predictable across services.
 */
public class JwtClaimValidator {

    private final SecurityProperties.Jwt props;

    public JwtClaimValidator(SecurityProperties.Jwt props) {
        this.props = Objects.requireNonNull(props, "props");
    }

    public void validate(Jwt jwt) {
        List<OAuth2Error> errors = new ArrayList<>();
        Instant now = Instant.now();
        long skewSeconds = props.getClockSkew() == null ? 0 : props.getClockSkew().getSeconds();

        // issuer
        String iss = jwt.getClaimAsString(JwtClaimNames.ISS);
        if (props.getIssuer() != null && !props.getIssuer().equals(iss)) {
            errors.add(new OAuth2Error("invalid_issuer",
                    "JWT issuer mismatch: expected " + props.getIssuer() + ", got " + iss, null));
        }

        // audience
        if (props.getAudience() != null) {
            List<String> audiences = jwt.getAudience();
            if (audiences == null || !audiences.contains(props.getAudience())) {
                errors.add(new OAuth2Error("invalid_audience",
                        "JWT audience does not contain " + props.getAudience(), null));
            }
        }

        // exp
        Instant exp = jwt.getExpiresAt();
        if (exp == null) {
            errors.add(new OAuth2Error("missing_exp", "JWT missing 'exp' claim", null));
        } else if (exp.plusSeconds(skewSeconds).isBefore(now)) {
            errors.add(new OAuth2Error("token_expired", "JWT expired at " + exp, null));
        }

        // nbf
        Instant nbf = jwt.getNotBefore();
        if (nbf != null && nbf.minusSeconds(skewSeconds).isAfter(now)) {
            errors.add(new OAuth2Error("token_not_yet_valid", "JWT not valid before " + nbf, null));
        }

        // iat
        Instant iat = jwt.getIssuedAt();
        if (iat == null) {
            errors.add(new OAuth2Error("missing_iat", "JWT missing 'iat' claim", null));
        } else if (iat.minusSeconds(skewSeconds).isAfter(now)) {
            errors.add(new OAuth2Error("token_issued_in_future", "JWT 'iat' is in the future", null));
        }

        // kid on header
        Object kid = jwt.getHeaders().get("kid");
        if (kid == null || kid.toString().isBlank()) {
            errors.add(new OAuth2Error("missing_kid", "JWT header missing 'kid'", null));
        }

        if (!errors.isEmpty()) {
            throw new JwtValidationException("JWT validation failed", errors);
        }
    }
}
