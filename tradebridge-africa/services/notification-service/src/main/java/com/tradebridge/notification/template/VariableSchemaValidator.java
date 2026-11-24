
package com.tradebridge.notification.template;

import com.tradebridge.common.exception.ValidationException;
import com.tradebridge.common.response.FieldError;
import com.tradebridge.notification.domain.NotificationTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class VariableSchemaValidator {

    private final TemplateRenderer renderer;

    public VariableSchemaValidator(TemplateRenderer renderer) {
        this.renderer = renderer;
    }

    public void validate(
        NotificationTemplate template,
        Map<String, Object> variables) {

        final Map<String, Object> suppliedVariables =
            variables == null ? Map.of() : variables;

        Set<String> required = collectRequired(template);

        List<String> missing = required.stream()
            .filter(name -> !suppliedVariables.containsKey(name))
            .sorted()
            .toList();

        if (!missing.isEmpty()) {
            List<FieldError> errors = missing.stream()
                .map(f -> new FieldError(
                    "variables." + f,
                    "Required variable is missing"))
                .toList();

            throw new ValidationException(
                "Missing required template variables",
                errors);
        }
    }

    private Set<String> collectRequired(NotificationTemplate template) {
        Set<String> required = new java.util.HashSet<>();

        required.addAll(
            names(renderer.extractVariables(
                template.getSubjectTemplate())));

        required.addAll(
            names(renderer.extractVariables(
                template.getBodyTemplate())));

        return required;
    }

    private static Set<String> names(List<TemplateVariable> vars) {
        return vars.stream()
            .map(TemplateVariable::name)
            .collect(Collectors.toSet());
    }
}
