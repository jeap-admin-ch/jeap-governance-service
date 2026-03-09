package ch.admin.bit.jeap.governance.persistence;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.score.ComponentScore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ComponentScoreRepositoryImpl.class)
class ComponentScoreRepositoryImplTest extends PostgresTestContainerBase {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ComponentScoreRepositoryImpl repository;

    @Test
    void save_shouldPersistComponentScore() {
        SystemComponent component = createAndPersistSystemWithComponent();
        LocalDate day = LocalDate.of(2026, 1, 15);
        ComponentScore score = ComponentScore.builder()
                .systemComponent(component)
                .score(75)
                .day(day)
                .build();

        ComponentScore saved = repository.save(score);
        entityManager.flush();

        assertNotNull(saved.getId());
        assertEquals(75, saved.getScore());
        assertEquals(component, saved.getSystemComponent());
    }

    @Test
    void findBySystemComponentAndDay_shouldReturnScore_whenExists() {
        SystemComponent component = createAndPersistSystemWithComponent();
        LocalDate day = LocalDate.of(2026, 1, 15);
        ComponentScore score = ComponentScore.builder()
                .systemComponent(component)
                .score(88)
                .day(day)
                .build();
        entityManager.persist(score);
        entityManager.flush();

        Optional<ComponentScore> result = repository.findBySystemComponentAndDay(component, day);

        assertTrue(result.isPresent());
        assertEquals(88, result.get().getScore());
    }

    @Test
    void findBySystemComponentAndDay_shouldReturnEmpty_whenNotExists() {
        SystemComponent component = createAndPersistSystemWithComponent();

        Optional<ComponentScore> result = repository.findBySystemComponentAndDay(component, LocalDate.of(2026, 1, 15));

        assertTrue(result.isEmpty());
    }

    @Test
    void findBySystemComponent_shouldReturnAllScores() {
        SystemComponent component = createAndPersistSystemWithComponent();
        entityManager.persist(ComponentScore.builder()
                .systemComponent(component).score(70).day(LocalDate.of(2026, 1, 1)).build());
        entityManager.persist(ComponentScore.builder()
                .systemComponent(component).score(80).day(LocalDate.of(2026, 1, 2)).build());
        entityManager.flush();

        List<ComponentScore> result = repository.findBySystemComponent(component);

        assertEquals(2, result.size());
    }

    @Test
    void saveOrReplaceAllForSystemAndDay_shouldReplaceExistingScores() {
        System system = createAndPersistSystem("Test System", "Component A", "Component B");
        SystemComponent componentA = system.getSystemComponents().get(0);
        SystemComponent componentB = system.getSystemComponents().get(1);
        LocalDate day = LocalDate.of(2026, 1, 15);

        // Persist initial scores
        entityManager.persist(ComponentScore.builder().systemComponent(componentA).score(50).day(day).build());
        entityManager.persist(ComponentScore.builder().systemComponent(componentB).score(60).day(day).build());
        entityManager.flush();
        entityManager.clear();

        // Replace with new scores
        List<ComponentScore> newScores = List.of(
                ComponentScore.builder().systemComponent(componentA).score(80).day(day).build(),
                ComponentScore.builder().systemComponent(componentB).score(90).day(day).build()
        );
        repository.saveOrReplaceAllForSystemAndDay(system, newScores, day);
        entityManager.flush();
        entityManager.clear();

        Optional<ComponentScore> scoreA = repository.findBySystemComponentAndDay(componentA, day);
        Optional<ComponentScore> scoreB = repository.findBySystemComponentAndDay(componentB, day);
        assertTrue(scoreA.isPresent());
        assertTrue(scoreB.isPresent());
        assertEquals(80, scoreA.get().getScore());
        assertEquals(90, scoreB.get().getScore());
    }

    @Test
    void saveOrReplaceAllForSystemAndDay_shouldNotAffectOtherDays() {
        System system = createAndPersistSystem("Test System", "Component A");
        SystemComponent component = system.getSystemComponents().getFirst();
        LocalDate day1 = LocalDate.of(2026, 1, 15);
        LocalDate day2 = LocalDate.of(2026, 1, 16);

        entityManager.persist(ComponentScore.builder().systemComponent(component).score(50).day(day1).build());
        entityManager.persist(ComponentScore.builder().systemComponent(component).score(60).day(day2).build());
        entityManager.flush();
        entityManager.clear();

        List<ComponentScore> newScores = List.of(
                ComponentScore.builder().systemComponent(component).score(99).day(day1).build()
        );
        repository.saveOrReplaceAllForSystemAndDay(system, newScores, day1);
        entityManager.flush();
        entityManager.clear();

        assertEquals(99, repository.findBySystemComponentAndDay(component, day1).get().getScore());
        assertEquals(60, repository.findBySystemComponentAndDay(component, day2).get().getScore());
    }

    @Test
    void saveOrReplaceAllForSystemAndDay_shouldNotAffectOtherSystems() {
        System systemA = createAndPersistSystem("System A", "Component A");
        System systemB = createAndPersistSystem("System B", "Component B");
        SystemComponent componentA = systemA.getSystemComponents().getFirst();
        SystemComponent componentB = systemB.getSystemComponents().getFirst();
        LocalDate day = LocalDate.of(2026, 1, 15);

        entityManager.persist(ComponentScore.builder().systemComponent(componentA).score(50).day(day).build());
        entityManager.persist(ComponentScore.builder().systemComponent(componentB).score(60).day(day).build());
        entityManager.flush();
        entityManager.clear();

        List<ComponentScore> newScores = List.of(
                ComponentScore.builder().systemComponent(componentA).score(99).day(day).build()
        );
        repository.saveOrReplaceAllForSystemAndDay(systemA, newScores, day);
        entityManager.flush();
        entityManager.clear();

        assertEquals(99, repository.findBySystemComponentAndDay(componentA, day).get().getScore());
        assertEquals(60, repository.findBySystemComponentAndDay(componentB, day).get().getScore());
    }

    @Test
    void deleteAllBySystemComponentId() {
        System systemA = createAndPersistSystem("System A", "Component A");
        SystemComponent componentA = systemA.getSystemComponents().getFirst();
        LocalDate day = LocalDate.of(2026, 1, 15);

        entityManager.persist(ComponentScore.builder().systemComponent(componentA).score(50).day(day).build());
        entityManager.persist(ComponentScore.builder().systemComponent(componentA).score(60).day(day.minusDays(1)).build());
        entityManager.persist(ComponentScore.builder().systemComponent(componentA).score(70).day(day.minusDays(2)).build());
        entityManager.flush();
        entityManager.clear();

        List<ComponentScore> results = entityManager.getEntityManager()
                .createQuery("SELECT cs FROM ComponentScore cs", ComponentScore.class)
                .getResultList();
        assertEquals(3, results.size());

        repository.deleteAllBySystemComponentId(componentA.getId());
        entityManager.flush();
        entityManager.clear();

        results = entityManager.getEntityManager()
                .createQuery("SELECT cs FROM ComponentScore cs", ComponentScore.class)
                .getResultList();
        assertTrue(results.isEmpty());
    }

    @Test
    void deleteAllBySystemComponentId_shouldNotAffectOtherComponentScores() {
        System systemA = createAndPersistSystem("System A", "Component A");
        System systemB = createAndPersistSystem("System B", "Component B");
        SystemComponent componentA = systemA.getSystemComponents().getFirst();
        SystemComponent componentB = systemB.getSystemComponents().getFirst();
        LocalDate day = LocalDate.of(2026, 1, 15);

        entityManager.persist(ComponentScore.builder().systemComponent(componentA).score(50).day(day).build());
        entityManager.persist(ComponentScore.builder().systemComponent(componentA).score(60).day(day.minusDays(1)).build());
        entityManager.persist(ComponentScore.builder().systemComponent(componentB).score(60).day(day).build());
        entityManager.flush();
        entityManager.clear();

        List<ComponentScore> results = entityManager.getEntityManager()
                .createQuery("SELECT cs FROM ComponentScore cs", ComponentScore.class)
                .getResultList();
        assertEquals(3, results.size());

        repository.deleteAllBySystemComponentId(componentA.getId());
        entityManager.flush();
        entityManager.clear();

        results = entityManager.getEntityManager()
                .createQuery("SELECT cs FROM ComponentScore cs", ComponentScore.class)
                .getResultList();
        assertEquals(1, results.size());
    }

    private SystemComponent createAndPersistSystemWithComponent() {
        System system = createAndPersistSystem("Test System", "Test Component");
        return system.getSystemComponents().getFirst();
    }

    private System createAndPersistSystem(String systemName, String... componentNames) {
        List<SystemComponent> components = java.util.Arrays.stream(componentNames)
                .map(name -> SystemComponent.builder()
                        .name(name)
                        .type(ComponentType.BACKEND_SERVICE)
                        .build())
                .toList();
        System system = System.builder()
                .name(systemName)
                .systemComponents(components)
                .aliases(Set.of())
                .build();
        entityManager.persist(system);
        entityManager.flush();
        return system;
    }
}
