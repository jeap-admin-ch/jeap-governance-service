package ch.admin.bit.jeap.governance.web;

import ch.admin.bit.jeap.governance.dataimport.DataImportScheduler;
import ch.admin.bit.jeap.governance.domain.plugin.datasource.DataSourceImporter;
import ch.admin.bit.jeap.governance.domain.scheduler.JobStopSignal;
import ch.admin.bit.jeap.governance.web.api.management.AsyncJobRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An import still running when the service shuts down ends at its next consistent point and releases its lock, so
 * that the next instance can import right away instead of waiting for {@code lock-at-most} to run out.
 * <p>
 * Stops the context instead of closing it: a shutdown stops the lifecycle beans first, which is where the import has to
 * end, and only then closes the database connections. A stopped context keeps them, for the assertions, and the test
 * framework can restart it.
 */
@SpringBootTest(properties = "jeap.governance.dataimport.lock-at-most=PT1H")
@Import(DataImportShutdownIT.LongRunningImporterConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DataImportShutdownIT extends GovernanceIntegrationTestBase {

    private static final CountDownLatch IMPORT_RUNNING = new CountDownLatch(1);

    /**
     * Imported, not a {@code @TestConfiguration}: the component scan of {@code GovernanceWebConfig} covers this package
     * and would add the importer to every other integration test.
     */
    static class LongRunningImporterConfig {

        /**
         * Stands for an importer working through many components: it runs until the service shuts down.
         */
        @Bean
        @Order(Ordered.LOWEST_PRECEDENCE)
        DataSourceImporter longRunningImporter(JobStopSignal stopSignal) {
            return () -> {
                IMPORT_RUNNING.countDown();
                long giveUpAt = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
                while (!stopSignal.isStopRequested() && System.nanoTime() < giveUpAt) {
                    sleep();
                }
            };
        }

        private static void sleep() {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataImportScheduler dataImportScheduler;

    @Autowired
    private AsyncJobRunner asyncJobRunner;

    @Test
    void shutdown_endsTheRunningImportAndReleasesItsLock() throws Exception {
        assertThat(dataImportScheduler.startImport(asyncJobRunner::run)).isTrue();
        assertThat(IMPORT_RUNNING.await(30, TimeUnit.SECONDS)).isTrue();
        assertThat(lockUntil()).isAfter(LocalDateTime.now().plusMinutes(30));

        context.stop();

        assertThat(lockUntil()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(jdbcTemplate.queryForList("SELECT job_name FROM scheduler_run WHERE job_name = 'data-import'"))
                .as("a stopped import is not recorded as a run")
                .isEmpty();
    }

    private LocalDateTime lockUntil() {
        return jdbcTemplate.queryForObject("SELECT lock_until FROM shedlock WHERE name = 'data-import'", Timestamp.class)
                .toLocalDateTime();
    }
}
