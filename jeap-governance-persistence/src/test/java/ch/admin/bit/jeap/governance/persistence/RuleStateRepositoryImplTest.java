package ch.admin.bit.jeap.governance.persistence;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import ch.admin.bit.jeap.governance.domain.rule.RuleState;
import ch.admin.bit.jeap.governance.domain.rule.State;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RuleStateRepositoryImpl.class)
class RuleStateRepositoryImplTest extends PostgresTestContainerBase {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RuleStateRepositoryImpl repository;

    @Test
    void findBySystemComponentAndRuleId_shouldReturnRuleState_whenExists() {
        SystemComponent component = createAndPersistSystemWithComponent();
        entityManager.persist(RuleState.builder()
                .ruleId(RuleId.of("RULE-001")).systemComponent(component).state(State.OK).build());
        entityManager.persist(RuleState.builder()
                .ruleId(RuleId.of("RULE-002")).systemComponent(component).state(State.FAIL).build());
        entityManager.flush();

        Optional<RuleState> result = repository.findBySystemComponentAndRuleId(component, RuleId.of("RULE-001"));

        assertThat(result).isPresent();
        assertThat(result.get().getState()).isEqualTo(State.OK);
    }

    @Test
    void findBySystemComponentAndRuleId_shouldReturnEmpty_whenNotExists() {
        SystemComponent component = createAndPersistSystemWithComponent();
        entityManager.persist(RuleState.builder()
                .ruleId(RuleId.of("RULE-001")).systemComponent(component).state(State.OK).build());
        entityManager.flush();

        Optional<RuleState> result = repository.findBySystemComponentAndRuleId(component, RuleId.of("RULE-999"));

        assertThat(result).isEmpty();
    }

    @Test
    void findBySystemComponentAndRuleId_shouldNotReturnStateFromOtherComponent() {
        SystemComponent componentA = createAndPersistSystemWithComponent("System A", "Component A");
        SystemComponent componentB = createAndPersistSystemWithComponent("System B", "Component B");
        entityManager.persist(RuleState.builder()
                .ruleId(RuleId.of("RULE-001")).systemComponent(componentA).state(State.OK).build());
        entityManager.flush();

        Optional<RuleState> result = repository.findBySystemComponentAndRuleId(componentB, RuleId.of("RULE-001"));

        assertThat(result).isEmpty();
    }

    @Test
    void saveAll_shouldPersistMultipleRuleStates() {
        SystemComponent component = createAndPersistSystemWithComponent();
        List<RuleState> ruleStates = List.of(
                RuleState.builder().ruleId(RuleId.of("RULE-001")).systemComponent(component)
                        .state(State.OK).build(),
                RuleState.builder().ruleId(RuleId.of("RULE-002")).systemComponent(component)
                        .state(State.FAIL).build()
        );

        repository.saveAll(ruleStates);
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findBySystemComponentAndRuleId(component, RuleId.of("RULE-001"))).isPresent();
        assertThat(repository.findBySystemComponentAndRuleId(component, RuleId.of("RULE-002"))).isPresent();
    }

    @Test
    void saveAll_shouldSaveManagedAndNewEntityTogether() {
        SystemComponent component = createAndPersistSystemWithComponent();

        // Persist an existing (managed) entity
        RuleState existing = entityManager.persist(RuleState.builder()
                .ruleId(RuleId.of("RULE-001")).systemComponent(component)
                .state(State.OK).build());
        entityManager.flush();

        // Create a new (transient) entity
        RuleState newRuleState = RuleState.builder()
                .ruleId(RuleId.of("RULE-002")).systemComponent(component)
                .state(State.FAIL).build();

        // Save both together
        repository.saveAll(List.of(existing, newRuleState));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findBySystemComponentAndRuleId(component, RuleId.of("RULE-001"))).isPresent();
        assertThat(repository.findBySystemComponentAndRuleId(component, RuleId.of("RULE-002"))).isPresent();
    }

    private SystemComponent createAndPersistSystemWithComponent() {
        return createAndPersistSystemWithComponent("Test System", "Test Component");
    }

    private SystemComponent createAndPersistSystemWithComponent(String systemName, String componentName) {
        SystemComponent component = SystemComponent.builder()
                .name(componentName)
                .type(ComponentType.BACKEND_SERVICE)
                .build();
        System system = System.builder()
                .name(systemName)
                .systemComponents(List.of(component))
                .aliases(Set.of())
                .build();
        entityManager.persist(system);
        entityManager.flush();
        return component;
    }
}
