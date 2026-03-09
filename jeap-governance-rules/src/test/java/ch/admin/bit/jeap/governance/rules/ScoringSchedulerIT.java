package ch.admin.bit.jeap.governance.rules;

import ch.admin.bit.jeap.governance.domain.SystemRepository;
import ch.admin.bit.jeap.governance.domain.rule.RuleConformanceRateService;
import ch.admin.bit.jeap.governance.domain.score.ScoringService;
import io.micrometer.core.instrument.MeterRegistry;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {ScoringSchedulerIT.TestConfig.class, ScoringScheduler.class})
@ActiveProfiles("scheduler-it")
class ScoringSchedulerIT {

    @TestConfiguration
    @EnableScheduling
    @EnableSchedulerLock(defaultLockAtMostFor = "PT10S")
    static class TestConfig {
        @Bean
        LockProvider lockProvider() {
            SimpleLock lock = mock(SimpleLock.class);
            LockProvider provider = mock(LockProvider.class);
            when(provider.lock(any())).thenReturn(Optional.of(lock));
            return provider;
        }
    }

    @MockitoBean
    private ScoringService scoringService;

    @MockitoBean
    private SystemRepository systemRepository;

    @MockitoBean
    private RuleConformanceRateService ruleConformanceRateService;

    @MockitoBean
    private MeterRegistry meterRegistry;

    @Test
    void schedulerFiresAndCallsScoringService() {
        when(systemRepository.findAllIds()).thenReturn(List.of(1L));

        await().atMost(5, SECONDS).untilAsserted(() ->
                verify(scoringService, atLeastOnce()).updateSystemScore(any(Long.class), any())
        );
    }

}
