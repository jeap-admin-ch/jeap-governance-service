package ch.admin.bit.jeap.governance.web.api.docs.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OK: compliant, FAIL: non-compliant, PAUSED: temporarily exempted, DISABLED: exempted, "
                      + "UNKNOWN: not determined")
public enum RuleStateValue {
    OK, FAIL, PAUSED, DISABLED, UNKNOWN
}
