package ch.admin.bit.jeap.governance.docgen;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DocGenerationComponent {

    @PostConstruct
    public void init() {
        log.info("The docgen module will soon be implemented");
    }
}
