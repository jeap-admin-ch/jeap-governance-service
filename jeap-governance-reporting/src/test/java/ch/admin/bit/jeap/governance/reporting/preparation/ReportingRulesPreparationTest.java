package ch.admin.bit.jeap.governance.reporting.preparation;

import ch.admin.bit.jeap.governance.domain.SystemComponentReference;
import ch.admin.bit.jeap.governance.domain.SystemReference;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleInfo;
import ch.admin.bit.jeap.governance.domain.rule.GracePeriodComponentEntry;
import ch.admin.bit.jeap.governance.domain.rule.NonCompliantComponentEntry;
import ch.admin.bit.jeap.governance.domain.rule.RuleConformanceRate;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import ch.admin.bit.jeap.governance.domain.rule.SystemRuleConformanceRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportingRulesPreparationTest {

    private static final String RULE_ID = "rule-1";
    private static final String RULE_LABEL = "My Rule";
    private static final long SYSTEM_ID = 10L;
    private static final String SYSTEM_NAME = "System A";
    private static final long COMPONENT_ID = 20L;
    private static final String COMPONENT_NAME = "Component X";
    private static final String GRACE_PERIOD_COMMENT = "Outdated message contracts:\nFirstEvent uses 1.0.0, latest is 2.0.0";
    private static final ZonedDateTime TIMESTAMP = ZonedDateTime.parse("2026-08-21T10:00:00+02:00[Europe/Zurich]");

    @Mock
    private ReportingDataAccess dataAccess;

    private ReportingRulesPreparation preparation;

    @BeforeEach
    void setUp() {
        preparation = new ReportingRulesPreparation(
                dataAccess, Clock.fixed(TIMESTAMP.toInstant(), ZoneId.of("Europe/Zurich")));
    }


    @Test
    void prepareAllRules_happyPath_returnsPopulatedRule() {
        List<RuleConformanceRate> conformanceRates = List.of(
                conformanceRate(RULE_ID, TIMESTAMP, 85)
        );
        List<SystemRuleConformanceRate> systemConformanceRates = List.of(
                systemConformanceRate(RULE_ID, SYSTEM_ID, 90)
        );
        List<RuleInfo> activeRules = List.of(ruleInfo(RULE_ID, RULE_LABEL));
        List<NonCompliantComponentEntry> nonCompliant = List.of(
                nonCompliantEntry(RULE_ID, SYSTEM_ID, COMPONENT_ID, TIMESTAMP.minusDays(5))
        );
        List<SystemReference> systems = List.of(systemRef(SYSTEM_ID, SYSTEM_NAME));
        List<SystemComponentReference> components = List.of(componentRef(COMPONENT_ID, COMPONENT_NAME));

        List<ReportingRule> result = preparation.prepareRules(
                conformanceRates, systemConformanceRates, activeRules,
                nonCompliant, List.of(), systems, components, Set.of()
        );

        assertThat(result).hasSize(1);
        ReportingRule rule = result.get(0);
        assertThat(rule.getRuleId()).isEqualTo(RULE_ID);
        assertThat(rule.getRuleName()).isEqualTo(RULE_LABEL);
        assertThat(rule.getConformanceRates()).hasSize(1);
        assertThat(rule.getSystemConformanceRates()).hasSize(1);
        assertThat(rule.getNonCompliantComponents()).hasSize(1);
    }

    @Test
    void prepareAllRules_noConformanceRatesForRule_ruleIsSkipped() {
        List<RuleConformanceRate> conformanceRates = List.of(); // no rates
        List<SystemRuleConformanceRate> systemConformanceRates = List.of();
        List<RuleInfo> activeRules = List.of(ruleInfo(RULE_ID, RULE_LABEL));
        List<NonCompliantComponentEntry> nonCompliant = List.of();
        List<SystemReference> systems = List.of();
        List<SystemComponentReference> components = List.of();

        List<ReportingRule> result = preparation.prepareRules(
                conformanceRates, systemConformanceRates, activeRules,
                nonCompliant, List.of(), systems, components, Set.of()
        );

        assertThat(result).isEmpty();
    }

    @Test
    void prepareAllRules_noActiveRules_returnsEmptyList() {
        List<ReportingRule> result = preparation.prepareRules(
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Set.of()
        );

        assertThat(result).isEmpty();
    }

    @Test
    void prepareAllRules_noSystemConformanceRates_ruleStillIncluded() {
        List<RuleConformanceRate> conformanceRates = List.of(
                conformanceRate(RULE_ID, TIMESTAMP, 90)
        );

        List<ReportingRule> result = preparation.prepareRules(
                conformanceRates, List.of(),
                List.of(ruleInfo(RULE_ID, RULE_LABEL)),
                List.of(), List.of(), List.of(systemRef(SYSTEM_ID, SYSTEM_NAME)), List.of(), Set.of()
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSystemConformanceRates()).isEmpty();
    }

    @Test
    void prepareAllRules_noNonCompliantComponents_ruleStillIncluded() {
        List<RuleConformanceRate> conformanceRates = List.of(
                conformanceRate(RULE_ID, TIMESTAMP, 100)
        );

        List<ReportingRule> result = preparation.prepareRules(
                conformanceRates, List.of(),
                List.of(ruleInfo(RULE_ID, RULE_LABEL)),
                List.of(), List.of(), List.of(), List.of(), Set.of()
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNonCompliantComponents()).isEmpty();
    }

    @Test
    void prepareAllRules_systemReferenceNotFoundForNonCompliantEntry_entrySkipped() {
        List<RuleConformanceRate> conformanceRates = List.of(
                conformanceRate(RULE_ID, TIMESTAMP, 70)
        );
        NonCompliantComponentEntry entryWithUnknownSystem = nonCompliantEntry(RULE_ID, 999L, COMPONENT_ID, TIMESTAMP);

        List<ReportingRule> result = preparation.prepareRules(
                conformanceRates, List.of(),
                List.of(ruleInfo(RULE_ID, RULE_LABEL)),
                List.of(entryWithUnknownSystem), List.of(),
                List.of(systemRef(SYSTEM_ID, SYSTEM_NAME)), // system 999 not present
                List.of(componentRef(COMPONENT_ID, COMPONENT_NAME)),
                Set.of()
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNonCompliantComponents()).isEmpty();
    }

    @Test
    void prepareAllRules_componentReferenceNotFoundForNonCompliantEntry_entrySkipped() {
        List<RuleConformanceRate> conformanceRates = List.of(
                conformanceRate(RULE_ID, TIMESTAMP, 70)
        );
        NonCompliantComponentEntry entryWithUnknownComponent = nonCompliantEntry(RULE_ID, SYSTEM_ID, 999L, TIMESTAMP);

        List<ReportingRule> result = preparation.prepareRules(
                conformanceRates, List.of(),
                List.of(ruleInfo(RULE_ID, RULE_LABEL)),
                List.of(entryWithUnknownComponent), List.of(),
                List.of(systemRef(SYSTEM_ID, SYSTEM_NAME)),
                List.of(componentRef(COMPONENT_ID, COMPONENT_NAME)), // component 999 not present
                Set.of()
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNonCompliantComponents()).isEmpty();
    }

    @Test
    void prepareAllRules_multipleRules_allReturned() {
        String ruleId2 = "rule-2";
        List<RuleConformanceRate> conformanceRates = List.of(
                conformanceRate(RULE_ID, TIMESTAMP, 85),
                conformanceRate(ruleId2, TIMESTAMP, 60)
        );
        List<RuleInfo> activeRules = List.of(
                ruleInfo(RULE_ID, RULE_LABEL),
                ruleInfo(ruleId2, "Rule Two")
        );

        List<ReportingRule> result = preparation.prepareRules(
                conformanceRates, List.of(), activeRules,
                List.of(), List.of(), List.of(), List.of(), Set.of()
        );

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ReportingRule::getRuleId)
                .containsExactlyInAnyOrder(RULE_ID, ruleId2);
    }

    @Test
    void prepareAllRules_ignoredComponentNonComplianceIsExcluded() {
        long gatewayComponentId = 21L;
        List<RuleConformanceRate> conformanceRates = List.of(
                conformanceRate(RULE_ID, TIMESTAMP, 50)
        );
        List<NonCompliantComponentEntry> nonCompliant = List.of(
                nonCompliantEntry(RULE_ID, SYSTEM_ID, COMPONENT_ID, TIMESTAMP.minusDays(2)),
                nonCompliantEntry(RULE_ID, SYSTEM_ID, gatewayComponentId, TIMESTAMP.minusDays(3))
        );
        List<SystemComponentReference> components = List.of(
                componentRef(COMPONENT_ID, COMPONENT_NAME),
                componentRef(gatewayComponentId, "Gateway X")
        );

        List<ReportingRule> result = preparation.prepareRules(
                conformanceRates, List.of(), List.of(ruleInfo(RULE_ID, RULE_LABEL)),
                nonCompliant, List.of(), List.of(systemRef(SYSTEM_ID, SYSTEM_NAME)), components, Set.of(gatewayComponentId)
        );

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getNonCompliantComponents())
                .singleElement()
                .satisfies(component -> {
                    assertThat(component.getComponentId()).isEqualTo(COMPONENT_ID);
                    assertThat(component.getComponentName()).isEqualTo(COMPONENT_NAME);
                });
    }

    @Test
    void prepareRules_addsGracePeriodComponentsForEveryConfiguredRule() {
        String secondRuleId = "rule-2";
        long secondComponentId = 21L;
        ZonedDateTime firstDetectedAt = TIMESTAMP.minusDays(2);
        ZonedDateTime secondDetectedAt = TIMESTAMP.minusDays(1);

        List<ReportingRule> result = preparation.prepareRules(
                List.of(
                        conformanceRate(RULE_ID, TIMESTAMP, 100),
                        conformanceRate(secondRuleId, TIMESTAMP, 100)),
                List.of(),
                List.of(
                        ruleInfo(RULE_ID, RULE_LABEL, Duration.ofDays(7)),
                        ruleInfo(secondRuleId, "Rule Two", Duration.ofDays(14))),
                List.of(),
                List.of(
                        gracePeriodEntry(RULE_ID, SYSTEM_ID, COMPONENT_ID, firstDetectedAt),
                        gracePeriodEntry(secondRuleId, SYSTEM_ID, secondComponentId, secondDetectedAt)),
                List.of(systemRef(SYSTEM_ID, SYSTEM_NAME)),
                List.of(
                        componentRef(COMPONENT_ID, COMPONENT_NAME),
                        componentRef(secondComponentId, "Component Y")),
                Set.of());

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(ReportingRule::hasViolationGracePeriod);
        assertThat(result).filteredOn(rule -> rule.getRuleId().equals(RULE_ID))
                .singleElement()
                .satisfies(rule -> assertThat(rule.getGracePeriodComponents())
                        .singleElement()
                        .satisfies(component -> {
                            assertThat(component.getStateComment()).isEqualTo(GRACE_PERIOD_COMMENT);
                            assertThat(component.getViolationDetectedAt()).isEqualTo(firstDetectedAt);
                            assertThat(component.getGracePeriodEndsAt()).isEqualTo(firstDetectedAt.plusDays(7));
                        }));
        assertThat(result).filteredOn(rule -> rule.getRuleId().equals(secondRuleId))
                .singleElement()
                .satisfies(rule -> assertThat(rule.getGracePeriodComponents())
                        .singleElement()
                        .extracting(ReportingRuleGracePeriodComponent::getGracePeriodEndsAt)
                        .isEqualTo(secondDetectedAt.plusDays(14)));
    }

    @Test
    void prepareRules_gracePeriodComponentsAreOrderedByExpiryAndIgnoredComponentsAreExcluded() {
        long urgentComponentId = 21L;
        long ignoredComponentId = 22L;
        Duration delay = Duration.ofDays(7);

        List<ReportingRule> result = preparation.prepareRules(
                List.of(conformanceRate(RULE_ID, TIMESTAMP, 100)),
                List.of(),
                List.of(ruleInfo(RULE_ID, RULE_LABEL, delay)),
                List.of(),
                List.of(
                        gracePeriodEntry(RULE_ID, SYSTEM_ID, COMPONENT_ID, TIMESTAMP.minusDays(2)),
                        gracePeriodEntry(RULE_ID, SYSTEM_ID, urgentComponentId, TIMESTAMP.minusDays(5)),
                        gracePeriodEntry(RULE_ID, SYSTEM_ID, ignoredComponentId, TIMESTAMP.minusDays(6))),
                List.of(systemRef(SYSTEM_ID, SYSTEM_NAME)),
                List.of(
                        componentRef(COMPONENT_ID, COMPONENT_NAME),
                        componentRef(urgentComponentId, "Urgent Component"),
                        componentRef(ignoredComponentId, "Ignored Component")),
                Set.of(ignoredComponentId));

        assertThat(result.getFirst().getGracePeriodComponents())
                .extracting(ReportingRuleGracePeriodComponent::getComponentId)
                .containsExactly(urgentComponentId, COMPONENT_ID);
    }

    @Test
    void prepareRules_withoutConfiguredDelayDoesNotExposeGracePeriodSection() {
        List<ReportingRule> result = preparation.prepareRules(
                List.of(conformanceRate(RULE_ID, TIMESTAMP, 100)),
                List.of(),
                List.of(ruleInfo(RULE_ID, RULE_LABEL)),
                List.of(),
                List.of(gracePeriodEntry(RULE_ID, SYSTEM_ID, COMPONENT_ID, TIMESTAMP.minusDays(1))),
                List.of(systemRef(SYSTEM_ID, SYSTEM_NAME)),
                List.of(componentRef(COMPONENT_ID, COMPONENT_NAME)),
                Set.of());

        assertThat(result.getFirst().hasViolationGracePeriod()).isFalse();
        assertThat(result.getFirst().getGracePeriodComponents()).isEmpty();
    }

    @Test
    void prepareRules_excludesExpiredGracePeriods() {
        Duration delay = Duration.ofDays(7);

        List<ReportingRule> result = preparation.prepareRules(
                List.of(conformanceRate(RULE_ID, TIMESTAMP, 100)),
                List.of(),
                List.of(ruleInfo(RULE_ID, RULE_LABEL, delay)),
                List.of(),
                List.of(
                        gracePeriodEntry(RULE_ID, SYSTEM_ID, COMPONENT_ID, TIMESTAMP.minusDays(8)),
                        gracePeriodEntry(RULE_ID, SYSTEM_ID, COMPONENT_ID + 1, TIMESTAMP.minusDays(7)),
                        gracePeriodEntry(RULE_ID, SYSTEM_ID, COMPONENT_ID + 2, TIMESTAMP.minusDays(6))),
                List.of(systemRef(SYSTEM_ID, SYSTEM_NAME)),
                List.of(
                        componentRef(COMPONENT_ID, "Expired"),
                        componentRef(COMPONENT_ID + 1, "Expires Now"),
                        componentRef(COMPONENT_ID + 2, "Active")),
                Set.of());

        assertThat(result.getFirst().getGracePeriodComponents())
                .singleElement()
                .extracting(ReportingRuleGracePeriodComponent::getComponentName)
                .isEqualTo("Active");
    }


    private RuleInfo ruleInfo(String ruleId, String label) {
        return new RuleInfo(RuleId.of(ruleId), label, label + " documentation link");
    }

    private RuleInfo ruleInfo(String ruleId, String label, Duration violationDelay) {
        return new RuleInfo(RuleId.of(ruleId), label, label + " documentation link", violationDelay);
    }

    private RuleConformanceRate conformanceRate(String ruleId, ZonedDateTime createdAt, int rate) {
        RuleConformanceRate ruleConformanceRate = mock(RuleConformanceRate.class);
        when(ruleConformanceRate.getRuleId()).thenReturn(ruleId);
        when(ruleConformanceRate.getCreatedAt()).thenReturn(createdAt);
        when(ruleConformanceRate.getConformanceRate()).thenReturn(rate);
        return ruleConformanceRate;
    }

    private SystemRuleConformanceRate systemConformanceRate(String ruleId, long systemId, int rate) {
        SystemRuleConformanceRate systemRuleConformanceRate = mock(SystemRuleConformanceRate.class);
        when(systemRuleConformanceRate.getRuleId()).thenReturn(ruleId);
        when(systemRuleConformanceRate.getSystemId()).thenReturn(systemId);
        when(systemRuleConformanceRate.getConformanceRate()).thenReturn(rate);
        return systemRuleConformanceRate;
    }

    private NonCompliantComponentEntry nonCompliantEntry(String ruleId, long systemId, long componentId, ZonedDateTime since) {
        NonCompliantComponentEntry nonCompliantComponentEntry = mock(NonCompliantComponentEntry.class);
        when(nonCompliantComponentEntry.getRuleId()).thenReturn(ruleId);
        when(nonCompliantComponentEntry.getSystemId()).thenReturn(systemId);
        when(nonCompliantComponentEntry.getSystemComponentId()).thenReturn(componentId);
        when(nonCompliantComponentEntry.getNonCompliantSince()).thenReturn(since);
        return nonCompliantComponentEntry;
    }

    private GracePeriodComponentEntry gracePeriodEntry(String ruleId, long systemId, long componentId,
                                                       ZonedDateTime violationDetectedAt) {
        GracePeriodComponentEntry entry = mock(GracePeriodComponentEntry.class);
        when(entry.getRuleId()).thenReturn(ruleId);
        when(entry.getSystemId()).thenReturn(systemId);
        when(entry.getSystemComponentId()).thenReturn(componentId);
        when(entry.getStateComment()).thenReturn(GRACE_PERIOD_COMMENT);
        when(entry.getViolationDetectedAt()).thenReturn(violationDetectedAt);
        return entry;
    }

    private SystemReference systemRef(long id, String name) {
        SystemReference systemReference = mock(SystemReference.class);
        when(systemReference.getId()).thenReturn(id);
        when(systemReference.getName()).thenReturn(name);
        return systemReference;
    }

    private SystemComponentReference componentRef(long id, String name) {
        SystemComponentReference systemComponentReference = mock(SystemComponentReference.class);
        when(systemComponentReference.getId()).thenReturn(id);
        when(systemComponentReference.getName()).thenReturn(name);
        return systemComponentReference;
    }
}
