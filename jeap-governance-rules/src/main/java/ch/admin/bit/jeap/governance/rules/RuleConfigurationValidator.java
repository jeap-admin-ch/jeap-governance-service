package ch.admin.bit.jeap.governance.rules;

import ch.admin.bit.jeap.governance.domain.plugin.rule.Rule;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import ch.admin.bit.jeap.governance.rules.RuleConfigurationProperties.ActiveRule;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates the rule configuration against the known rules at application startup and on configuration refresh.
 * Ensures that all active rules and component exemptions reference valid rule IDs.
 */
@Component
@Slf4j
class RuleConfigurationValidator {

    private final Set<RuleId> knownRuleIds;
    private final RuleConfigurationProperties properties;

    RuleConfigurationValidator(List<Rule> rules, RuleConfigurationProperties properties) {
        this.knownRuleIds = rules.stream()
                .map(rule -> rule.metadata().ruleId())
                .collect(Collectors.toSet());
        this.properties = properties;
    }

    @PostConstruct
    void validateOnStartup() {
        validateConfiguration(true);
        properties.logConfiguration();
    }

    @EventListener(RefreshScopeRefreshedEvent.class)
    void onConfigurationRefresh() {
        validateConfiguration(false);
        properties.logConfiguration();
    }

    void validateConfiguration(boolean failOnError) {
        List<RuleId> unknownActiveRuleIds = properties.getActive().stream()
                .map(ActiveRule::getId)
                .filter(id -> !knownRuleIds.contains(id))
                .toList();
        if (!unknownActiveRuleIds.isEmpty()) {
            log.error("Active rule(s) reference unknown rule ID(s): {}", unknownActiveRuleIds);
        }

        List<RuleId> unknownExemptionRuleIds = properties.getComponentExemptions().stream()
                .flatMap(exemption -> exemption.getRuleIds().stream())
                .distinct()
                .filter(id -> !knownRuleIds.contains(id))
                .toList();
        if (!unknownExemptionRuleIds.isEmpty()) {
            log.error("Component exemption(s) reference unknown rule ID(s): {}", unknownExemptionRuleIds);
        }

        if (failOnError && (!unknownActiveRuleIds.isEmpty() || !unknownExemptionRuleIds.isEmpty())) {
            throw new IllegalStateException("Rule configuration references unknown rule ID(s)");
        }
    }
}
