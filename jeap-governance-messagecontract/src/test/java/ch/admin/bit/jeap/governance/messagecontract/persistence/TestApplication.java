package ch.admin.bit.jeap.governance.messagecontract.persistence;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@AutoConfigurationPackage(basePackages = {
        "ch.admin.bit.jeap.governance.domain",
        "ch.admin.bit.jeap.governance.messagecontract.domain"
})
@EnableJpaRepositories(basePackages = {
        "ch.admin.bit.jeap.governance.persistence",
        "ch.admin.bit.jeap.governance.messagecontract.persistence"
})
class TestApplication {
}
