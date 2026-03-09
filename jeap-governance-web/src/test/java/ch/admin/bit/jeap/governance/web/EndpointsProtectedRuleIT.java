package ch.admin.bit.jeap.governance.web;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.SystemRepository;
import ch.admin.bit.jeap.governance.domain.rule.RuleEvaluationService;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import ch.admin.bit.jeap.governance.domain.rule.RuleStateRepository;
import ch.admin.bit.jeap.governance.domain.rule.State;
import ch.admin.bit.jeap.governance.secscan.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@SpringBootTest(properties = {"jeap.governance.secscan.enabled=true"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("endpoints-protected-rule-it")
class EndpointsProtectedRuleIT extends GovernanceIntegrationTestBase {

    @Autowired
    private SystemRepository systemRepository;

    @Autowired
    private RuleEvaluationService ruleEvaluationService;

    @Autowired
    private RuleStateRepository ruleStateRepository;

    @Autowired
    private SecscanStateRepository secscanStateRepository;

    @Autowired
    private SecscanFlaggedEndpointRepository secscanFlaggedEndpointRepository;

    // Mock external dependencies that are not needed for the rule evaluation
    @MockitoBean
    private SystemComponentHttpApiDiscoveryClient apiDiscoveryClient;

    @MockitoBean
    private HttpEndpointSecurityChecker httpEndpointSecurityChecker;

    private static final RuleId RULE_ID = RuleId.of("endpoints-protected");

    @Test
    void noFlaggedEndpoints_resultsInOk() {
        var system = systemRepository.add(System.builder()
                .name("secscanokesys")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("secscanokesys-app-service").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        var component = system.getSystemComponents().getFirst();

        // Insert scan state but no flagged endpoints
        secscanStateRepository.save(SecscanState.builder()
                .systemComponentId(component.getId())
                .scanMessage("Scan completed successfully")
                .scanTimestamp(ZonedDateTime.now())
                .build());

        ruleEvaluationService.updateRuleStatesForComponent(component);

        var ruleState = ruleStateRepository.findBySystemComponentAndRuleId(component, RULE_ID);
        assertThat(ruleState).isPresent();
        assertThat(ruleState.get().getState()).isEqualTo(State.OK);
    }

    @Test
    void flaggedEndpoint_resultsInFail() {
        var system = systemRepository.add(System.builder()
                .name("secscanfailsys")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("secscanfailsys-app-service").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        var component = system.getSystemComponents().getFirst();

        // Insert scan state and a flagged endpoint
        secscanStateRepository.save(SecscanState.builder()
                .systemComponentId(component.getId())
                .scanMessage("Scan flagged 1 endpoint")
                .scanTimestamp(ZonedDateTime.now())
                .build());

        secscanFlaggedEndpointRepository.saveAll(List.of(
                SecscanFlaggedEndpoint.builder()
                        .systemComponentId(component.getId())
                        .path("/api/unprotected")
                        .method("GET")
                        .scanMessage("HTTP 200 returned without JWT")
                        .scanTimestamp(ZonedDateTime.now())
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
                .name("secscanexemptsys")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("secscanexemptsys-app-service").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        var component = system.getSystemComponents().getFirst();

        // Insert scan state and flagged endpoints that match the exemption config
        secscanStateRepository.save(SecscanState.builder()
                .systemComponentId(component.getId())
                .scanMessage("Scan flagged 2 endpoints")
                .scanTimestamp(ZonedDateTime.now())
                .build());

        secscanFlaggedEndpointRepository.saveAll(List.of(
                SecscanFlaggedEndpoint.builder()
                        .systemComponentId(component.getId())
                        .path("/exempt/path")
                        .method("GET")
                        .scanMessage("HTTP 200 returned")
                        .scanTimestamp(ZonedDateTime.now())
                        .build(),
                SecscanFlaggedEndpoint.builder()
                        .systemComponentId(component.getId())
                        .path("/exempt/wildcard/foo")
                        .method("POST")
                        .scanMessage("HTTP 200 returned")
                        .scanTimestamp(ZonedDateTime.now())
                        .build()
        ));

        ruleEvaluationService.updateRuleStatesForComponent(component);

        var ruleState = ruleStateRepository.findBySystemComponentAndRuleId(component, RULE_ID);
        assertThat(ruleState).isPresent();
        assertThat(ruleState.get().getState()).isEqualTo(State.OK);
    }

    @Test
    void endpointIgnoredByDefault_resultsInOk() {
        var system = systemRepository.add(System.builder()
                .name("secscandefaultsys")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("secscandefaultsys-app-service").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        var component = system.getSystemComponents().getFirst();

        // Insert scan state and flagged endpoints that should be ignored by default
        secscanStateRepository.save(SecscanState.builder()
                .systemComponentId(component.getId())
                .scanMessage("Scan flagged 2 endpoints")
                .scanTimestamp(ZonedDateTime.now())
                .build());

        secscanFlaggedEndpointRepository.saveAll(List.of(
                SecscanFlaggedEndpoint.builder()
                        .systemComponentId(component.getId())
                        .path("/actuator/health")
                        .method("GET")
                        .scanMessage("HTTP 200 returned")
                        .scanTimestamp(ZonedDateTime.now())
                        .build(),
                SecscanFlaggedEndpoint.builder()
                        .systemComponentId(component.getId())
                        .path("/swagger-ui")
                        .method("GET")
                        .scanMessage("HTTP 200 returned")
                        .scanTimestamp(ZonedDateTime.now())
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
                .name("secscanexemptmethodsys")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("secscanexemptmethodsys-app-service").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        var component = system.getSystemComponents().getFirst();

        secscanStateRepository.save(SecscanState.builder()
                .systemComponentId(component.getId())
                .scanMessage("Scan flagged 1 endpoint")
                .scanTimestamp(ZonedDateTime.now())
                .build());

        secscanFlaggedEndpointRepository.saveAll(List.of(
                SecscanFlaggedEndpoint.builder()
                        .systemComponentId(component.getId())
                        .path("/api/preflight")
                        .method("OPTIONS")
                        .scanMessage("HTTP 200 returned")
                        .scanTimestamp(ZonedDateTime.now())
                        .build()
        ));

        ruleEvaluationService.updateRuleStatesForComponent(component);

        var ruleState = ruleStateRepository.findBySystemComponentAndRuleId(component, RULE_ID);
        assertThat(ruleState).isPresent();
        assertThat(ruleState.get().getState()).isEqualTo(State.OK);
    }

    @Test
    void endpointIgnoredByExemptPaths_resultsInOk() {
        var system = systemRepository.add(System.builder()
                .name("secscanexemptpathsys")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("secscanexemptpathsys-app-service").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        var component = system.getSystemComponents().getFirst();

        secscanStateRepository.save(SecscanState.builder()
                .systemComponentId(component.getId())
                .scanMessage("Scan flagged 1 endpoint")
                .scanTimestamp(ZonedDateTime.now())
                .build());

        secscanFlaggedEndpointRepository.saveAll(List.of(
                SecscanFlaggedEndpoint.builder()
                        .systemComponentId(component.getId())
                        .path("/exempt/pathonly")
                        .method("GET")
                        .scanMessage("HTTP 200 returned")
                        .scanTimestamp(ZonedDateTime.now())
                        .build()
        ));

        ruleEvaluationService.updateRuleStatesForComponent(component);

        var ruleState = ruleStateRepository.findBySystemComponentAndRuleId(component, RULE_ID);
        assertThat(ruleState).isPresent();
        assertThat(ruleState.get().getState()).isEqualTo(State.OK);
    }

    @Test
    void flaggedEndpoint_stateCommentContainsEndpoint() {
        var system = systemRepository.add(System.builder()
                .name("secscanfailcommentsys")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("secscanfailcommentsys-app-service").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        var component = system.getSystemComponents().getFirst();

        secscanStateRepository.save(SecscanState.builder()
                .systemComponentId(component.getId())
                .scanMessage("Scan flagged 1 endpoint")
                .scanTimestamp(ZonedDateTime.now())
                .build());

        secscanFlaggedEndpointRepository.saveAll(List.of(
                SecscanFlaggedEndpoint.builder()
                        .systemComponentId(component.getId())
                        .path("/api/unprotected")
                        .method("GET")
                        .scanMessage("HTTP 200 returned without JWT")
                        .scanTimestamp(ZonedDateTime.now())
                        .build()
        ));

        ruleEvaluationService.updateRuleStatesForComponent(component);

        var ruleState = ruleStateRepository.findBySystemComponentAndRuleId(component, RULE_ID);
        assertThat(ruleState).isPresent();
        assertThat(ruleState.get().getState()).isEqualTo(State.FAIL);
        assertThat(ruleState.get().getStateComment()).contains("GET /api/unprotected");
        assertThat(ruleState.get().getStateComment()).contains("HTTP 200 returned without JWT");
    }

    @Test
    void endpointIgnoredByExemptComponentNames_resultsInOk() {
        var system = systemRepository.add(System.builder()
                .name("secscanexemptbynamesys")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("myapp-exemptbyname-service").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        var component = system.getSystemComponents().getFirst();

        secscanStateRepository.save(SecscanState.builder()
                .systemComponentId(component.getId())
                .scanMessage("Scan flagged 1 endpoint")
                .scanTimestamp(ZonedDateTime.now())
                .build());

        secscanFlaggedEndpointRepository.saveAll(List.of(
                SecscanFlaggedEndpoint.builder()
                        .systemComponentId(component.getId())
                        .path("/api/unprotected")
                        .method("GET")
                        .scanMessage("HTTP 200 returned")
                        .scanTimestamp(ZonedDateTime.now())
                        .build()
        ));

        ruleEvaluationService.updateRuleStatesForComponent(component);

        var ruleState = ruleStateRepository.findBySystemComponentAndRuleId(component, RULE_ID);
        assertThat(ruleState).isPresent();
        assertThat(ruleState.get().getState()).isEqualTo(State.OK);
    }

    @Test
    void mixedExemptAndNonExemptEndpoints_resultsInFail() {
        var system = systemRepository.add(System.builder()
                .name("secscanmixedsys")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("secscanmixedsys-app-service").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        var component = system.getSystemComponents().getFirst();

        secscanStateRepository.save(SecscanState.builder()
                .systemComponentId(component.getId())
                .scanMessage("Scan flagged 3 endpoints")
                .scanTimestamp(ZonedDateTime.now())
                .build());

        secscanFlaggedEndpointRepository.saveAll(List.of(
                // This one is exempt by default (actuator)
                SecscanFlaggedEndpoint.builder()
                        .systemComponentId(component.getId())
                        .path("/actuator/health")
                        .method("GET")
                        .scanMessage("HTTP 200 returned")
                        .scanTimestamp(ZonedDateTime.now())
                        .build(),
                // This one is exempt by parameter (exempt-methods: OPTIONS)
                SecscanFlaggedEndpoint.builder()
                        .systemComponentId(component.getId())
                        .path("/api/preflight")
                        .method("OPTIONS")
                        .scanMessage("HTTP 200 returned")
                        .scanTimestamp(ZonedDateTime.now())
                        .build(),
                // This one is NOT exempt — should cause FAIL
                SecscanFlaggedEndpoint.builder()
                        .systemComponentId(component.getId())
                        .path("/api/unprotected")
                        .method("GET")
                        .scanMessage("HTTP 200 returned")
                        .scanTimestamp(ZonedDateTime.now())
                        .build()
        ));

        ruleEvaluationService.updateRuleStatesForComponent(component);

        var ruleState = ruleStateRepository.findBySystemComponentAndRuleId(component, RULE_ID);
        assertThat(ruleState).isPresent();
        assertThat(ruleState.get().getState()).isEqualTo(State.FAIL);
        assertThat(ruleState.get().getStateComment()).contains("GET /api/unprotected");
        // The exempt endpoints should NOT appear in the state comment
        assertThat(ruleState.get().getStateComment()).doesNotContain("/actuator/health");
        assertThat(ruleState.get().getStateComment()).doesNotContain("/api/preflight");
    }

    @Test
    void exemptedComponent_resultsInDisabled() {
        var system = systemRepository.add(System.builder()
                .name("secscanexemptedcompsys")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("secscanexemptedcomp-app-service").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        var component = system.getSystemComponents().getFirst();

        // Insert scan state and a flagged endpoint — should still result in DISABLED due to exemption
        secscanStateRepository.save(SecscanState.builder()
                .systemComponentId(component.getId())
                .scanMessage("Scan flagged 1 endpoint")
                .scanTimestamp(ZonedDateTime.now())
                .build());

        secscanFlaggedEndpointRepository.saveAll(List.of(
                SecscanFlaggedEndpoint.builder()
                        .systemComponentId(component.getId())
                        .path("/api/unprotected")
                        .method("GET")
                        .scanMessage("HTTP 200 returned without JWT")
                        .scanTimestamp(ZonedDateTime.now())
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
                .name("secscanexempteduntilcompsys")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("secscanexempteduntilcomp-app-service").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        var component = system.getSystemComponents().getFirst();

        // Insert scan state and a flagged endpoint — should still result in PAUSED due to temporary exemption
        secscanStateRepository.save(SecscanState.builder()
                .systemComponentId(component.getId())
                .scanMessage("Scan flagged 1 endpoint")
                .scanTimestamp(ZonedDateTime.now())
                .build());

        secscanFlaggedEndpointRepository.saveAll(List.of(
                SecscanFlaggedEndpoint.builder()
                        .systemComponentId(component.getId())
                        .path("/api/unprotected")
                        .method("GET")
                        .scanMessage("HTTP 200 returned without JWT")
                        .scanTimestamp(ZonedDateTime.now())
                        .build()
        ));

        ruleEvaluationService.updateRuleStatesForComponent(component);

        var ruleState = ruleStateRepository.findBySystemComponentAndRuleId(component, RULE_ID);
        assertThat(ruleState).isPresent();
        assertThat(ruleState.get().getState()).isEqualTo(State.PAUSED);
    }

    @Test
    void endpointIgnoredByComponentExemptPaths_resultsInOk() {
        var system = systemRepository.add(System.builder()
                .name("secscanexemptedcompparamsys")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("secscanexemptedcompparam-app-service").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        var component = system.getSystemComponents().getFirst();

        secscanStateRepository.save(SecscanState.builder()
                .systemComponentId(component.getId())
                .scanMessage("Scan flagged 2 endpoints")
                .scanTimestamp(ZonedDateTime.now())
                .build());

        secscanFlaggedEndpointRepository.saveAll(List.of(
                SecscanFlaggedEndpoint.builder()
                        .systemComponentId(component.getId())
                        .path("/api/safeplay/notify/gugu")
                        .method("GET")
                        .scanMessage("HTTP 200 returned")
                        .scanTimestamp(ZonedDateTime.now())
                        .build(),
                SecscanFlaggedEndpoint.builder()
                        .systemComponentId(component.getId())
                        .path("/api/safeplay/notify/juhu")
                        .method("GET")
                        .scanMessage("HTTP 200 returned")
                        .scanTimestamp(ZonedDateTime.now())
                        .build()
        ));

        ruleEvaluationService.updateRuleStatesForComponent(component);

        var ruleState = ruleStateRepository.findBySystemComponentAndRuleId(component, RULE_ID);
        assertThat(ruleState).isPresent();
        assertThat(ruleState.get().getState()).isEqualTo(State.DISABLED);
    }
}
