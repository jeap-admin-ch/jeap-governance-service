package ch.admin.bit.jeap.governance.web.api.docs;

import ch.admin.bit.jeap.governance.domain.rule.RuleComponentExemption;
import ch.admin.bit.jeap.governance.domain.rule.RuleDescription;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import ch.admin.bit.jeap.governance.web.api.docs.DocsApiReader.RulesData;
import ch.admin.bit.jeap.governance.web.api.docs.model.ExemptionDto;
import ch.admin.bit.jeap.governance.web.api.docs.model.RuleDto;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class DocsApiDtoFactoryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 30);
    private static final RuleId RULE_ID = RuleId.of("rule");

    private final DocsApiDtoFactory dtoFactory =
            new DocsApiDtoFactory(new DocsApiEtagSupport(JsonMapper.builder().build()), ZoneOffset.UTC);

    @Test
    void exemptionsWithoutIdOrComponentName_areSortedLast() {
        RulesData data = rulesData(List.of(
                exemption(null, "component-b"),
                exemption("exemption-2", null),
                exemption("exemption-1", "component-b"),
                exemption("exemption-3", "component-a")));

        RuleDto rule = dtoFactory.rule(data, RULE_ID.id()).orElseThrow();

        assertThat(rule.exemptions())
                .extracting(ExemptionDto::component, ExemptionDto::id)
                .containsExactly(
                        tuple("component-a", "exemption-3"),
                        tuple("component-b", "exemption-1"),
                        tuple("component-b", null),
                        tuple(null, "exemption-2"));
        assertThat(dtoFactory.ruleIndex(data).rules()).hasSize(1);
    }

    @Test
    void ruleIndex_exemptionCount_skipsExpiredExemptions() {
        RulesData data = rulesData(List.of(
                exemption("permanent", "component-a"),
                exemption("paused", "component-b", TODAY.plusDays(1)),
                exemption("expired", "component-c", TODAY.minusDays(1))));

        assertThat(dtoFactory.ruleIndex(data).rules().getFirst().exemptionCount()).isEqualTo(2);
        assertThat(dtoFactory.rule(data, RULE_ID.id()).orElseThrow().exemptions()).hasSize(3);
    }

    private static RulesData rulesData(List<RuleComponentExemption> exemptions) {
        RuleDescription description = new RuleDescription(RULE_ID, "Rule", null, true, 1, Duration.ZERO, Map.of(),
                exemptions);
        return new RulesData(null, TODAY, List.of(description), Map.of(RULE_ID, BigDecimal.valueOf(100)), Map.of());
    }

    private static RuleComponentExemption exemption(String id, String componentName) {
        return exemption(id, componentName, null);
    }

    private static RuleComponentExemption exemption(String id, String componentName, LocalDate until) {
        return new RuleComponentExemption(id, componentName, "reason", until, Map.of());
    }
}
