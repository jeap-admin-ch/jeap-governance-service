package ch.admin.bit.jeap.governance.secscan.domain;

import ch.admin.bit.jeap.governance.domain.plugin.security.api.HttpEndpoint;
import ch.admin.bit.jeap.governance.domain.plugin.security.api.SystemComponentHttpApi;

public interface SystemComponentHttpApiIgnoreFilter {

    Result shouldIgnoreApi(SystemComponentHttpApi api);

    Result shouldIgnoreEndpoint(String systemComponentName, HttpEndpoint httpEndpoint, String environment);

    record Result(boolean ignore, String reason) {
        private static final Result NOT_IGNORED = new Result(false, null);

        public static Result ignoredWithReason(String reason) {
            return new Result(true, reason);
        }

        public static Result notIgnored() {
            return NOT_IGNORED;
        }
    }

}
