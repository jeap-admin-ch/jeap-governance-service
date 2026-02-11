package ch.admin.bit.jeap.governance.prometheus.domain;

import java.util.List;
import java.util.Map;

public record PromTimeSeriesSample (
        Map<String, String> metric, // label key-value pairs
        List<String> value
) {}
