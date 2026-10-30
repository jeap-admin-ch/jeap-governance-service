package ch.admin.bit.jeap.governance.web.api.docs.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "PAUSED: until a day not yet passed, DISABLED: without an end day, EXPIRED: the end day has "
                      + "passed and the rule applies again")
public enum ExemptionEffect {
    PAUSED, DISABLED, EXPIRED
}
