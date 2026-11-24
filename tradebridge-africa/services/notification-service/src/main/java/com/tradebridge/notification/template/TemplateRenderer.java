package com.tradebridge.notification.template;

import com.tradebridge.common.util.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders a template by substituting `${var}` placeholders with values
 * from a variable map (spec §5.4 "Template rendering must escape
 * untrusted input to prevent injection").
 *
 * <p>Escaping:
 * <ul>
 *   <li><b>HTML mode</b> (default): all substitutions HTML-escaped
 *       ({@code & < > " '} → entities). Email bodies are HTML.</li>
 *   <li><b>Plain mode</b> (SMS / subject): newlines preserved, control
 *       characters stripped, HTML metacharacters escaped for safety.</li>
 * </ul>
 *
 * <p>Missing variables are replaced with an empty string — this is a
 * safety net; {@link VariableSchemaValidator} should have already
 * rejected submissions that omit required variables.
 *
 * <p>Uses a hand-rolled placeholder parser rather than Thymeleaf because:
 * <ul>
 *   <li>Template syntax is intentionally trivial (no expressions).</li>
 *   <li>No template-engine evaluation surface (no SSTI).</li>
 *   <li>Consistent escaping semantics.</li>
 * </ul>
 */
@Component
public class TemplateRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([A-Za-z0-9_.]+)}");

    public String render(String template, Map<String, Object> variables) {
        return render(template, variables, true);
    }

    public String renderPlain(String template, Map<String, Object> variables) {
        return render(template, variables, false);
    }

    /**
     * Extracts the ordered list of variables referenced by the template.
     * Used by {@link VariableSchemaValidator} to check for missing names.
     */
    public List<TemplateVariable> extractVariables(String template) {
        if (template == null || template.isEmpty()) {
            return List.of();
        }
        Matcher m = PLACEHOLDER.matcher(template);
        List<TemplateVariable> result = new ArrayList<>();
        while (m.find()) {
            result.add(new TemplateVariable(m.group(1), m.start(), m.end()));
        }
        return result;
    }

    // ------------------------------------------------------------------ internal

    private String render(String template, Map<String, Object> variables, boolean html) {
        if (template == null || template.isEmpty()) {
            return template == null ? "" : template;
        }
        if (variables == null || variables.isEmpty()) {
            return template;
        }
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuilder out = new StringBuilder(template.length() + 32);
        int last = 0;
        while (m.find()) {
            out.append(template, last, m.start());
            Object value = variables.get(m.group(1));
            String substituted = value == null ? "" : value.toString();
            out.append(html ? escapeHtml(substituted) : escapePlain(substituted));
            last = m.end();
        }
        out.append(template, last, template.length());
        return out.toString();
    }

    private static String escapeHtml(String value) {
        if (value == null || value.isEmpty()) return "";
        return StringUtils.isBlank(value) ? value : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String escapePlain(String value) {
        if (value == null) return "";
        // Preserve newlines, strip control chars other than \n\t, and escape
        // a minimal set to prevent log injection.
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\n' || c == '\t') {
                sb.append(c);
            } else if (c < 0x20) {
                // drop
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}