package ch.admin.bit.jeap.governance.prometheus.amp;

import ch.admin.bit.jeap.governance.domain.GovernanceServiceEnvironment;
import ch.admin.bit.jeap.governance.prometheus.domain.PromQueryType;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeriesSample;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to check the connection to an Amazon Managed Prometheus instance. Disabled by default as instance
 * properties and access credentials need to be provided. Therefore, to be run manually for testing against an actual
 * AWS AMP instance.
 */
@Disabled
class AmazonManagedPromClientIT {

    private static AmazonManagedPromClient amazonManagedPromClient;
    private static final String SERVICE_NAME = "service-name tbd";
    private static final GovernanceServiceEnvironment ENVIRONMENT = GovernanceServiceEnvironment.PROD;
    private static final PromQueryType QUERY_TYPE = PromQueryType.JEAP_MESSAGING_SIGNATURE_REQUIRED;


    @BeforeAll
    static void setup() {
        AmazonManagedPromClientProperties properties = new AmazonManagedPromClientProperties();
        properties.setHost("host tbd");
        properties.setWorkspace("workspace tbd");
        properties.setRoleArn("role-arn tbd");
        properties.setRoleSessionName("session tbd");
        amazonManagedPromClient = new AmazonManagedPromClient(properties);
    }

    @Test
    void queryPrometheus() {
        List<PromTimeSeriesSample> results = amazonManagedPromClient.query(QUERY_TYPE, ENVIRONMENT, SERVICE_NAME);

        assertFalse(results.isEmpty(), "There must be results");
        assertEquals(SERVICE_NAME, results.getFirst().metric().get("service"));
        assertTrue(results.getFirst().metric().containsKey("service"), "We expect a service in the response");
        assertTrue(results.getFirst().metric().containsKey("stage"), "We expect a stage in the response");
        assertTrue(results.getFirst().metric().containsKey("account_id"), "We expect an account_id in the response");
    }

}
