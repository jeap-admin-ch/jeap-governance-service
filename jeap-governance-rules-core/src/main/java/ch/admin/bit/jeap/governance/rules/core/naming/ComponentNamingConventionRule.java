package ch.admin.bit.jeap.governance.rules.core.naming;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.Rule;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleMetadata;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleResult;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates that system component names follow the convention {@code {system-name}-{context}-{type-id}}.
 */
@Component
class ComponentNamingConventionRule implements Rule {

    private static final RuleMetadata METADATA = RuleMetadata.builder()
            .ruleId(RuleId.of("component-naming-convention"))
            .label("Component Naming Convention")
            .build();

    private static final Pattern SYSTEM_NAME_PATTERN = Pattern.compile("[a-z][a-z0-9_]*");
    private static final Pattern CONTEXT_PATTERN = Pattern.compile("[a-z][a-z0-9-]*");

    private static final Set<String> VALID_TYPE_IDS = Set.of("service", "ui", "scs", "mobileapp", "gateway", "db");

    @Override
    public RuleMetadata metadata() {
        return METADATA;
    }

    @Override
    public RuleResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {
        String componentName = systemComponent.getName();
        String[] parts = componentName.split("-");

        if (parts.length < 3) {
            return RuleResult.failed(
                    "Component name '%s' must have at least 3 parts separated by '-' ({system-name}-{context}-{type-id}).".formatted(componentName));
        }

        List<String> violations = new ArrayList<>();

        String systemNamePart = parts[0];
        if (!SYSTEM_NAME_PATTERN.matcher(systemNamePart).matches()) {
            violations.add("System name part '%s' must match pattern [a-z]+[a-z0-9_]*.".formatted(systemNamePart));
        } else if (!matchesSystemNameOrAlias(systemNamePart, systemComponent)) {
            violations.add("System name part '%s' does not match system name '%s' or any of its aliases.".formatted(
                    systemNamePart, systemComponent.getSystem().getName()));
        }

        String contextPart = String.join("-", java.util.Arrays.copyOfRange(parts, 1, parts.length - 1));
        if (!CONTEXT_PATTERN.matcher(contextPart).matches()) {
            violations.add("Context part '%s' must match pattern [a-z]+[a-z0-9-]*.".formatted(contextPart));
        }

        String typeIdPart = parts[parts.length - 1];
        if (!VALID_TYPE_IDS.contains(typeIdPart)) {
            violations.add("Type-id '%s' is not valid. Must be one of: %s.".formatted(typeIdPart, VALID_TYPE_IDS));
        }

        if (violations.isEmpty()) {
            return RuleResult.ok();
        }

        return RuleResult.failed(String.join(" ", violations));
    }

    private static boolean matchesSystemNameOrAlias(String systemNamePart, SystemComponent systemComponent) {
        var system = systemComponent.getSystem();
        if (systemNamePart.equalsIgnoreCase(system.getName())) {
            return true;
        }
        var aliases = system.getAliases();
        if (aliases == null) {
            return false;
        }
        return aliases.stream().anyMatch(systemNamePart::equalsIgnoreCase);
    }
}
