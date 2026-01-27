package ch.admin.bit.jeap.governance.pactbroker;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PactBrokerComponent {

    @PostConstruct
    public void init() {
        log.info("The pactbroker module will soon be implemented");
    }
}
