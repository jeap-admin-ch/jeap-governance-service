package ch.admin.bit.jeap.governance.rules;

import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.time.LocalDate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.joining;

/// Configuration properties for governance rules. This allows to define which rules are active, rule parameters
/// and exemptions.
/// ```yaml
/// jeap:
///   governance:
///     rules:
///       active:
///         - id: enforce-oauth2
///           weight: 10
///           documentation-link: https://wiki.example.com/enforce-oauth2
///         - id: another-rule
///           weight: 10
///         - id: some-special-rule
///           weight: 5
///           parameters:
///             key: value
///             threshold: 10
///       component-exemptions:
///         - id: applicationplatform-basic-auth
///           component-name: applicationplatform-archrepo-service
///           rule-id:
///             - enforce-oauth2
///             - another-rule
///           reason: "foo bar"
///           until: "2026-12-24"
///           parameters:
///             key: value
/// ```
@Slf4j
@Data
@ConfigurationProperties(prefix = "jeap.governance.rules")
@RefreshScope
public class RuleConfigurationProperties {

    /**
     * List of activated rules, optionally with parameters.
     */
    private List<ActiveRule> active = new ArrayList<>();

    /**
     * Component-level exemptions for one or more rules.
     */
    private List<ComponentExemption> componentExemptions = new ArrayList<>();

    void logConfiguration() {
        String activeRules = active.stream()
                .map(ActiveRule::toString)
                .collect(joining(","));
        String exemptions = componentExemptions.stream()
                .map(ComponentExemption::toString)
                .collect(joining(","));
        log.info("Governance rules configuration loaded. Active Rules: {}, Exemptions: {}", activeRules, exemptions);
    }

    @Data
    public static class ActiveRule {

        /**
         * Rule id (e.g. enforce-oauth2).
         */
        private String id;

        /**
         * Rule weight for this rule, see scoring documentation for details. Must be provided, positive integer.
         */
        private Integer weight;

        /**
         * Optional delay before a continuous violation affects governance scoring.
         */
        private Duration violationDelay = Duration.ZERO;

        /**
         * Optional link to the documentation for this rule. Used when generating governance reports
         * to provide a reference for violation explanations.
         */
        private String documentationLink;

        /**
         * Optional parameters for the rule.
         */
        private Map<String, String> parameters = new HashMap<>();

        public RuleId getId() {
            return RuleId.of(id);
        }

        @Override
        public String toString() {
            return id;
        }
    }

    @Data
    public static class ComponentExemption {

        /**
         * Exemption id (unique).
         */
        private String id;

        /**
         * Component name this exemption applies to.
         */
        private String componentName;

        /**
         * List of rule ids this exemption applies to.
         */
        private List<String> ruleId = new ArrayList<>();

        /**
         * Reason why the exemption exists.
         */
        private String reason;

        /**
         * Optional end date of the exemption (ISO-8601 yyyy-MM-dd).
         */
        private LocalDate until;

        /**
         * Optional parameters to further scope the exemption.
         */
        private Map<String, String> parameters = new HashMap<>();

        public List<RuleId> getRuleIds() {
            return ruleId.stream().map(RuleId::of).toList();
        }

        @Override
        public String toString() {
            return "ComponentExemption{" +
                    "componentName='" + componentName + '\'' +
                    ", ruleId=" + ruleId +
                    ", until=" + until +
                    '}';
        }
    }
}
