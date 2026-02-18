package ch.admin.bit.jeap.governance.prometheus;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeries;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackageClasses = {PromTimeSeries.class, SystemComponent.class})
public class TestApplication {
}
