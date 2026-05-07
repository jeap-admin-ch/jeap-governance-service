package ch.admin.bit.jeap.governance.deploymentlog.persistence;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@AutoConfigurationPackage(basePackages = {
        "ch.admin.bit.jeap.governance.domain",
        "ch.admin.bit.jeap.governance.deploymentlog.domain"

})
@EnableJpaRepositories(basePackages = {
        "ch.admin.bit.jeap.governance.persistence",
        "ch.admin.bit.jeap.governance.deploymentlog.persistence"})
public class TestApplication {
}
