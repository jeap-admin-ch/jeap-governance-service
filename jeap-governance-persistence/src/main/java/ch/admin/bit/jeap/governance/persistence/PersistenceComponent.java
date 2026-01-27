package ch.admin.bit.jeap.governance.persistence;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PersistenceComponent {

    @PostConstruct
    public void init() {
        log.info("The persistence module will soon be implemented");
    }
}
