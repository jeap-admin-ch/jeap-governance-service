package ch.admin.bit.jeap.governance.secscan;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.secscan.domain.SecscanState;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@AutoConfigurationPackage(basePackageClasses = {SecscanState.class, SystemComponent.class})
@EnableJpaRepositories(basePackages = {
		"ch.admin.bit.jeap.governance.persistence",
		"ch.admin.bit.jeap.governance.secscan.persistence"
})
public class TestApplication {
}
