package ch.admin.bit.jeap.governance.prometheus;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeries;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@AutoConfigurationPackage(basePackageClasses = {PromTimeSeries.class, SystemComponent.class})
@EnableJpaRepositories(basePackages = {
		"ch.admin.bit.jeap.governance.persistence",
		"ch.admin.bit.jeap.governance.prometheus.persistence"
})
public class TestApplication {
}
