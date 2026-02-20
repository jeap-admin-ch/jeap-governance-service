package ch.admin.bit.jeap.governance.persistence;

import ch.admin.bit.jeap.governance.domain.rule.RuleConformanceRate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RuleConformanceRateRepositoryImpl.class)
class RuleConformanceRateRepositoryImplTest extends PostgresTestContainerBase {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RuleConformanceRateRepositoryImpl repository;

    @Test
    void save_shouldPersistRuleConformanceRate() {
        RuleConformanceRate rate = RuleConformanceRate.builder()
                .ruleId("RULE-001")
                .conformanceRate(95)
                .day(LocalDate.of(2026, 1, 15))
                .build();

        RuleConformanceRate saved = repository.save(rate);
        entityManager.flush();

        assertNotNull(saved.getId());
        assertEquals("RULE-001", saved.getRuleId());
        assertEquals(95, saved.getConformanceRate());
    }

    @Test
    void findByRuleIdAndDay_shouldReturnRate_whenExists() {
        LocalDate day = LocalDate.of(2026, 1, 15);
        entityManager.persist(RuleConformanceRate.builder()
                .ruleId("RULE-001").conformanceRate(92).day(day).build());
        entityManager.flush();

        Optional<RuleConformanceRate> result = repository.findByRuleIdAndDay("RULE-001", day);

        assertTrue(result.isPresent());
        assertEquals(92, result.get().getConformanceRate());
    }

    @Test
    void findByRuleIdAndDay_shouldReturnEmpty_whenNotExists() {
        Optional<RuleConformanceRate> result = repository.findByRuleIdAndDay("RULE-999", LocalDate.of(2026, 1, 15));

        assertTrue(result.isEmpty());
    }

    @Test
    void findByRuleId_shouldReturnAllRates() {
        entityManager.persist(RuleConformanceRate.builder()
                .ruleId("RULE-001").conformanceRate(90).day(LocalDate.of(2026, 1, 1)).build());
        entityManager.persist(RuleConformanceRate.builder()
                .ruleId("RULE-001").conformanceRate(95).day(LocalDate.of(2026, 1, 2)).build());
        entityManager.flush();

        List<RuleConformanceRate> result = repository.findByRuleId("RULE-001");

        assertEquals(2, result.size());
    }

    @Test
    void findAllByDayBetweenInclusive_oneRule() {
        LocalDate day1 = LocalDate.of(2026, 1, 1);
        LocalDate day2 = LocalDate.of(2026, 1, 2);
        LocalDate day3 = LocalDate.of(2026, 1, 3);
        LocalDate day4 = LocalDate.of(2026, 1, 4);
        entityManager.persist(createRate("RULE-001", 90, day1));
        entityManager.persist(createRate("RULE-001", 95, day2));
        entityManager.persist(createRate("RULE-001", 95, day3));
        entityManager.persist(createRate("RULE-001", 90, day4));
        entityManager.flush();

        List<RuleConformanceRate> result = repository.findAllByDayBetweenInclusive(day2, day3);
        assertEquals(2, result.size());
        for (RuleConformanceRate rate : result) {
            assertEquals(95, rate.getConformanceRate());
        }
    }

    @Test
    void findAllByDayBetweenInclusive_severalRules() {
        LocalDate day1 = LocalDate.of(2026, 1, 1);
        LocalDate day2 = LocalDate.of(2026, 1, 2);
        LocalDate day3 = LocalDate.of(2026, 1, 3);
        LocalDate day4 = LocalDate.of(2026, 1, 4);
        entityManager.persist(createRate("RULE-001", 90, day1));
        entityManager.persist(createRate("RULE-001", 95, day2));
        entityManager.persist(createRate("RULE-001", 95, day3));
        entityManager.persist(createRate("RULE-001", 90, day4));
        entityManager.persist(createRate("RULE-002", 90, day1));
        entityManager.persist(createRate("RULE-002", 95, day2));
        entityManager.persist(createRate("RULE-002", 95, day3));
        entityManager.persist(createRate("RULE-002", 90, day4));
        entityManager.persist(createRate("RULE-003", 90, day1));
        entityManager.persist(createRate("RULE-003", 95, day2));
        entityManager.persist(createRate("RULE-003", 95, day3));
        entityManager.persist(createRate("RULE-003", 90, day4));


        entityManager.flush();

        List<RuleConformanceRate> result = repository.findAllByDayBetweenInclusive(day2, day3);
        assertEquals(6, result.size());
        for (RuleConformanceRate rate : result) {
            assertEquals(95, rate.getConformanceRate());
        }
    }

    private RuleConformanceRate createRate(String ruleId, int conformanceRate, LocalDate day) {
        return RuleConformanceRate.builder()
                .ruleId(ruleId)
                .conformanceRate(conformanceRate)
                .day(day)
                .createdAt(ZonedDateTime.of(day, java.time.LocalTime.NOON, java.time.ZoneId.systemDefault()))
                .build();
    }
}
