package ch.admin.bit.jeap.governance.persistence;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.rule.NonCompliantComponentEntry;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import ch.admin.bit.jeap.governance.domain.rule.RuleState;
import ch.admin.bit.jeap.governance.domain.rule.State;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void violationDetectionTime_survivesPersistenceRoundTrip() {
        SystemComponent component = createAndPersistSystemWithComponent();
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime detectedAt = now.minusDays(3);
        entityManager.persist(RuleState.createWithTimestamps(
                RuleId.of("RULE-001"), component, State.OK, now, now, detectedAt));
        entityManager.flush();
        entityManager.clear();

        Optional<RuleState> result = repository.findBySystemComponentAndRuleId(component, RuleId.of("RULE-001"));

        assertThat(result).isPresent()
                .hasValueSatisfying(ruleState -> assertThat(ruleState.getViolationDetectedAt()
                        .truncatedTo(ChronoUnit.MILLIS).toInstant())
                        .isEqualTo(detectedAt.truncatedTo(ChronoUnit.MILLIS).toInstant()));
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

    @Test
    void findAll() {
        SystemComponent component = createAndPersistSystemWithComponent();

        // Persist an existing (managed) entity
        entityManager.persist(RuleState.builder()
                .ruleId(RuleId.of("RULE-001")).systemComponent(component)
                .state(State.OK).build());
        entityManager.flush();

        List<RuleState> allRuleStates = repository.findAll();
        assertThat(allRuleStates).hasSize(1);
    }

    @Test
    void findAll_severalEntries() {
        SystemComponent component1 = createAndPersistSystemWithComponent("System 1", "Component 1");
        SystemComponent component2 = createAndPersistSystemWithComponent("System 2", "Component 2");

        // Persist an existing (managed) entity
        entityManager.persist(RuleState.builder()
                .ruleId(RuleId.of("RULE-001")).systemComponent(component1)
                .state(State.OK).build());
        entityManager.persist(RuleState.builder()
                .ruleId(RuleId.of("RULE-002")).systemComponent(component1)
                .state(State.OK).build());
        entityManager.persist(RuleState.builder()
                .ruleId(RuleId.of("RULE-001")).systemComponent(component2)
                .state(State.OK).build());
        entityManager.flush();

        List<RuleState> allRuleStates = repository.findAll();
        assertThat(allRuleStates).hasSize(3);
    }

    @Test
    void findNonCompliantSince() {
        ZonedDateTime timestamp1 = ZonedDateTime.now().minusDays(10);
        ZonedDateTime timestamp2 = ZonedDateTime.now().minusDays(9);
        ZonedDateTime timestamp3 = ZonedDateTime.now().minusDays(8);
        SystemComponent component1 = createAndPersistSystemWithComponent("System 1", "Component 1");
        SystemComponent component2 = createAndPersistSystemWithComponent("System 2", "Component 2");

        entityManager.persist(RuleState.createWithTimestamps(RuleId.of("RULE-001"), component1, State.OK, ZonedDateTime.now(), timestamp1));
        entityManager.persist(RuleState.createWithTimestamps(RuleId.of("RULE-002"), component1, State.FAIL, ZonedDateTime.now(), timestamp2));
        entityManager.persist(RuleState.createWithTimestamps(RuleId.of("RULE-003"), component1, State.PAUSED, ZonedDateTime.now(), timestamp3));
        entityManager.persist(RuleState.createWithTimestamps(RuleId.of("RULE-001"), component2, State.DISABLED, ZonedDateTime.now(), timestamp1));
        entityManager.persist(RuleState.createWithTimestamps(RuleId.of("RULE-002"), component2, State.FAIL, ZonedDateTime.now(), timestamp2));
        entityManager.persist(RuleState.createWithTimestamps(RuleId.of("RULE-003"), component2, State.OK, ZonedDateTime.now(), timestamp3));


        entityManager.flush();

        List<NonCompliantComponentEntry> result = repository.findNonCompliantSince();
        assertThat(result).hasSize(2);
        for (NonCompliantComponentEntry entry : result) {
            assertNotNull(entry.getSystemId());
            assertNotNull(entry.getSystemComponentId());
            assertNotNull(entry.getSystemComponentName());
            assertEquals("RULE-002", entry.getRuleId());
            assertEquals(timestamp2.truncatedTo(ChronoUnit.MILLIS).toInstant(), entry.getNonCompliantSince().truncatedTo(ChronoUnit.MILLIS).toInstant());
        }
    }

    @Test
    void deleteAllBySystemComponentId() {
        SystemComponent component1 = createAndPersistSystemWithComponent("System 1", "Component 1");

        // Persist an existing (managed) entity
        entityManager.persist(RuleState.builder()
                .ruleId(RuleId.of("RULE-001")).systemComponent(component1)
                .state(State.OK).build());
        entityManager.persist(RuleState.builder()
                .ruleId(RuleId.of("RULE-002")).systemComponent(component1)
                .state(State.OK).build());
        entityManager.flush();
        entityManager.clear();

        List<RuleState> results = entityManager.getEntityManager()
                .createQuery("SELECT rs FROM RuleState rs", RuleState.class)
                .getResultList();
        assertEquals(2, results.size());

        repository.deleteAllBySystemComponentId(component1.getId());
        entityManager.flush();
        entityManager.clear();

        results = entityManager.getEntityManager()
                .createQuery("SELECT rs FROM RuleState rs", RuleState.class)
                .getResultList();
        assertTrue(results.isEmpty());
    }

    @Test
    void deleteAllBySystemComponentId_shouldNotAffectOtherComponentRuleStates() {
        SystemComponent component1 = createAndPersistSystemWithComponent("System 1", "Component 1");
        SystemComponent component2 = createAndPersistSystemWithComponent("System 2", "Component 2");

        // Persist an existing (managed) entity
        entityManager.persist(RuleState.builder()
                .ruleId(RuleId.of("RULE-001")).systemComponent(component1)
                .state(State.OK).build());
        entityManager.persist(RuleState.builder()
                .ruleId(RuleId.of("RULE-002")).systemComponent(component1)
                .state(State.OK).build());
        entityManager.persist(RuleState.builder()
                .ruleId(RuleId.of("RULE-001")).systemComponent(component2)
                .state(State.OK).build());
        entityManager.flush();
        entityManager.clear();

        List<RuleState> results = entityManager.getEntityManager()
                .createQuery("SELECT rs FROM RuleState rs", RuleState.class)
                .getResultList();
        assertEquals(3, results.size());

        repository.deleteAllBySystemComponentId(component1.getId());
        entityManager.flush();
        entityManager.clear();

        results = entityManager.getEntityManager()
                .createQuery("SELECT rs FROM RuleState rs", RuleState.class)
                .getResultList();
        assertEquals(1, results.size());
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
