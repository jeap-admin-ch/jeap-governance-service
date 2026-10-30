package ch.admin.bit.jeap.governance.web.api.docs.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The trend over the trend period, a linear regression with a noise threshold. UNKNOWN with a "
                      + "single value, NO_DATA without any.")
public enum Trend {
    UP, DOWN, STABLE, UNKNOWN, NO_DATA
}
