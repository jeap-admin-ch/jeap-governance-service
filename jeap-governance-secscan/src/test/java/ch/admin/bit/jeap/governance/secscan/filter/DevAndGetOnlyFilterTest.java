package ch.admin.bit.jeap.governance.secscan.filter;

import ch.admin.bit.jeap.governance.domain.GovernanceServiceEnvironment;
import ch.admin.bit.jeap.governance.secscan.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DevAndGetOnlyFilterTest {

    private final DevAndGetOnlyFilter filter = new DevAndGetOnlyFilter();

    @Test
    void shouldIgnoreApi_devEnvironment_notIgnored() {
        SystemComponentHttpApi api = createApi(GovernanceServiceEnvironment.DEV);

        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreApi(api);

        assertThat(result.ignore()).isFalse();
    }

    @ParameterizedTest(name = "{0} environment should be ignored")
    @EnumSource(value = GovernanceServiceEnvironment.class, names = "DEV", mode = EnumSource.Mode.EXCLUDE)
    void shouldIgnoreApi_nonDevEnvironment_ignored(GovernanceServiceEnvironment environment) {
        SystemComponentHttpApi api = createApi(environment);

        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreApi(api);

        assertThat(result.ignore()).isTrue();
        assertThat(result.reason()).contains("DEV");
    }

    @Test
    void shouldIgnoreEndpoint_getRequest_notIgnored() {
        HttpEndpoint endpoint = new HttpEndpoint("/api/users", "GET");

        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreEndpoint("mysystem-mycomp-svc", endpoint);

        assertThat(result.ignore()).isFalse();
    }

    @Test
    void shouldIgnoreEndpoint_getLowerCase_notIgnored() {
        HttpEndpoint endpoint = new HttpEndpoint("/api/users", "get");

        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreEndpoint("mysystem-mycomp-svc", endpoint);

        assertThat(result.ignore()).isFalse();
    }

    @ParameterizedTest(name = "{0} request should be ignored")
    @ValueSource(strings = {"POST", "PUT", "DELETE", "PATCH"})
    void shouldIgnoreEndpoint_nonGetRequest_ignored(String httpMethod) {
        HttpEndpoint endpoint = new HttpEndpoint("/api/users", httpMethod);

        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreEndpoint("mysystem-mycomp-svc", endpoint);

        assertThat(result.ignore()).isTrue();
        assertThat(result.reason()).contains("GET");
    }

    private SystemComponentHttpApi createApi(GovernanceServiceEnvironment environment) {
        HttpApi httpApi = new HttpApi("http://example.com", "1.0", List.of(new HttpEndpoint("/api/test", "GET")));
        return new SystemComponentHttpApi("mysystem-mycomp-svc", environment, httpApi, null);
    }
}
