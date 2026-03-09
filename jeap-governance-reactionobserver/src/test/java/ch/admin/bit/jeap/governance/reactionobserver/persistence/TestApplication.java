package ch.admin.bit.jeap.governance.reactionobserver.persistence;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = {
        "ch.admin.bit.jeap.governance.domain",
        "ch.admin.bit.jeap.governance.reactionobserver.domain"

})
@EnableJpaRepositories(basePackages = {
        "ch.admin.bit.jeap.governance.persistence",
        "ch.admin.bit.jeap.governance.reactionobserver.persistence"})
public class TestApplication {
}
