package ch.admin.bit.jeap.governance.secscan.domain;

import ch.admin.bit.jeap.governance.domain.GovernanceServiceEnvironment;


public interface SystemComponentHttpApiDiscoveryClient {

    SystemComponentHttpApi discover(String systemComponentName, GovernanceServiceEnvironment environment);

}
