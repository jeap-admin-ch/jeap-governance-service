package ch.admin.bit.jeap.governance.prometheus.amp.dto;

import lombok.Value;

import java.util.List;

@Value
public class PrometheusQueryResponseData {
    String resultType;
    List<PrometheusQueryResponseResult> result;
}
