package ch.admin.bit.jeap.governance.archrepo;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArchRepoPropertiesTest {

    @Test
    void shouldInitializeProperties() {
        ArchRepoProperties properties = new ArchRepoProperties();
        properties.setUrl("http://example.com/repo");
        properties.setTimeout(Duration.ofSeconds(30));
        assertDoesNotThrow(properties::checkAndLog);
        assertEquals("http://example.com/repo", properties.getUrl());
        assertEquals(Duration.ofSeconds(30), properties.getTimeout());
    }

    @Test
    void shouldThrowExceptionWhenUrlIsEmpty() {
        ArchRepoProperties properties = new ArchRepoProperties();
        properties.setUrl("");
        properties.setTimeout(Duration.ofSeconds(30));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, properties::checkAndLog);
        assertEquals("ArchRepoProperties 'url' must be set", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenUrlIsNull() {
        ArchRepoProperties properties = new ArchRepoProperties();
        properties.setUrl(null);
        properties.setTimeout(Duration.ofSeconds(30));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, properties::checkAndLog);
        assertEquals("ArchRepoProperties 'url' must be set", exception.getMessage());
    }

}
