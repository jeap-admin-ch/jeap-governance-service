package ch.admin.bit.jeap.governance.secscan.apidiscovery;

import java.time.ZonedDateTime;
import java.util.List;

record RestApiResultDto (String serverUrl, ZonedDateTime lastUpdated, String version, List<RestApiDto> restApis){}
