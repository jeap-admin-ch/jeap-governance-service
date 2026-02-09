package ch.admin.bit.jeap.governance.deploymentlog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeploymentLogPropertiesTest {

    @Test
    void checkAndLog_shouldSucceed_whenAllPropertiesSet() {
        DeploymentLogProperties properties = createProperties("http://example.com", "myUser", "myPassword");
        assertDoesNotThrow(properties::checkAndLog);
    }

    @ParameterizedTest(name = "should throw when {3}")
    @MethodSource("invalidProperties")
    void checkAndLog_shouldThrow_whenPropertyInvalid(String url, String username, String password, String description) {
        DeploymentLogProperties properties = createProperties(url, username, password);
        assertThrows(IllegalArgumentException.class, properties::checkAndLog, description);
    }

    private static Stream<Arguments> invalidProperties() {
        return Stream.of(
                Arguments.of(null, "myUser", "myPassword", "url is null"),
                Arguments.of("", "myUser", "myPassword", "url is empty"),
                Arguments.of("http://example.com", null, "myPassword", "username is null"),
                Arguments.of("http://example.com", "", "myPassword", "username is empty"),
                Arguments.of("http://example.com", "myUser", null, "password is null"),
                Arguments.of("http://example.com", "myUser", "", "password is empty")
        );
    }

    private static DeploymentLogProperties createProperties(String url, String username, String password) {
        DeploymentLogProperties properties = new DeploymentLogProperties();
        properties.setUrl(url);
        properties.setUsername(username);
        properties.setPassword(password);
        return properties;
    }
}
