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
@ActiveProfiles("component-dependencies-versions-rule-it")
class ComponentDependenciesVersionsRuleIT extends GovernanceIntegrationTestBase {

    @Autowired
    private SystemRepository systemRepository;

    @Autowired
    private RuleEvaluationService ruleEvaluationService;

    @Autowired
    private RuleStateRepository ruleStateRepository;

    @Autowired
    private JpaPromTimeSeriesRepository promTimeSeriesRepository;

    @Test
    void componentDependenciesVersionsUpToDate_resultsInOk() {
        var system = systemRepository.add(System.builder()
                .name("testsys1")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("service1").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        var component = system.getSystemComponents().getFirst();
        promTimeSeriesRepository.saveAll(createOkSamples(component));
        ruleEvaluationService.updateRuleStatesForComponent(component);

        var ruleState = ruleStateRepository.findBySystemComponentAndRuleId(component, RuleId.of("component-dependencies-versions"));
        assertThat(ruleState).isPresent();
        assertThat(ruleState.get().getState()).isEqualTo(State.OK);
    }

    @Test
    void componentDependenciesVersionsNotUpToDate_resultsInFail() {
        var system = systemRepository.add(System.builder()
                .name("testsys2")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("service2").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        var component = system.getSystemComponents().getFirst();
        promTimeSeriesRepository.saveAll(createNokSamples(component));
        ruleEvaluationService.updateRuleStatesForComponent(component);

        var ruleState = ruleStateRepository.findBySystemComponentAndRuleId(component, RuleId.of("component-dependencies-versions"));
        assertThat(ruleState).isPresent();
        assertThat(ruleState.get().getState()).isEqualTo(State.FAIL);
    }

    private List<PromTimeSeries> createOkSamples(SystemComponent component) {
        return List.of(
                createSample(component, "ch.admin.bit.jeap:jeap-spring-boot-application-starter","18.2.0"),
                createSample(component, "ch.admin.bit.jeap:jeap-messaging","9.3.1"),
                createSample(component, "ch.admin.bit.jeap:jeap-messaging-outbox","9.3.1"),
                createSample(component, "ch.admin.bit.jeap:jeap-messaging-sequential-inbox","9.3.1"),
                createSample(component, "ch.admin.bit.jeap:jeap-crypto","4.2.0"),
                createSample(component, "ch.admin.bit.jeap:jeap-error-handling-service","14.0.0"),
                createSample(component, "ch.admin.bit.jeap:jeap-process-context-scs","13.25.0"),
                createSample(component, "ch.admin.bit.jeap:jeap-process-archive-service","9.5.0"),
                createSample(component, "spring.boot","3.5.6")
        );
    }

    private List<PromTimeSeries> createNokSamples(SystemComponent component) {
        return List.of(
                createSample(component, "ch.admin.bit.jeap:jeap-spring-boot-application-starter","18.1.0"),
                createSample(component, "ch.admin.bit.jeap:jeap-messaging","9.3.0"),
                createSample(component, "ch.admin.bit.jeap:jeap-messaging-outbox","9.3.0"),
                createSample(component, "ch.admin.bit.jeap:jeap-messaging-sequential-inbox","9.3.0"),
                createSample(component, "ch.admin.bit.jeap:jeap-crypto","4.1.0"),
                createSample(component, "ch.admin.bit.jeap:jeap-error-handling-service","13.9.0"),
                createSample(component, "ch.admin.bit.jeap:jeap-process-context-scs","13.24.1"),
                createSample(component, "ch.admin.bit.jeap:jeap-process-archive-service","9.4.0"),
                createSample(component, "spring.boot","3.5.5")
        );
    }

    private PromTimeSeries createSample(SystemComponent component, String name, String version) {
        return PromTimeSeries.builder()
                        .prometheusQueryType(PromQueryType.JEAP_DEPENDENCY_VERSION)
                        .queryTimestamp(ZonedDateTime.now())
                        .systemComponentId(component.getId())
                        .sample(new PromTimeSeriesSample(Map.of("name", name, "version", version), List.of("1")))
                        .build();
    }


}
