package ch.admin.bit.jeap.governance.deploymentlog;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DeploymentLogComponent {

    @PostConstruct
    public void init() {
        log.info("The deploymentlog module will soon be implemented");
    }
}
