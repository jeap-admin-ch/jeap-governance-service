package ch.admin.bit.jeap.governance.domain;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DomainComponent {

    @PostConstruct
    public void init() {
        log.info("The domain module will soon be implemented");
    }
}
