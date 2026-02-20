package ch.admin.bit.jeap.governance.reporting;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReportingPropertiesTest {

    @Test
    void checkAndLog_allPropertiesSet() {
        ReportingProperties properties = createProperties();

        Assertions.assertDoesNotThrow(properties::checkAndLog);
    }

    @Test
    void checkAndLog_throwIllegalArgumentException_whenConfluenceUrlNotSet() {
        ReportingProperties properties = createProperties();
        properties.getConfluence().setUrl(null);

        assertThrows(IllegalArgumentException.class, properties::checkAndLog);
    }

    @Test
    void checkAndLog_throwIllegalArgumentException_whenConfluenceRootPageNameNotSet() {
        ReportingProperties properties = createProperties();
        properties.getConfluence().setRootPageName(null);

        assertThrows(IllegalArgumentException.class, properties::checkAndLog);
    }

    @Test
    void checkAndLog_throwIllegalArgumentException_whenConfluenceRootAncestorIdNotSet() {
        ReportingProperties properties = createProperties();
        properties.getConfluence().setAncestorId(null);

        assertThrows(IllegalArgumentException.class, properties::checkAndLog);
    }

    @Test
    void checkAndLog_throwIllegalArgumentException_whenConfluenceSpaceKeyNotSet() {
        ReportingProperties properties = createProperties();
        properties.getConfluence().setSpaceKey(null);

        assertThrows(IllegalArgumentException.class, properties::checkAndLog);
    }

    @Test
    void checkAndLog_throwIllegalArgumentException_whenConfluenceUsernameNotSet() {
        ReportingProperties properties = createProperties();
        properties.getConfluence().setUsername(null);

        assertThrows(IllegalArgumentException.class, properties::checkAndLog);
    }

    @Test
    void checkAndLog_throwIllegalArgumentException_whenConfluencePasswordNotSet() {
        ReportingProperties properties = createProperties();
        properties.getConfluence().setPassword(null);

        assertThrows(IllegalArgumentException.class, properties::checkAndLog);
    }

    @Test
    void checkProperties() {
        ReportingProperties properties = createProperties();

        assertEquals(30, properties.getTrendPeriodDays());
        assertEquals("https://example.com/confluence", properties.getConfluenceUrl());
        assertEquals("12345", properties.getConfluenceSpaceKey());
        assertEquals("Test", properties.getConfluenceRootPageName());
        assertEquals("123456789", properties.getConfluenceAncestorId());
        assertEquals("user", properties.getConfluenceUsername());
        assertEquals("pass", properties.getConfluencePassword());
    }

    private static ReportingProperties createProperties() {
        ReportingProperties properties = new ReportingProperties();
        properties.setTrendPeriodDays(30);

        properties.getConfluence().setUrl("https://example.com/confluence");
        properties.getConfluence().setSpaceKey("12345");
        properties.getConfluence().setRootPageName("Test");
        properties.getConfluence().setAncestorId("123456789");
        properties.getConfluence().setUsername("user");
        properties.getConfluence().setPassword("pass");
        return properties;
    }
}
