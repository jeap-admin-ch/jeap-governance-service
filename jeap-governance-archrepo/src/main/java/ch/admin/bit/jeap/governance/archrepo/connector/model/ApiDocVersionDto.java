package ch.admin.bit.jeap.governance.archrepo.connector.model;

import lombok.Value;

@Value
public class ApiDocVersionDto {
    String system;
    String component;
    String version;
}
