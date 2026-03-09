package ch.admin.bit.jeap.governance.reactionobserver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReactionObserverPropertiesTest {

    @Test
    void checkAndLog_shouldSucceed_whenAllPropertiesSet() {
        ReactionObserverProperties properties = createProperties("https://example.com", "myUser", "myPassword");
        assertDoesNotThrow(properties::checkAndLog);
    }

    @ParameterizedTest(name = "should throw when {3}")
    @MethodSource("invalidProperties")
    void checkAndLog_shouldThrow_whenPropertyInvalid(String url, String username, String password, String description) {
        ReactionObserverProperties properties = createProperties(url, username, password);
        assertThrows(IllegalArgumentException.class, properties::checkAndLog, description);
    }

    private static Stream<Arguments> invalidProperties() {
        return Stream.of(
                Arguments.of(null, "myUser", "myPassword", "url is null"),
                Arguments.of("", "myUser", "myPassword", "url is empty"),
                Arguments.of("https://example.com", null, "myPassword", "username is null"),
                Arguments.of("https://example.com", "", "myPassword", "username is empty"),
                Arguments.of("https://example.com", "myUser", null, "password is null"),
                Arguments.of("https://example.com", "myUser", "", "password is empty")
        );
    }

    private static ReactionObserverProperties createProperties(String url, String username, String password) {
        ReactionObserverProperties properties = new ReactionObserverProperties();
        properties.setUrl(url);
        properties.setUsername(username);
        properties.setPassword(password);
        return properties;
    }
}
