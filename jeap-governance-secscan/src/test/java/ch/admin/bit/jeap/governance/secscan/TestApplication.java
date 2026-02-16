package ch.admin.bit.jeap.governance.secscan;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.secscan.domain.SecscanState;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackageClasses = {SecscanState.class, SystemComponent.class})
public class TestApplication {
}
