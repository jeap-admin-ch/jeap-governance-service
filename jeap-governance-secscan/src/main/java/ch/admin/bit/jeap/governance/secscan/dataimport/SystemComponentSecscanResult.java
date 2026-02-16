package ch.admin.bit.jeap.governance.secscan.dataimport;

import ch.admin.bit.jeap.governance.domain.GovernanceServiceEnvironment;
import ch.admin.bit.jeap.governance.domain.SystemComponentReference;
import ch.admin.bit.jeap.governance.secscan.domain.SecscanFlaggedEndpoint;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.List;

record SystemComponentSecscanResult(ScanResultType resultType,
                                   ZonedDateTime scanTimestamp,
                                   SystemComponentReference systemComponentReference,
                                   GovernanceServiceEnvironment environment,
                                   String scanMessage,
                                   List<SecscanFlaggedEndpoint> flaggedEndpoints) {
    @Getter
    enum ScanResultType {
        NO_API(true),
        API_EMPTY(true),
        API_IGNORED(true),
        API_SCAN_DATA_STILL_UP_TO_DATE(false),
        API_SCANNED_OK(true),
        API_SCANNED_FLAGGED(true);

        private final boolean updateSecscanData; // does this result type require an update of the data in the database?

        ScanResultType(boolean updateSecscanData) {
            this.updateSecscanData = updateSecscanData;
        }
    }

}
