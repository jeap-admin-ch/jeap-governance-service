package ch.admin.bit.jeap.governance.persistence;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.rule.RuleConformanceRate;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import ch.admin.bit.jeap.governance.domain.rule.RuleState;
import ch.admin.bit.jeap.governance.domain.rule.State;
import ch.admin.bit.jeap.governance.domain.score.ComponentScore;
import ch.admin.bit.jeap.governance.domain.score.SystemScore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(DatabaseHousekeepingService.class)
class DatabaseHousekeepingServiceTest extends PostgresTestContainerBase {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DatabaseHousekeepingService housekeepingService;

    @Autowired
    private JpaSystemScoreRepository jpaSystemScoreRepository;

    @Autowired
    private JpaComponentScoreRepository jpaComponentScoreRepository;

    @Autowired
    private JpaRuleConformanceRateRepository jpaRuleConformanceRateRepository;

    @Autowired
    private JpaRuleStateRepository jpaRuleStateRepository;

    private static final LocalDate TODAY = LocalDate.of(2026, 2, 17);
    private static final LocalDate OLD_DAY = LocalDate.of(2025, 1, 1);
    private static final int MAX_AGE_DAYS = 30;

    @Test
    void performHousekeeping_shouldDeleteOldSystemScores() {
        System system = createAndPersistSystem();
        LocalDate recentDay = TODAY.minusDays(MAX_AGE_DAYS - 1);

        entityManager.persist(SystemScore.builder().system(system).score(80).day(OLD_DAY).build());
        entityManager.persist(SystemScore.builder().system(system).score(90).day(recentDay).build());
        entityManager.flush();
        entityManager.clear();

        housekeepingService.performHousekeeping(MAX_AGE_DAYS);
        entityManager.flush();
        entityManager.clear();

        List<SystemScore> remaining = jpaSystemScoreRepository.findBySystem(system);
        assertThat(remaining).hasSize(1);
        assertThat(remaining.getFirst().getScore()).isEqualTo(90);
    }

    @Test
    void performHousekeeping_shouldDeleteOldComponentScores() {
        SystemComponent component = createAndPersistSystemWithComponent();
        LocalDate recentDay = TODAY.minusDays(MAX_AGE_DAYS - 1);

        entityManager.persist(ComponentScore.builder().systemComponent(component).score(70).day(OLD_DAY).build());
        entityManager.persist(ComponentScore.builder().systemComponent(component).score(85).day(recentDay).build());
        entityManager.flush();
        entityManager.clear();

        housekeepingService.performHousekeeping(MAX_AGE_DAYS);
        entityManager.flush();
        entityManager.clear();

        List<ComponentScore> remaining = jpaComponentScoreRepository.findBySystemComponent(component);
        assertThat(remaining).hasSize(1);
        assertThat(remaining.getFirst().getScore()).isEqualTo(85);
    }

    @Test
    void performHousekeeping_shouldDeleteOldRuleConformanceRates() {
        LocalDate recentDay = TODAY.minusDays(MAX_AGE_DAYS - 1);

        entityManager.persist(RuleConformanceRate.builder().ruleId("rule-1").conformanceRate(50).day(OLD_DAY).build());
        entityManager.persist(RuleConformanceRate.builder().ruleId("rule-1").conformanceRate(95).day(recentDay).build());
        entityManager.flush();
        entityManager.clear();

        housekeepingService.performHousekeeping(MAX_AGE_DAYS);
        entityManager.flush();
        entityManager.clear();

        List<RuleConformanceRate> remaining = jpaRuleConformanceRateRepository.findByRuleId("rule-1");
        assertThat(remaining).hasSize(1);
        assertThat(remaining.getFirst().getConformanceRate()).isEqualTo(95);
    }

    @Test
    void performHousekeeping_shouldDeleteOldRuleStates() {
        SystemComponent componentA = createAndPersistSystemWithComponent();
        SystemComponent componentB = createAndPersistSystemWithComponent();
        ZonedDateTime oldTimestamp = ZonedDateTime.now().minusDays(MAX_AGE_DAYS + 10);
        ZonedDateTime recentTimestamp = ZonedDateTime.now();

        entityManager.persist(RuleState.createWithTimestamps(
                RuleId.of("rule-1"), componentA, State.FAIL, oldTimestamp, oldTimestamp));
        entityManager.persist(RuleState.createWithTimestamps(
                RuleId.of("rule-1"), componentB, State.OK, recentTimestamp, recentTimestamp));
        entityManager.flush();
        entityManager.clear();

        housekeepingService.performHousekeeping(MAX_AGE_DAYS);
        entityManager.flush();
        entityManager.clear();

        assertThat(jpaRuleStateRepository.findBySystemComponentAndRuleId(componentA, "rule-1")).isEmpty();
        assertThat(jpaRuleStateRepository.findBySystemComponentAndRuleId(componentB, "rule-1"))
                .isPresent()
                .hasValueSatisfying(rs -> assertThat(rs.getState()).isEqualTo(State.OK));
    }

    @Test
    void performHousekeeping_shouldNotDeleteRecentEntries() {
        System system = createAndPersistSystem();
        SystemComponent component = system.getSystemComponents().getFirst();
        LocalDate recentDay = TODAY.minusDays(MAX_AGE_DAYS - 1);
        ZonedDateTime recentTimestamp = ZonedDateTime.now();

        entityManager.persist(SystemScore.builder().system(system).score(80).day(recentDay).build());
        entityManager.persist(ComponentScore.builder().systemComponent(component).score(70).day(recentDay).build());
        entityManager.persist(RuleConformanceRate.builder().ruleId("rule-1").conformanceRate(90).day(recentDay).build());
        entityManager.persist(RuleState.createWithTimestamps(
                RuleId.of("rule-1"), component, State.OK, recentTimestamp, recentTimestamp));
        entityManager.flush();
        entityManager.clear();

        housekeepingService.performHousekeeping(MAX_AGE_DAYS);
        entityManager.flush();
        entityManager.clear();

        assertThat(jpaSystemScoreRepository.findBySystem(system)).hasSize(1);
        assertThat(jpaComponentScoreRepository.findBySystemComponent(component)).hasSize(1);
        assertThat(jpaRuleConformanceRateRepository.findByRuleId("rule-1")).hasSize(1);
        assertThat(jpaRuleStateRepository.findBySystemComponentAndRuleId(component, "rule-1")).isPresent();
    }

    private System createAndPersistSystem() {
        SystemComponent component = SystemComponent.builder()
                .name("Test Component " + randomUUID())
                .state(State.OK)
                .type(ComponentType.BACKEND_SERVICE)
                .build();
        System system = System.builder()
                .name("Test System " + randomUUID())
                .systemComponents(List.of(component))
                .state(State.OK)
                .aliases(Set.of())
                .build();
        entityManager.persist(system);
        entityManager.flush();
        return system;
    }

    private SystemComponent createAndPersistSystemWithComponent() {
        return createAndPersistSystem().getSystemComponents().getFirst();
    }
}
