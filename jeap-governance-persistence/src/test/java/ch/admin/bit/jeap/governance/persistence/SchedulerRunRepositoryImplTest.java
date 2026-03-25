package ch.admin.bit.jeap.governance.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SchedulerRunRepositoryImpl.class)
class SchedulerRunRepositoryImplTest extends PostgresTestContainerBase {

    @Autowired
    private SchedulerRunRepositoryImpl repository;

    @Test
    void findLastRunDateTime_returnsEmpty_whenNoRunPersisted() {
        Optional<LocalDateTime> result = repository.findLastRunDateTime("data-import");

        assertThat(result).isEmpty();
    }

    @Test
    void saveAndFindLastRunDateTime() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);

        repository.saveLastRunDateTime("data-import", now);

        Optional<LocalDateTime> result = repository.findLastRunDateTime("data-import");
        assertThat(result).contains(now);
    }

    @Test
    void saveLastRunDateTime_updatesExistingRow() {
        LocalDateTime first = LocalDateTime.of(2025, 1, 15, 10, 0);
        LocalDateTime second = LocalDateTime.of(2025, 6, 20, 14, 30);

        repository.saveLastRunDateTime("scoring", first);
        repository.saveLastRunDateTime("scoring", second);

        Optional<LocalDateTime> result = repository.findLastRunDateTime("scoring");
        assertThat(result).contains(second);
    }

    @Test
    void saveLastRunDateTime_differentJobsAreIndependent() {
        LocalDateTime importTime = LocalDateTime.of(2025, 3, 10, 8, 0);
        LocalDateTime scoringTime = LocalDateTime.of(2025, 3, 10, 12, 0);

        repository.saveLastRunDateTime("data-import", importTime);
        repository.saveLastRunDateTime("scoring", scoringTime);

        assertThat(repository.findLastRunDateTime("data-import")).contains(importTime);
        assertThat(repository.findLastRunDateTime("scoring")).contains(scoringTime);
    }
}
