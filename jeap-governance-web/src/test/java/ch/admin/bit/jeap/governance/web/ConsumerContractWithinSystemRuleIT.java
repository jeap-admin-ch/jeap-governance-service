package ch.admin.bit.jeap.governance.web;

import ch.admin.bit.jeap.governance.archrepo.domain.RestApiRelationWithoutPact;
import ch.admin.bit.jeap.governance.archrepo.domain.RestApiRelationWithoutPactRepository;
import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.SystemRepository;
import ch.admin.bit.jeap.governance.domain.rule.RuleEvaluationService;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import ch.admin.bit.jeap.governance.domain.rule.RuleStateRepository;
import ch.admin.bit.jeap.governance.domain.rule.State;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "jeap.governance.archrepo.import.restapirelationwithoutpact.enabled=true"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("component-cdc-contractwithinsystem-rule-it")
class ConsumerContractWithinSystemRuleIT extends GovernanceIntegrationTestBase {

    @Autowired
    private SystemRepository systemRepository;

    @Autowired
    private RuleEvaluationService ruleEvaluationService;

    @Autowired
    private RuleStateRepository ruleStateRepository;

    @Autowired
    private RestApiRelationWithoutPactRepository restApiRelationWithoutPactRepository;

    @Test
    void componentWithoutRestApiRelationWithoutPact_resultsInOk() {
        var system = systemRepository.add(System.builder()
                .name("testsys")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("service").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        var component = system.getSystemComponents().getFirst();

        ruleEvaluationService.updateRuleStatesForComponent(component);

        var ruleState = ruleStateRepository.findBySystemComponentAndRuleId(component, RuleId.of("component-cdc-contractwithinsystem"));
        assertThat(ruleState).isPresent();
        assertThat(ruleState.get().getState()).isEqualTo(State.OK);
    }

    @Test
    void componentWithRestApiRelationWithoutPact_resultsInFail() {
        var system = systemRepository.add(System.builder()
                .name("testsys1")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("service1").type(ComponentType.BACKEND_SERVICE).build(),
                        SystemComponent.builder().name("service2").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());


        var component = system.getSystemComponents().getFirst();
        var otherComponent = system.getSystemComponents().get(1);

        restApiRelationWithoutPactRepository.add(RestApiRelationWithoutPact.builder().providerSystemComponent(otherComponent).consumerSystemComponent(component).method("GET").path("/foo/bar").build());

        ruleEvaluationService.updateRuleStatesForComponent(component);

        var ruleState = ruleStateRepository.findBySystemComponentAndRuleId(component, RuleId.of("component-cdc-contractwithinsystem"));
        assertThat(ruleState).isPresent();
        assertThat(ruleState.get().getState()).isEqualTo(State.FAIL);
        assertThat(ruleState.get().getStateComment()).isEqualTo("No consumer contract found for relation with service2 on 'GET /foo/bar'");
    }

    @Test
    void componentWithRestApiRelationWithoutPact_relationsAndServicesIgnored_resultsInOk() {
        var system = systemRepository.add(System.builder()
                .name("testsys3")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("service3").type(ComponentType.BACKEND_SERVICE).build(),
                        SystemComponent.builder().name("service4").type(ComponentType.BACKEND_SERVICE).build(),
                        SystemComponent.builder().name("my-errorhandler-service").type(ComponentType.BACKEND_SERVICE).build(),
                        SystemComponent.builder().name("my-testagent-service").type(ComponentType.BACKEND_SERVICE).build(),
                        SystemComponent.builder().name("my-testar-orchestrator-service").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        var component = system.getSystemComponents().getFirst();
        var otherComponent = system.getSystemComponents().get(1);
        var otherComponent2 = system.getSystemComponents().get(2);
        var otherComponent3 = system.getSystemComponents().get(3);
        var otherComponent4 = system.getSystemComponents().get(4);

        restApiRelationWithoutPactRepository.add(RestApiRelationWithoutPact.builder().providerSystemComponent(otherComponent).consumerSystemComponent(component).method("POST").path("/api/dbschemas").build());
        restApiRelationWithoutPactRepository.add(RestApiRelationWithoutPact.builder().providerSystemComponent(otherComponent).consumerSystemComponent(component).method("POST").path("/api/openapi/{systemComponentName}").build());
        restApiRelationWithoutPactRepository.add(RestApiRelationWithoutPact.builder().providerSystemComponent(otherComponent2).consumerSystemComponent(component).method("GET").path("/foo/bar").build());
        restApiRelationWithoutPactRepository.add(RestApiRelationWithoutPact.builder().providerSystemComponent(otherComponent3).consumerSystemComponent(component).method("GET").path("/foo/bar").build());
        restApiRelationWithoutPactRepository.add(RestApiRelationWithoutPact.builder().providerSystemComponent(otherComponent4).consumerSystemComponent(component).method("GET").path("/foo/bar").build());

        ruleEvaluationService.updateRuleStatesForComponent(component);

        var ruleState = ruleStateRepository.findBySystemComponentAndRuleId(component, RuleId.of("component-cdc-contractwithinsystem"));
        assertThat(ruleState).isPresent();
        assertThat(ruleState.get().getState()).isEqualTo(State.OK);
    }
}
