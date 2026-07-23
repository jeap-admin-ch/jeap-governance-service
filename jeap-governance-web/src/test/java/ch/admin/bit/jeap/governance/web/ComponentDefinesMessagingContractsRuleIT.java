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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "jeap.governance.prometheus.enabled=true")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("component-defines-messagingcontracts-rule-it")
class ComponentDefinesMessagingContractsRuleIT extends GovernanceIntegrationTestBase {

    @Autowired
    private SystemRepository systemRepository;

    @Autowired
    private RuleEvaluationService ruleEvaluationService;

    @Autowired
    private RuleStateRepository ruleStateRepository;

    @Autowired
    private JpaPromTimeSeriesRepository promTimeSeriesRepository;

    @Test
    void componentDefinesMessagingContracts_resultsInOk() {
        var system = systemRepository.add(System.builder()
                .name("testsys")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("service1").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        var component = system.getSystemComponents().getFirst();
        ruleEvaluationService.updateRuleStatesForComponent(component);

        var ruleState = ruleStateRepository.findBySystemComponentAndRuleId(component, RuleId.of("component-defines-messagingcontracts"));
        assertThat(ruleState).isPresent();
        assertThat(ruleState.get().getState()).isEqualTo(State.OK);
    }


    @Test
    void componentDoesNotDefineMessagingContracts_resultsInFail() {
        var system = systemRepository.add(System.builder()
                .name("testsys2")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("service2").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        var component = system.getSystemComponents().getFirst();
        promTimeSeriesRepository.saveAll(List.of(
                PromTimeSeries.builder()
                        .prometheusQueryType(PromQueryType.JEAP_MESSAGING_CONTRACT)
                        .queryTimestamp(ZonedDateTime.now())
                        .systemComponentId(component.getId())
                        .sample(new PromTimeSeriesSample(Map.of("switch", "noMasterContracts"), List.of("1")))
                        .build()
        ));

        ruleEvaluationService.updateRuleStatesForComponent(component);

        var ruleState = ruleStateRepository.findBySystemComponentAndRuleId(component, RuleId.of("component-defines-messagingcontracts"));
        assertThat(ruleState).isPresent();
        assertThat(ruleState.get().getState()).isEqualTo(State.FAIL);
    }

    @Test
    void componentSilentlyIgnoresMessagesWithoutContract_resultsInFail() {
        var system = systemRepository.add(System.builder()
                .name("testsys3")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("service3").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        var component = system.getSystemComponents().getFirst();
        promTimeSeriesRepository.saveAll(List.of(
                PromTimeSeries.builder()
                        .prometheusQueryType(PromQueryType.JEAP_MESSAGING_CONTRACT)
                        .queryTimestamp(ZonedDateTime.now())
                        .systemComponentId(component.getId())
                        .sample(new PromTimeSeriesSample(Map.of("switch", "silentIgnoreWithoutContract"), List.of("1")))
                        .build()
        ));

        ruleEvaluationService.updateRuleStatesForComponent(component);

        var ruleState = ruleStateRepository.findBySystemComponentAndRuleId(component, RuleId.of("component-defines-messagingcontracts"));
        assertThat(ruleState).isPresent();
        assertThat(ruleState.get().getState()).isEqualTo(State.FAIL);
    }
}
