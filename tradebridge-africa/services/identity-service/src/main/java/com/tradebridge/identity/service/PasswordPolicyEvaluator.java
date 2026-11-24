package com.tradebridge.identity.service;

import com.tradebridge.common.exception.ValidationException;
import com.tradebridge.common.response.FieldError;
import com.tradebridge.identity.config.IdentityProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Enforces the configured password policy (spec §3.4).
 *
 * <p>Checks:
 * <ul>
 *   <li>Length within [minLength, maxLength].</li>
 *   <li>Required character classes (uppercase, lowercase, digit, symbol).</li>
 *   <li>Forbidden substrings (case-insensitive).</li>
 * </ul>
 *
 * <p>Never logs the password itself.
 */
@Component
public class PasswordPolicyEvaluator {

    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SYMBOL = Pattern.compile("[^A-Za-z0-9]");

    private final IdentityProperties.Password policy;

    public PasswordPolicyEvaluator(IdentityProperties properties) {
        this.policy = properties.getPassword();
    }

    /**
     * Validate the raw password. Throws {@link ValidationException} with
     * field-level errors if the policy is violated.
     */
    public void evaluate(String password) {
        List<FieldError> errors = new ArrayList<>();

        if (password == null || password.isBlank()) {
            throw new ValidationException("Password must not be blank",
                    List.of(new FieldError("password", "Password is required")));
        }
        if (password.length() < policy.getMinLength()) {
            errors.add(new FieldError("password",
                    "Password must be at least " + policy.getMinLength() + " characters"));
        }
        if (password.length() > policy.getMaxLength()) {
            errors.add(new FieldError("password",
                    "Password must be at most " + policy.getMaxLength() + " characters"));
        }
        if (policy.isRequireUppercase() && !UPPER.matcher(password).find()) {
            errors.add(new FieldError("password", "Password must include an uppercase letter"));
        }
        if (policy.isRequireLowercase() && !LOWER.matcher(password).find()) {
            errors.add(new FieldError("password", "Password must include a lowercase letter"));
        }
        if (policy.isRequireDigit() && !DIGIT.matcher(password).find()) {
            errors.add(new FieldError("password", "Password must include a digit"));
        }
        if (policy.isRequireSymbol() && !SYMBOL.matcher(password).find()) {
            errors.add(new FieldError("password", "Password must include a symbol"));
        }
        String lower = password.toLowerCase(Locale.ROOT);
        for (String forbidden : policy.getForbiddenSubstrings()) {
            if (forbidden != null && !forbidden.isBlank() && lower.contains(forbidden.toLowerCase(Locale.ROOT))) {
                errors.add(new FieldError("password",
                        "Password must not contain the sequence '" + forbidden + "'"));
                break;
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationException("The submitted information is invalid", errors);
        }
    }
}
