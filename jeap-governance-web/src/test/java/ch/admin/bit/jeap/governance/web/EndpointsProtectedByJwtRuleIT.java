package ch.admin.bit.jeap.governance.web;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.SystemRepository;
import ch.admin.bit.jeap.governance.domain.rule.RuleEvaluationService;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import ch.admin.bit.jeap.governance.domain.rule.RuleStateRepository;
import ch.admin.bit.jeap.governance.domain.rule.State;
import ch.admin.bit.jeap.governance.prometheus.domain.PromClient;
import ch.admin.bit.jeap.governance.prometheus.domain.PromQueryType;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeries;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeriesSample;
import ch.admin.bit.jeap.governance.prometheus.persistence.JpaPromTimeSeriesRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@SpringBootTest(properties = {"jeap.governance.prometheus.enabled=true"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("endpoints-protected-by-jwt-rule-it")
class EndpointsProtectedByJwtRuleIT extends GovernanceIntegrationTestBase {

    @Autowired
    private SystemRepository systemRepository;

    @Autowired
    private RuleEvaluationService ruleEvaluationService;

    @Autowired
    private RuleStateRepository ruleStateRepository;

    @Autowired
    private JpaPromTimeSeriesRepository promTimeSeriesRepository;

    // We provide the relevant PromTimeSeries data directly via the repository
    @MockitoBean
    private PromClient promClient;

    private static final RuleId RULE_ID = RuleId.of("endpoints-protected-by-jwt");

    @Test
    void noEndpointsWithoutJwt_resultsInOk() {
        var system = systemRepository.add(System.builder()
                .name("jwtsys")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("jwtsys-app-service").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        var component = system.getSystemComponents().getFirst();
        ruleEvaluationService.updateRuleStatesForComponent(component);

        var ruleState = ruleStateRepository.findBySystemComponentAndRuleId(component, RULE_ID);
        assertThat(ruleState).isPresent();
        assertThat(ruleState.get().getState()).isEqualTo(State.OK);
    }

    @Test
    void endpointWithoutJwt_resultsInFail() {
        var system = systemRepository.add(System.builder()
                .name("jwtfailsys")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("jwtfailsys-app-service").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        var component = system.getSystemComponents().getFirst();
        promTimeSeriesRepository.saveAll(List.of(
                PromTimeSeries.builder()
                        .prometheusQueryType(PromQueryType.JEAP_REST_ENDPOINT_WITHOUT_JWT)
                        .queryTimestamp(ZonedDateTime.now())
                        .systemComponentId(component.getId())
                        .sample(new PromTimeSeriesSample(
                                Map.of("datapoint", "/api/unprotected", "method", "GET", "stage", "ref"),
                                List.of("1")))
                        .build()
        ));

        ruleEvaluationService.updateRuleStatesForComponent(component);

        var ruleState = ruleStateRepository.findBySystemComponentAndRuleId(component, RULE_ID);
        assertThat(ruleState).isPresent();
        assertThat(ruleState.get().getState()).isEqualTo(State.FAIL);
    }

    @Test
    void endpointIgnoredByExemption_resultsInOk() {
        var system = systemRepository.add(System.builder()
                .name("jwtexemptsys")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("jwtexemptsys-app-service").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        var component = system.getSystemComponents().getFirst();
        promTimeSeriesRepository.saveAll(List.of(
                PromTimeSeries.builder()
                        .prometheusQueryType(PromQueryType.JEAP_REST_ENDPOINT_WITHOUT_JWT)
                        .queryTimestamp(ZonedDateTime.now())
                        .systemComponentId(component.getId())
                        .sample(new PromTimeSeriesSample(
                                Map.of("datapoint", "/exempt/path", "method", "GET", "stage", "ref"),
                                List.of("1")))
                        .build(),
                PromTimeSeries.builder()
                        .prometheusQueryType(PromQueryType.JEAP_REST_ENDPOINT_WITHOUT_JWT)
                        .queryTimestamp(ZonedDateTime.now())
                        .systemComponentId(component.getId())
                        .sample(new PromTimeSeriesSample(
                                Map.of("datapoint", "/exempt/wildcard/foo", "method", "POST", "stage", "ref"),
                                List.of("1")))
                        .build()
        ));

        ruleEvaluationService.updateRuleStatesForComponent(component);

        var ruleState = ruleStateRepository.findBySystemComponentAndRuleId(component, RULE_ID);
        assertThat(ruleState).isPresent();
        assertThat(ruleState.get().getState()).isEqualTo(State.OK);
    }

    @Test
    void endpointIgnoredByExemptMethods_resultsInOk() {
        var system = systemRepository.add(System.builder()
                .name("jwtexemptmethodsys")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("jwtexemptmethodsys-app-service").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        var component = system.getSystemComponents().getFirst();
        promTimeSeriesRepository.saveAll(List.of(
                PromTimeSeries.builder()
                        .prometheusQueryType(PromQueryType.JEAP_REST_ENDPOINT_WITHOUT_JWT)
                        .queryTimestamp(ZonedDateTime.now())
                        .systemComponentId(component.getId())
                        .sample(new PromTimeSeriesSample(
                                Map.of("datapoint", "/api/preflight", "method", "OPTIONS", "stage", "ref"),
                                List.of("1")))
                        .build()
        ));

        ruleEvaluationService.updateRuleStatesForComponent(component);

        var ruleState = ruleStateRepository.findBySystemComponentAndRuleId(component, RULE_ID);
        assertThat(ruleState).isPresent();
        assertThat(ruleState.get().getState()).isEqualTo(State.OK);
    }

    @Test
    void endpointWithoutJwt_stateCommentContainsEndpoint() {
        var system = systemRepository.add(System.builder()
                .name("jwtfailcommentsys")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("jwtfailcommentsys-app-service").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        var component = system.getSystemComponents().getFirst();
        promTimeSeriesRepository.saveAll(List.of(
                PromTimeSeries.builder()
                        .prometheusQueryType(PromQueryType.JEAP_REST_ENDPOINT_WITHOUT_JWT)
                        .queryTimestamp(ZonedDateTime.now())
                        .systemComponentId(component.getId())
                        .sample(new PromTimeSeriesSample(
                                Map.of("datapoint", "/api/unprotected", "method", "GET", "stage", "ref"),
                                List.of("1")))
                        .build()
        ));

        ruleEvaluationService.updateRuleStatesForComponent(component);

        var ruleState = ruleStateRepository.findBySystemComponentAndRuleId(component, RULE_ID);
        assertThat(ruleState).isPresent();
        assertThat(ruleState.get().getState()).isEqualTo(State.FAIL);
        assertThat(ruleState.get().getStateComment()).contains("GET /api/unprotected");
        assertThat(ruleState.get().getStateComment()).contains("without a JWT bearer token");
    }

    @Test
    void exemptedComponent_resultsInDisabled() {
        var system = systemRepository.add(System.builder()
                .name("jwtexemptedcompsys")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("jwtexemptedcomp-app-service").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        var component = system.getSystemComponents().getFirst();
        promTimeSeriesRepository.saveAll(List.of(
                PromTimeSeries.builder()
                        .prometheusQueryType(PromQueryType.JEAP_REST_ENDPOINT_WITHOUT_JWT)
                        .queryTimestamp(ZonedDateTime.now())
                        .systemComponentId(component.getId())
                        .sample(new PromTimeSeriesSample(
                                Map.of("datapoint", "/api/unprotected", "method", "GET", "stage", "ref"),
                                List.of("1")))
                        .build()
        ));

        ruleEvaluationService.updateRuleStatesForComponent(component);

        var ruleState = ruleStateRepository.findBySystemComponentAndRuleId(component, RULE_ID);
        assertThat(ruleState).isPresent();
        assertThat(ruleState.get().getState()).isEqualTo(State.DISABLED);
    }

    @Test
    void exemptedUntilComponent_resultsInPaused() {
        var system = systemRepository.add(System.builder()
                .name("jwtexempteduntilcompsys")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("jwtexempteduntilcomp-app-service").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        var component = system.getSystemComponents().getFirst();
        promTimeSeriesRepository.saveAll(List.of(
                PromTimeSeries.builder()
                        .prometheusQueryType(PromQueryType.JEAP_REST_ENDPOINT_WITHOUT_JWT)
                        .queryTimestamp(ZonedDateTime.now())
                        .systemComponentId(component.getId())
                        .sample(new PromTimeSeriesSample(
                                Map.of("datapoint", "/api/unprotected", "method", "GET", "stage", "ref"),
                                List.of("1")))
                        .build()
        ));

        ruleEvaluationService.updateRuleStatesForComponent(component);

        var ruleState = ruleStateRepository.findBySystemComponentAndRuleId(component, RULE_ID);
        assertThat(ruleState).isPresent();
        assertThat(ruleState.get().getState()).isEqualTo(State.PAUSED);
    }

    @Test
    void endpointIgnoredByExemptEndpoints_resultsInOk() {
        var system = systemRepository.add(System.builder()
                .name("jwtexemptendpointsys")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("jwtexemptendpointsys-app-service").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        var component = system.getSystemComponents().getFirst();
        promTimeSeriesRepository.saveAll(List.of(
                PromTimeSeries.builder()
                        .prometheusQueryType(PromQueryType.JEAP_REST_ENDPOINT_WITHOUT_JWT)
                        .queryTimestamp(ZonedDateTime.now())
                        .systemComponentId(component.getId())
                        .sample(new PromTimeSeriesSample(
                                Map.of("datapoint", "/exempt/endpoint", "method", "GET", "stage", "ref"),
                                List.of("1")))
                        .build()
        ));

        ruleEvaluationService.updateRuleStatesForComponent(component);

        var ruleState = ruleStateRepository.findBySystemComponentAndRuleId(component, RULE_ID);
        assertThat(ruleState).isPresent();
        assertThat(ruleState.get().getState()).isEqualTo(State.OK);
    }
}
