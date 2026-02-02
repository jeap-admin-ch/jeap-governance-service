package ch.admin.bit.jeap.governance.archrepo.persistence;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = {
        "ch.admin.bit.jeap.governance.domain",
        "ch.admin.bit.jeap.governance.archrepo.domain"

})
@EnableJpaRepositories(basePackages = {
        "ch.admin.bit.jeap.governance.persistence",
        "ch.admin.bit.jeap.governance.archrepo.persistence"})
public class TestApplication {
}
