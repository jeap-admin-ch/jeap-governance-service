package ch.admin.bit.jeap.governance.persistence;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "ch.admin.bit.jeap.governance.domain")
@EnableJpaRepositories(basePackages = "ch.admin.bit.jeap.governance.persistence")
public class TestApplication {
}
