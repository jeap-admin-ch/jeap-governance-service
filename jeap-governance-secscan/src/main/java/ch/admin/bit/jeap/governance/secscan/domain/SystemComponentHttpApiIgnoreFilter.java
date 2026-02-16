package ch.admin.bit.jeap.governance.secscan.domain;

public interface SystemComponentHttpApiIgnoreFilter {

    Result shouldIgnoreApi(SystemComponentHttpApi api);

    Result shouldIgnoreEndpoint(String systemComponentName, HttpEndpoint httpEndpoint);

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
