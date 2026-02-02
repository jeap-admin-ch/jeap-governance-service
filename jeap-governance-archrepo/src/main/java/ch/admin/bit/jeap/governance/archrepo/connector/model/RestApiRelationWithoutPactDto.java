package ch.admin.bit.jeap.governance.archrepo.connector.model;

import lombok.Value;

@Value
public class RestApiRelationWithoutPactDto {
    String consumerSystem;
    String consumer;
    String providerSystem;
    String provider;
    String method;
    String path;
}
