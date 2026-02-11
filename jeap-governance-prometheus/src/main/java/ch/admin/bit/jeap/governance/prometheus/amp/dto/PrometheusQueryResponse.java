package ch.admin.bit.jeap.governance.prometheus.amp.dto;

import lombok.Value;

@Value
public class PrometheusQueryResponse {
    String status;
    PrometheusQueryResponseData data;
    String error;
    String errorType;
}
