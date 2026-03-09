package ch.admin.bit.jeap.governance.persistence;

import ch.admin.bit.jeap.governance.domain.rule.SystemRuleConformanceRate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SystemRuleConformanceRateRepositoryImpl.class)
class SystemRuleConformanceRateRepositoryImplTest extends PostgresTestContainerBase {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SystemRuleConformanceRateRepositoryImpl repository;

    @Test
    void saveAll_shouldPersistRates() {
        var rate = SystemRuleConformanceRate.builder()
                .systemId(1L)
                .ruleId("rule-1")
                .conformanceRate(80)
                .day(LocalDate.of(2026, 1, 15))
                .build();

        repository.saveAll(List.of(rate));
        entityManager.flush();
        entityManager.clear();

        var found = repository.findBySystemIdAndDay(1L, LocalDate.of(2026, 1, 15));
        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getRuleId()).isEqualTo("rule-1");
        assertThat(found.getFirst().getConformanceRate()).isEqualTo(80);
        assertThat(found.getFirst().getSystemId()).isEqualTo(1L);
        assertThat(found.getFirst().getId()).isNotNull();
    }

    @Test
    void findBySystemIdAndDay_shouldReturnOnlyMatchingEntries() {
        entityManager.persist(SystemRuleConformanceRate.builder()
                .systemId(1L).ruleId("rule-1").conformanceRate(90).day(LocalDate.of(2026, 1, 15)).build());
        entityManager.persist(SystemRuleConformanceRate.builder()
                .systemId(2L).ruleId("rule-1").conformanceRate(70).day(LocalDate.of(2026, 1, 15)).build());
        entityManager.persist(SystemRuleConformanceRate.builder()
                .systemId(1L).ruleId("rule-1").conformanceRate(80).day(LocalDate.of(2026, 1, 16)).build());
        entityManager.flush();
        entityManager.clear();

        var result = repository.findBySystemIdAndDay(1L, LocalDate.of(2026, 1, 15));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getConformanceRate()).isEqualTo(90);
    }

    @Test
    void deleteAllByDay_shouldDeleteOnlyMatchingDay() {
        entityManager.persist(SystemRuleConformanceRate.builder()
                .systemId(1L).ruleId("rule-1").conformanceRate(90).day(LocalDate.of(2026, 1, 15)).build());
        entityManager.persist(SystemRuleConformanceRate.builder()
                .systemId(1L).ruleId("rule-1").conformanceRate(80).day(LocalDate.of(2026, 1, 16)).build());
        entityManager.flush();
        entityManager.clear();

        repository.deleteAllByDay(LocalDate.of(2026, 1, 15));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findBySystemIdAndDay(1L, LocalDate.of(2026, 1, 15))).isEmpty();
        assertThat(repository.findBySystemIdAndDay(1L, LocalDate.of(2026, 1, 16))).hasSize(1);
    }

    @Test
    void findLatestPerRuleIdAndSystemId() {
        entityManager.persist(createRate(1L, "rule-1", 85, LocalDate.of(2026, 1, 15)));
        entityManager.persist(createRate(1L, "rule-1", 85, LocalDate.of(2026, 1, 16)));
        entityManager.persist(createRate(1L, "rule-1", 90, LocalDate.of(2026, 1, 17)));
        entityManager.flush();
        entityManager.clear();

        List<SystemRuleConformanceRate> result = repository.findLatestPerRuleIdAndSystemId();
        assertEquals(1, result.size());
        SystemRuleConformanceRate latestRate = result.getFirst();
        assertEquals(1L, latestRate.getSystemId());
        assertEquals("rule-1", latestRate.getRuleId());
        assertEquals(90, latestRate.getConformanceRate());
        assertEquals(LocalDate.of(2026, 1, 17), latestRate.getDay());
    }

    @Test
    void findLatestPerRuleIdAndSystemId_severalEntries() {
        entityManager.persist(createRate(1L, "rule-1", 85, LocalDate.of(2026, 1, 15)));
        entityManager.persist(createRate(1L, "rule-1", 85, LocalDate.of(2026, 1, 16)));
        entityManager.persist(createRate(1L, "rule-1", 90, LocalDate.of(2026, 1, 17)));
        entityManager.persist(createRate(1L, "rule-2", 85, LocalDate.of(2026, 1, 15)));
        entityManager.persist(createRate(1L, "rule-2", 85, LocalDate.of(2026, 1, 16)));
        entityManager.persist(createRate(1L, "rule-2", 90, LocalDate.of(2026, 1, 17)));

        entityManager.persist(createRate(2L, "rule-1", 85, LocalDate.of(2026, 1, 15)));
        entityManager.persist(createRate(2L, "rule-1", 85, LocalDate.of(2026, 1, 16)));
        entityManager.persist(createRate(2L, "rule-1", 90, LocalDate.of(2026, 1, 17)));
        entityManager.persist(createRate(2L, "rule-2", 85, LocalDate.of(2026, 1, 15)));
        entityManager.persist(createRate(2L, "rule-2", 85, LocalDate.of(2026, 1, 16)));
        entityManager.persist(createRate(2L, "rule-2", 90, LocalDate.of(2026, 1, 17)));
        entityManager.flush();
        entityManager.clear();

        List<SystemRuleConformanceRate> result = repository.findLatestPerRuleIdAndSystemId();
        assertEquals(4, result.size());
        for (SystemRuleConformanceRate rate : result) {
            assertEquals(90, rate.getConformanceRate());
            assertEquals(LocalDate.of(2026, 1, 17), rate.getDay());
        }
    }

    @Test
    void deleteAllBySystemId() {
        entityManager.persist(createRate(1L, "rule-1", 85, LocalDate.of(2026, 1, 15)));
        entityManager.persist(createRate(1L, "rule-1", 85, LocalDate.of(2026, 1, 16)));
        entityManager.flush();
        entityManager.clear();

        List<SystemRuleConformanceRate> results = entityManager.getEntityManager()
                .createQuery("SELECT s FROM SystemRuleConformanceRate s", SystemRuleConformanceRate.class)
                .getResultList();
        assertEquals(2, results.size());

        repository.deleteAllBySystemId(1L);
        entityManager.flush();
        entityManager.clear();

        results = entityManager.getEntityManager()
                .createQuery("SELECT s FROM SystemRuleConformanceRate s", SystemRuleConformanceRate.class)
                .getResultList();
        assertTrue(results.isEmpty());
    }

    @Test
    void deleteAllBySystemId_shouldNotAffectOtherSystemRates() {
        entityManager.persist(createRate(1L, "rule-1", 85, LocalDate.of(2026, 1, 15)));
        entityManager.persist(createRate(1L, "rule-1", 85, LocalDate.of(2026, 1, 16)));
        entityManager.persist(createRate(2L, "rule-1", 85, LocalDate.of(2026, 1, 16)));
        entityManager.flush();
        entityManager.clear();

        List<SystemRuleConformanceRate> results = entityManager.getEntityManager()
                .createQuery("SELECT s FROM SystemRuleConformanceRate s", SystemRuleConformanceRate.class)
                .getResultList();
        assertEquals(3, results.size());

        repository.deleteAllBySystemId(1L);
        entityManager.flush();
        entityManager.clear();

        results = entityManager.getEntityManager()
                .createQuery("SELECT s FROM SystemRuleConformanceRate s", SystemRuleConformanceRate.class)
                .getResultList();
        assertEquals(1, results.size());
    }

    private SystemRuleConformanceRate createRate(long systemId, String ruleId, int conformanceRate, LocalDate day) {
        return SystemRuleConformanceRate.builder()
                .systemId(systemId)
                .ruleId(ruleId)
                .conformanceRate(conformanceRate)
                .day(day)
                .createdAt(ZonedDateTime.of(day, java.time.LocalTime.NOON, java.time.ZoneId.systemDefault()))
                .build();
    }

}
