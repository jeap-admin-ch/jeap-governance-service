package ch.admin.bit.jeap.governance.rules;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RulesComponent {

    @PostConstruct
    public void init() {
        log.info("The rules module will soon be implemented");
    }
}
