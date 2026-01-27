package ch.admin.bit.jeap.governance.prometheus;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PrometheusComponent {

    @PostConstruct
    public void init() {
        log.info("The prometheus module will soon be implemented");
    }
}
