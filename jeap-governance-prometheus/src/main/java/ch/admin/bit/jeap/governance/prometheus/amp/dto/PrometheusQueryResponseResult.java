package ch.admin.bit.jeap.governance.prometheus.amp.dto;

import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
public class PrometheusQueryResponseResult {
    Map<String, String> metric;
    List<String> value;

}
