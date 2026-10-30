package ch.admin.bit.jeap.governance.messagecontract;

import org.junit.jupiter.api.Test;

import static ch.admin.bit.jeap.governance.domain.plugin.rule.RuleExplanationCoverage.assertEveryRuleHasAnExplanation;

class RuleExplanationsTest {

    @Test
    void everyRuleOfTheModuleShipsItsExplanation() {
        assertEveryRuleHasAnExplanation(getClass().getPackageName(), 1);
    }
}
