package ch.admin.bit.jeap.governance.web;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.SystemRepository;
import ch.admin.bit.jeap.governance.domain.rule.RuleEvaluationService;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import ch.admin.bit.jeap.governance.domain.rule.RuleStateRepository;
import ch.admin.bit.jeap.governance.domain.rule.State;
import ch.admin.bit.jeap.governance.prometheus.domain.PromQueryType;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeries;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeriesSample;
import ch.admin.bit.jeap.governance.prometheus.persistence.JpaPromTimeSeriesRepository;
import ch.admin.bit.jeap.governance.reactionobserver.domain.ReactionObserverComponentLastObservationDate;
import ch.admin.bit.jeap.governance.reactionobserver.domain.ReactionObserverComponentLastObservationDateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "jeap.governance.prometheus.enabled=true",
        "jeap.governance.reactionobserver.enabled=true"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("component-observes-reactions-rule-it")
class ComponentObservesReactionRuleIT extends GovernanceIntegrationTestBase {

    @Autowired
    private SystemRepository systemRepository;

    @Autowired
    private RuleEvaluationService ruleEvaluationService;

    @Autowired
    private RuleStateRepository ruleStateRepository;

    @Autowired
    private JpaPromTimeSeriesRepository promTimeSeriesRepository;

    @Autowired
    private ReactionObserverComponentLastObservationDateRepository lastObservationDateRepository;

    @Test
    void componentObservesReactions_resultsInOk() {
        var system = systemRepository.add(System.builder()
                .name("testsys")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("service1").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        var component = system.getSystemComponents().getFirst();
        ruleEvaluationService.updateRuleStatesForComponent(component);

        var ruleState = ruleStateRepository.findBySystemComponentAndRuleId(component, RuleId.of("component-observes-reactions"));
        assertThat(ruleState).isPresent();
        assertThat(ruleState.get().getState()).isEqualTo(State.OK);
    }

    @Test
    void componentIgnoredByRuleParameter_resultsInOk() {
        var system = systemRepository.add(System.builder()
                .name("testsys3")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("my-ignored-service-to-skip").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        var component = system.getSystemComponents().getFirst();
        ruleEvaluationService.updateRuleStatesForComponent(component);

        var ruleState = ruleStateRepository.findBySystemComponentAndRuleId(component, RuleId.of("component-observes-reactions"));
        assertThat(ruleState).isPresent();
        assertThat(ruleState.get().getState()).isEqualTo(State.OK);
        assertThat(ruleState.get().getStateComment()).isEqualTo("Component ignored for this rule");
    }

    @Test
    void componentDoesntObservesReactions_resultsInFail() {
        var system = systemRepository.add(System.builder()
                .name("testsys2")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("service-without-reactions").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        var component = system.getSystemComponents().getFirst();
        promTimeSeriesRepository.saveAll(List.of(
                PromTimeSeries.builder()
                        .prometheusQueryType(PromQueryType.JEAP_MESSAGING_TOTAL)
                        .queryTimestamp(ZonedDateTime.now())
                        .systemComponentId(component.getId())
                        .sample(new PromTimeSeriesSample(Map.of("service-without-reactions", "metricsys-app-service"), List.of("1")))
                        .build()
        ));

        ruleEvaluationService.updateRuleStatesForComponent(component);

        var ruleState = ruleStateRepository.findBySystemComponentAndRuleId(component, RuleId.of("component-observes-reactions"));
        assertThat(ruleState).isPresent();
        assertThat(ruleState.get().getState()).isEqualTo(State.FAIL);
    }

    @Test
    void componentDoesntObservesReactionsSince6Days_resultsInFail() {
        var system = systemRepository.add(System.builder()
                .name("testsys4")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("service-without-reactions-since-days").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        var component = system.getSystemComponents().getFirst();
        promTimeSeriesRepository.saveAll(List.of(
                PromTimeSeries.builder()
                        .prometheusQueryType(PromQueryType.JEAP_MESSAGING_TOTAL)
                        .queryTimestamp(ZonedDateTime.now())
                        .systemComponentId(component.getId())
                        .sample(new PromTimeSeriesSample(Map.of("service-without-reactions-since-days", "metricsys-app-service"), List.of("1")))
                        .build()
        ));
        LocalDate lastObservationDate = LocalDate.now().minusDays(6);
        lastObservationDateRepository.add(ReactionObserverComponentLastObservationDate.builder()
                .systemComponent(system.getSystemComponents().getFirst())
                .lastObservationDate(lastObservationDate).build());

        ruleEvaluationService.updateRuleStatesForComponent(component);

        var ruleState = ruleStateRepository.findBySystemComponentAndRuleId(component, RuleId.of("component-observes-reactions"));
        assertThat(ruleState).isPresent();
        assertThat(ruleState.get().getState()).isEqualTo(State.FAIL);
        assertThat(ruleState.get().getStateComment()).isEqualTo("Component has not observed any reactions in the last 4 days. Last observation date: " + lastObservationDate);
    }
}
