package ch.admin.bit.jeap.governance.secscan.httpcheck;

import ch.admin.bit.jeap.governance.domain.plugin.security.api.HttpEndpoint;
import ch.admin.bit.jeap.governance.secscan.domain.HttpEndpointSecurityChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class DefaultHttpEndpointSecurityCheckerTest {

    private MockRestServiceServer mockServer;
    private DefaultHttpEndpointSecurityChecker checker;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        checker = new DefaultHttpEndpointSecurityChecker(builder.build());
    }

    static Stream<Arguments> secureStatusCodes() {
        return Stream.of(
                Arguments.of(HttpStatus.UNAUTHORIZED, "401"),
                Arguments.of(HttpStatus.FORBIDDEN, "403"),
                Arguments.of(HttpStatus.BAD_GATEWAY, "502"),
                Arguments.of(HttpStatus.SERVICE_UNAVAILABLE, "503"),
                Arguments.of(HttpStatus.GATEWAY_TIMEOUT, "504")
        );
    }

    static Stream<Arguments> insecureStatusCodes() {
        return Stream.of(
                Arguments.of(HttpStatus.OK, "200"),
                Arguments.of(HttpStatus.MOVED_PERMANENTLY, "301"),
                Arguments.of(HttpStatus.NOT_FOUND, "404"),
                Arguments.of(HttpStatus.INTERNAL_SERVER_ERROR, "500")
        );
    }

    @ParameterizedTest(name = "HTTP {0} should not be flagged")
    @MethodSource("secureStatusCodes")
    void check_secureStatusCode_notFlagged(HttpStatus status, String expectedCodeInReason) {
        mockServer.expect(requestTo("http://api.example.com/api/test"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(status));

        HttpEndpointSecurityChecker.Result result = checker.check("http://api.example.com", new HttpEndpoint("/api/test", "GET"));

        assertThat(result.failed()).isFalse();
        assertThat(result.reason()).contains(expectedCodeInReason);
    }

    @ParameterizedTest(name = "HTTP {0} should be flagged")
    @MethodSource("insecureStatusCodes")
    void check_insecureStatusCode_flagged(HttpStatus status, String expectedCodeInReason) {
        mockServer.expect(requestTo("http://api.example.com/api/test"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(status));

        HttpEndpointSecurityChecker.Result result = checker.check("http://api.example.com", new HttpEndpoint("/api/test", "GET"));

        assertThat(result.failed()).isTrue();
        assertThat(result.reason()).contains(expectedCodeInReason);
    }

    @Test
    void check_requestThrowsException_notFlagged() {
        mockServer.expect(requestTo("http://api.example.com/api/unreachable"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(request -> { throw new IOException("Connection refused"); });

        HttpEndpointSecurityChecker.Result result = checker.check("http://api.example.com", new HttpEndpoint("/api/unreachable", "GET"));

        assertThat(result.failed()).isFalse();
        assertThat(result.reason()).contains("could not be checked");
    }

    @Test
    void check_postMethod_usesCorrectHttpMethod() {
        mockServer.expect(requestTo("http://api.example.com/api/data"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        HttpEndpointSecurityChecker.Result result = checker.check("http://api.example.com", new HttpEndpoint("/api/data", "POST"));

        assertThat(result.failed()).isFalse();
        assertThat(result.reason()).contains("401");
    }

    @Test
    void check_deleteMethod_usesCorrectHttpMethod() {
        mockServer.expect(requestTo("http://api.example.com/api/resource"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        HttpEndpointSecurityChecker.Result result = checker.check("http://api.example.com", new HttpEndpoint("/api/resource", "DELETE"));

        assertThat(result.failed()).isFalse();
        assertThat(result.reason()).contains("403");
    }

    @Test
    void check_pathWithPathVariables_substitutesVariables() {
        mockServer.expect(requestTo("http://api.example.com/api/users/test/orders/test"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        HttpEndpointSecurityChecker.Result result = checker.check("http://api.example.com",
                new HttpEndpoint("/api/users/{userId}/orders/{orderId}", "GET"));

        assertThat(result.failed()).isFalse();
        assertThat(result.reason()).contains("403");
    }

    @Test
    void check_pathWithSinglePathVariable_substitutesVariable() {
        mockServer.expect(requestTo("http://api.example.com/api/items/test"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK));

        HttpEndpointSecurityChecker.Result result = checker.check("http://api.example.com",
                new HttpEndpoint("/api/items/{id}", "GET"));

        assertThat(result.failed()).isTrue();
        assertThat(result.reason()).contains("200");
    }

    @Test
    void check_pathWithNoVariables_usesPathAsIs() {
        mockServer.expect(requestTo("http://api.example.com/api/health"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK));

        HttpEndpointSecurityChecker.Result result = checker.check("http://api.example.com",
                new HttpEndpoint("/api/health", "GET"));

        assertThat(result.failed()).isTrue();
    }
}
