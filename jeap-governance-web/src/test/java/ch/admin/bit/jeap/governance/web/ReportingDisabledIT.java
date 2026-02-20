package ch.admin.bit.jeap.governance.web;

import ch.admin.bit.jeap.governance.reporting.ReportingService;
import ch.admin.bit.jeap.governance.reporting.confluence.ConfluenceAdapter;
import ch.admin.bit.jeap.governance.reporting.confluence.ReportGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "jeap.governance.archrepo.url=http://localhost:8080",
        "jeap.governance.environment=DEV",
        "jeap.governance.reporting.enabled=false"
})
class ReportingDisabledIT extends PostgresTestContainerBase {

    @Autowired
    private ApplicationContext context;

    @Test
    void prometheusBeans_shouldNotExist_whenDisabled() {
        assertThat(context.getBeansOfType(ReportingService.class)).isEmpty();
        assertThat(context.getBeansOfType(ConfluenceAdapter.class)).isEmpty();
        assertThat(context.getBeansOfType(ReportGenerator.class)).isEmpty();
    }
}
