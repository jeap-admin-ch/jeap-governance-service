package ch.admin.bit.jeap.governance.archrepo;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ArchRepoComponent {

    @PostConstruct
    public void init() {
        log.info("The archrepo module will soon be implemented");
    }
}
