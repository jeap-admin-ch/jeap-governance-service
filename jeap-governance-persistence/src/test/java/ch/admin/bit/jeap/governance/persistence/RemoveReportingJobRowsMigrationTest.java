package ch.admin.bit.jeap.governance.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the core migrations in a schema of its own, with rows written between the version before the cleanup and the
 * cleanup itself. Not transactional, as Flyway migrates on connections of its own.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RemoveReportingJobRowsMigrationTest extends PostgresTestContainerBase {

    private static final String SCHEMA = "migration_test";
    private static final Timestamp RUN_AT = Timestamp.valueOf(LocalDateTime.of(2026, 11, 20, 6, 55));

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void migrateToTheVersionBeforeTheCleanup() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        flywayUpTo("1.0.3").migrate();
    }

    @AfterEach
    void dropSchema() {
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
    }

    @Test
    void removesTheLastRunAndTheLockOfTheReportingJobOnly() {
        for (String job : new String[]{"reporting", "scoring", "data-import"}) {
            jdbcTemplate.update("INSERT INTO " + SCHEMA + ".scheduler_run (job_name, last_run_at) VALUES (?, ?)",
                    job, RUN_AT);
            jdbcTemplate.update("INSERT INTO " + SCHEMA + ".shedlock (name, lock_until, locked_at, locked_by) "
                                + "VALUES (?, ?, ?, 'test')", job, RUN_AT, RUN_AT);
        }

        flywayUpTo("latest").migrate();

        assertThat(jdbcTemplate.queryForList("SELECT job_name FROM " + SCHEMA + ".scheduler_run", String.class))
                .containsExactlyInAnyOrder("scoring", "data-import");
        assertThat(jdbcTemplate.queryForList("SELECT name FROM " + SCHEMA + ".shedlock", String.class))
                .containsExactlyInAnyOrder("scoring", "data-import");
    }

    private Flyway flywayUpTo(String target) {
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas(SCHEMA)
                .locations("classpath:db/migration")
                .target(target)
                .load();
    }
}
