package ch.admin.bit.jeap.governance.web;

import ch.admin.bit.jeap.governance.prometheus.amp.AmazonManagedPromClient;
import ch.admin.bit.jeap.governance.prometheus.datadeletion.PrometheusComponentDeletionListener;
import ch.admin.bit.jeap.governance.prometheus.dataimport.PrometheusImporter;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeriesQueryRepository;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeriesRepository;
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
        "jeap.governance.prometheus.enabled=false"
})
class PrometheusDisabledIT extends PostgresTestContainerBase {

    @Autowired
    private ApplicationContext context;

    @Test
    void prometheusBeans_shouldNotExist_whenDisabled() {
        assertThat(context.getBeansOfType(PrometheusImporter.class)).isEmpty();
        assertThat(context.getBeansOfType(PrometheusComponentDeletionListener.class)).isEmpty();
        assertThat(context.getBeansOfType(AmazonManagedPromClient.class)).isEmpty();
        assertThat(context.getBeansOfType(PromTimeSeriesRepository.class)).isEmpty();
        assertThat(context.getBeansOfType(PromTimeSeriesQueryRepository.class)).isEmpty();
    }
}
