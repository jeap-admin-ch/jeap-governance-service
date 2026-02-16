package ch.admin.bit.jeap.governance.secscan.filter;

import ch.admin.bit.jeap.governance.domain.GovernanceServiceEnvironment;
import ch.admin.bit.jeap.governance.secscan.domain.HttpEndpoint;
import ch.admin.bit.jeap.governance.secscan.domain.SystemComponentHttpApi;
import ch.admin.bit.jeap.governance.secscan.domain.SystemComponentHttpApiIgnoreFilter;
import org.springframework.stereotype.Component;

/**
 * This is just a sample filter implementation that ignores all APIs that are not in the DEV environment and all
 * endpoints that are not GET requests. In the final implementation, the logic will be more complex, based on specific
 * requirements and configurations. -> This sample implementation will be replaced later.
 */
@Component
class DevAndGetOnlyFilter implements SystemComponentHttpApiIgnoreFilter {

    @Override
    public Result shouldIgnoreApi(SystemComponentHttpApi api) {
        if (GovernanceServiceEnvironment.DEV != api.environment()) {
            return Result.ignoredWithReason("Only APIs in the DEV environment should be checked");
        } else {
            return Result.notIgnored();
        }
    }

    @Override
    public Result shouldIgnoreEndpoint(String systemComponentName, HttpEndpoint httpEndpoint) {
        if (!"GET".equalsIgnoreCase(httpEndpoint.method())) {
            return Result.ignoredWithReason("Only GET requests should be checked");
        } else {
            return Result.notIgnored();
        }
    }

}
