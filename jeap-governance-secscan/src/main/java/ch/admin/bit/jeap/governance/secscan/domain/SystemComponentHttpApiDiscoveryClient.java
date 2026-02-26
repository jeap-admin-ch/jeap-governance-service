package ch.admin.bit.jeap.governance.secscan.domain;

import ch.admin.bit.jeap.governance.domain.GovernanceServiceEnvironment;
import ch.admin.bit.jeap.governance.domain.plugin.security.api.SystemComponentHttpApi;


public interface SystemComponentHttpApiDiscoveryClient {

    SystemComponentHttpApi discover(String systemComponentName, GovernanceServiceEnvironment environment);

}
