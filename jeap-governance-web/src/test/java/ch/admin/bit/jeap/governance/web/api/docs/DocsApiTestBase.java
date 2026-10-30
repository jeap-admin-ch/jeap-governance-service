package ch.admin.bit.jeap.governance.web.api.docs;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.Rule;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleMetadata;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleResult;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import ch.admin.bit.jeap.governance.web.GovernanceApplication;
import ch.admin.bit.jeap.governance.web.GovernanceIntegrationTestBase;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.JeapAuthenticationTestTokenBuilder;
import ch.admin.bit.jeap.security.test.resource.configuration.DisableJeapPermitAllSecurityConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * The application as an instance runs it, with the docs API's filter chain and two rules of an instance's own:
 * {@value #RULE_A} ships an explanation, {@value #RULE_B} none. The system name of the semantic roles is the one
 * configured in {@code application-docs-api-it.yaml}.
 */
@SpringBootTest(classes = GovernanceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("docs-api-it")
// Without this, the permit-all chain of jeap-spring-boot-security-starter-test shadows the docs API's chain
@Import({DisableJeapPermitAllSecurityConfiguration.class, DocsApiTestBase.TestRules.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
abstract class DocsApiTestBase extends GovernanceIntegrationTestBase {

    static final String RULE_A = "docs-rule-a";
    static final String RULE_B = "docs-rule-b";
    private static final String SYSTEM_NAME = "jme";

    static class TestRules {

        @Bean
        Rule docsRuleA() {
            return new TestRule(RULE_A, "Docs Rule A");
        }

        @Bean
        Rule docsRuleB() {
            return new TestRule(RULE_B, "Docs Rule B");
        }
    }

    protected static JeapAuthenticationToken tokenWithDocumentationRead() {
        return tokenWithRole(role(SYSTEM_NAME, "governance-documentation", "read"));
    }

    protected static JeapAuthenticationToken tokenWithDocumentationWrite() {
        return tokenWithRole(role(SYSTEM_NAME, "governance-documentation", "write"));
    }

    protected static JeapAuthenticationToken tokenOfAnotherSystem() {
        return tokenWithRole(role("another-system", "governance-documentation", "read"));
    }

    protected static JeapAuthenticationToken tokenWithGovernanceAdmin() {
        return tokenWithRole(role(SYSTEM_NAME, "governance", "admin"));
    }

    protected static JeapAuthenticationToken tokenWithoutRoles() {
        return JeapAuthenticationTestTokenBuilder.create().build();
    }

    private static JeapAuthenticationToken tokenWithRole(SemanticApplicationRole role) {
        return JeapAuthenticationTestTokenBuilder.create().withUserRoles(role).build();
    }

    private static SemanticApplicationRole role(String system, String resource, String operation) {
        return SemanticApplicationRole.builder()
                .system(system)
                .resource(resource)
                .operation(operation)
                .build();
    }

    private record TestRule(String ruleId, String label) implements Rule {
        @Override
        public RuleMetadata metadata() {
            return new RuleMetadata(RuleId.of(ruleId), label);
        }

        @Override
        public RuleResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {
            return RuleResult.ok();
        }
    }
}
