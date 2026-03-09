package ch.admin.bit.jeap.governance.persistence;

import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.score.SystemScore;
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
@Import(SystemScoreRepositoryImpl.class)
class SystemScoreRepositoryImplTest extends PostgresTestContainerBase {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SystemScoreRepositoryImpl repository;

    @Test
    void save_shouldPersistSystemScore() {
        System system = createAndPersistSystem("Test System");
        LocalDate day = LocalDate.of(2026, 1, 15);
        SystemScore score = SystemScore.builder()
                .system(system)
                .score(85)
                .day(day)
                .build();

        SystemScore saved = repository.save(score);
        entityManager.flush();

        assertNotNull(saved.getId());
        assertEquals(85, saved.getScore());
        assertEquals(system, saved.getSystem());
    }

    @Test
    void save_shouldReplaceExistingScoreForSameSystemAndDay() {
        System system = createAndPersistSystem("Test System");
        LocalDate day = LocalDate.of(2026, 1, 15);
        entityManager.persist(SystemScore.builder().system(system).score(70).day(day).build());
        entityManager.flush();
        entityManager.clear();

        SystemScore replacement = SystemScore.builder().system(system).score(95).day(day).build();
        repository.save(replacement);
        entityManager.flush();
        entityManager.clear();

        List<SystemScore> scores = repository.findBySystem(system);
        assertEquals(1, scores.size());
        assertEquals(95, scores.getFirst().getScore());
    }

    @Test
    void save_shouldNotAffectOtherDays() {
        System system = createAndPersistSystem("Test System");
        LocalDate day1 = LocalDate.of(2026, 1, 15);
        LocalDate day2 = LocalDate.of(2026, 1, 16);
        entityManager.persist(SystemScore.builder().system(system).score(70).day(day1).build());
        entityManager.persist(SystemScore.builder().system(system).score(80).day(day2).build());
        entityManager.flush();
        entityManager.clear();

        repository.save(SystemScore.builder().system(system).score(99).day(day1).build());
        entityManager.flush();
        entityManager.clear();

        assertEquals(99, repository.findBySystemAndDay(system, day1).get().getScore());
        assertEquals(80, repository.findBySystemAndDay(system, day2).get().getScore());
    }

    @Test
    void findBySystemAndDay_shouldReturnScore_whenExists() {
        System system = createAndPersistSystem("Test System");
        LocalDate day = LocalDate.of(2026, 1, 15);
        SystemScore score = SystemScore.builder()
                .system(system)
                .score(90)
                .day(day)
                .build();
        entityManager.persist(score);
        entityManager.flush();

        Optional<SystemScore> result = repository.findBySystemAndDay(system, day);

        assertTrue(result.isPresent());
        assertEquals(90, result.get().getScore());
    }

    @Test
    void findBySystemAndDay_shouldReturnEmpty_whenNotExists() {
        System system = createAndPersistSystem("Test System");

        Optional<SystemScore> result = repository.findBySystemAndDay(system, LocalDate.of(2026, 1, 15));

        assertTrue(result.isEmpty());
    }

    @Test
    void findBySystem_shouldReturnAllScores() {
        System system = createAndPersistSystem("Test System");
        entityManager.persist(SystemScore.builder()
                .system(system).score(80).day(LocalDate.of(2026, 1, 1)).build());
        entityManager.persist(SystemScore.builder()
                .system(system).score(90).day(LocalDate.of(2026, 1, 2)).build());
        entityManager.flush();

        List<SystemScore> result = repository.findBySystem(system);

        assertEquals(2, result.size());
    }

    @Test
    void findAllByDayBetweenInclusive_shouldReturnAllScoresWithinTimeframe() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);
        System system1 = createAndPersistSystem("Test System 1");
        entityManager.persist(SystemScore.builder()
                .system(system1).score(77).day(LocalDate.of(2025, 12, 31)).build());
        entityManager.persist(SystemScore.builder()
                .system(system1).score(80).day(LocalDate.of(2026, 1, 1)).build());
        entityManager.persist(SystemScore.builder()
                .system(system1).score(90).day(LocalDate.of(2026, 1, 2)).build());
        entityManager.persist(SystemScore.builder()
                .system(system1).score(90).day(LocalDate.of(2026, 2, 1)).build());
        entityManager.flush();

        List<SystemScore> result = repository.findAllByDayBetweenInclusive(from, to);

        assertEquals(2, result.size());
    }


    @Test
    void findAllByDayBetweenInclusive_shouldReturnAllScoresWithinTimeframe_severalSystems() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);
        System system1 = createAndPersistSystem("Test System 1");
        System system2 = createAndPersistSystem("Test System 2");
        entityManager.persist(SystemScore.builder()
                .system(system1).score(77).day(LocalDate.of(2025, 12, 31)).build());
        entityManager.persist(SystemScore.builder()
                .system(system1).score(80).day(LocalDate.of(2026, 1, 1)).build());
        entityManager.persist(SystemScore.builder()
                .system(system1).score(90).day(LocalDate.of(2026, 1, 2)).build());
        entityManager.persist(SystemScore.builder()
                .system(system1).score(90).day(LocalDate.of(2026, 2, 1)).build());
        entityManager.persist(SystemScore.builder()
                .system(system2).score(82).day(LocalDate.of(2026, 1, 1)).build());
        entityManager.persist(SystemScore.builder()
                .system(system2).score(82).day(LocalDate.of(2026, 2, 3)).build());
        entityManager.flush();

        List<SystemScore> result = repository.findAllByDayBetweenInclusive(from, to);

        assertEquals(3, result.size());
    }

    @Test
    void findAllByDayBetweenInclusive_shouldReturnEmpty_whneNoneExist() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);

        List<SystemScore> result = repository.findAllByDayBetweenInclusive(from, to);

        assertTrue(result.isEmpty());
    }

    @Test
    void deleteAllBySystemId() {
        System system1 = createAndPersistSystem("Test System 1");
        entityManager.persist(SystemScore.builder()
                .system(system1).score(77).day(LocalDate.of(2025, 12, 31)).build());
        entityManager.persist(SystemScore.builder()
                .system(system1).score(80).day(LocalDate.of(2026, 1, 1)).build());
        entityManager.persist(SystemScore.builder()
                .system(system1).score(90).day(LocalDate.of(2026, 1, 2)).build());
        entityManager.persist(SystemScore.builder()
                .system(system1).score(90).day(LocalDate.of(2026, 2, 1)).build());
        entityManager.flush();

        List<SystemScore> results = entityManager.getEntityManager().createQuery("SELECT s FROM SystemScore s", SystemScore.class)
                .getResultList();

        assertEquals(4, results.size());

        repository.deleteAllBySystemId(system1.getId());
        entityManager.flush();
        entityManager.clear();

        results = entityManager.getEntityManager().createQuery("SELECT s FROM SystemScore s", SystemScore.class)
                .getResultList();

        assertTrue(results.isEmpty());
    }

    @Test
    void deleteAllBySystemId_shouldNotAffectOtherSystemScores() {
        System system1 = createAndPersistSystem("Test System 1");
        System system2 = createAndPersistSystem("Test System 2");
        entityManager.persist(SystemScore.builder()
                .system(system1).score(77).day(LocalDate.of(2025, 12, 31)).build());
        entityManager.persist(SystemScore.builder()
                .system(system1).score(80).day(LocalDate.of(2026, 1, 1)).build());
        entityManager.persist(SystemScore.builder()
                .system(system1).score(90).day(LocalDate.of(2026, 1, 2)).build());
        entityManager.persist(SystemScore.builder()
                .system(system1).score(90).day(LocalDate.of(2026, 2, 1)).build());
        entityManager.persist(SystemScore.builder()
                .system(system2).score(82).day(LocalDate.of(2026, 1, 1)).build());
        entityManager.persist(SystemScore.builder()
                .system(system2).score(82).day(LocalDate.of(2026, 2, 3)).build());
        entityManager.flush();

        List<SystemScore> results = entityManager.getEntityManager().createQuery("SELECT s FROM SystemScore s", SystemScore.class)
                .getResultList();

        assertEquals(6, results.size());

        repository.deleteAllBySystemId(system1.getId());
        entityManager.flush();
        entityManager.clear();

        results = entityManager.getEntityManager().createQuery("SELECT s FROM SystemScore s", SystemScore.class)
                .getResultList();

        assertEquals(2, results.size());
    }

    private System createAndPersistSystem(String name) {
        System system = System.builder()
                .name(name)
                .systemComponents(List.of())
                .aliases(Set.of())
                .build();
        entityManager.persist(system);
        entityManager.flush();
        return system;
    }
}
