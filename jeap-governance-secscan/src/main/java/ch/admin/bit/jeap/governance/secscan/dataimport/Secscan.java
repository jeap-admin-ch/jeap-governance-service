package ch.admin.bit.jeap.governance.secscan.dataimport;

import ch.admin.bit.jeap.governance.domain.GovernanceServiceEnvironment;
import ch.admin.bit.jeap.governance.domain.SystemComponentReference;
import ch.admin.bit.jeap.governance.domain.SystemComponentRepository;
import ch.admin.bit.jeap.governance.domain.plugin.datasource.DataSourceImporter;
import ch.admin.bit.jeap.governance.secscan.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ch.admin.bit.jeap.governance.secscan.dataimport.SystemComponentSecscanResult.ScanResultType.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class Secscan implements DataSourceImporter {

    private final DataimportConfigurationProperties configProperties;
    private final SystemComponentRepository systemComponentRepository;
    private final SystemComponentHttpApiDiscoveryClient apiDiscoveryClient;
    private final SystemComponentHttpApiIgnoreFilter apiFilter;
    private final HttpEndpointSecurityChecker endpointSecurityChecker;
    private final SecscanTransactions secscanTransactions;
    private final SecscanFlaggedEndpointRepository flaggedEndpointRepository;
    private final SecscanStateRepository secscanStateRepository;

    @Override
    public void importData() {
        GovernanceServiceEnvironment environment = configProperties.getTargetEnvironment();
        log.info("Starting the security scan of the system components in the environment '{}'.", environment);
        systemComponentRepository.findAllSystemComponentReferences().forEach(componentRef -> {
            String systemComponentName = componentRef.getName();
            try {
                SystemComponentSecscanResult result = scanSystemComponent(componentRef, environment);
                log.info("Finished the security scan of the system component '{}' in the environment '{}'. Result: {}",
                        systemComponentName, environment, result.scanMessage());
                updateSecscanData(result);
            } catch (Exception e) {
                log.error("Failed to scan and update the system component '{}' in the environment '{}'. " +
                        "Leaving the existing security scan data unchanged.", systemComponentName, environment, e);
            }
        });
        log.info("Finished the security scan of the system components in the environment '{}'.", environment);
    }

    private void updateSecscanData(SystemComponentSecscanResult result) {
        secscanTransactions.inNewTransaction(() -> {
            if (result.resultType().isUpdateSecscanData()) {
                log.info("Updating the security scan data for the system component '{}' in the environment '{}'.",
                        result.systemComponentReference().getName(), result.environment());
                updateSecscanStateForSystemComponent(result);
                updateFlaggedEnpointsForSystemComponent(result);
            } else {
                log.info("Security scan data for the system component '{}' in the environment is '{}' unchanged.",
                        result.systemComponentReference().getName(), result.environment());
            }
        });
    }

    private void updateSecscanStateForSystemComponent(SystemComponentSecscanResult result) {
        long systemComponentId = result.systemComponentReference().getId();
        String systemComponentName = result.systemComponentReference().getName();
        log.debug("Updating security scan state for the system component '{}'.", systemComponentName);
        // Assuming isolation level READ COMMITTED for queries to the repository
        log.info("Deleting the current security scan state for the system component '{}'.", systemComponentName);
        secscanStateRepository.deleteBySystemComponentId(systemComponentId);
        log.info("Writing the security scan state for the system component '{}'.", systemComponentName);
        SecscanState newSecscanState = createSecScanState(systemComponentId, result.scanMessage(), result.scanTimestamp());
        secscanStateRepository.save(newSecscanState);
        log.debug("Updated the scurity scan state for the system component '{}'.", systemComponentName);
    }

    private void updateFlaggedEnpointsForSystemComponent(SystemComponentSecscanResult result) {
        long systemComponentId = result.systemComponentReference().getId();
        String systemComponentName = result.systemComponentReference().getName();
        List<SecscanFlaggedEndpoint> flaggedEndpoints = result.flaggedEndpoints();
        log.debug("Updating security scan data for the system component '{}' with {} flagged endpoints.", systemComponentName, flaggedEndpoints.size());
        // Assuming isolation level READ COMMITTED for queries to the repository
        log.info("Deleting all flagged endpoints for the system component '{}'.", systemComponentName);
        flaggedEndpointRepository.deleteBySystemComponentId(systemComponentId);
        if (!flaggedEndpoints.isEmpty()) {
            log.info("Writing {} new flagged endpoints for the system component '{}'.", flaggedEndpoints.size(), systemComponentName);
            flaggedEndpointRepository.saveAll(flaggedEndpoints);
        }
        log.debug("Updated the flagged endpoints for the system component '{}'.", systemComponentName);
    }

    private SystemComponentSecscanResult scanSystemComponent(SystemComponentReference componentRef, GovernanceServiceEnvironment environment) {
        String systemComponentName = componentRef.getName();
        SystemComponentHttpApi systemComponentApi = apiDiscoveryClient.discover(systemComponentName, environment);
        ZonedDateTime scanTimestamp = ZonedDateTime.now();
        return hasNoApi(systemComponentApi, componentRef, environment, scanTimestamp).or(() ->
                isApiEmpty(systemComponentApi, componentRef, scanTimestamp)).or(() ->
                isApiFilteredOut(systemComponentApi, componentRef, scanTimestamp)).or(() ->
                isApiScanDataUpToDate(systemComponentApi, componentRef, scanTimestamp)).or(() ->
                scanEndpoints(systemComponentApi, componentRef, scanTimestamp)).
                orElseThrow(() -> new IllegalStateException("The endpoint scan should always return a result."));
    }

    private Optional<SystemComponentSecscanResult> hasNoApi(SystemComponentHttpApi systemComponentApi,
                                                            SystemComponentReference componentRef,
                                                            GovernanceServiceEnvironment environment,
                                                            ZonedDateTime scanTimestamp) {
        if ((systemComponentApi == null) || (systemComponentApi.httpApi() == null)) {
            String message = """
                     No HTTP API found for the system component '%s' in the environment '%s' for the HTTP API security scan.
                     Skipping security scan.""".formatted(componentRef.getName(), environment);
            log.info(message);
            return Optional.of(new SystemComponentSecscanResult(
                    NO_API, scanTimestamp, componentRef, environment, message, List.of()));
        } else {
            return Optional.empty();
        }
    }

    private Optional<SystemComponentSecscanResult> isApiEmpty(SystemComponentHttpApi systemComponentApi,
                                                              SystemComponentReference componentRef,
                                                              ZonedDateTime scanTimestamp) {
        if ((systemComponentApi.httpApi().endpoints() == null) || systemComponentApi.httpApi().endpoints().isEmpty()) {
            String message = """
                     No HTTP endpoints found in the HTTP API for the system component '%s' in the environment
                     '%s' to check in the HTTP API security scan. Skipping security scan.
                     """.formatted(systemComponentApi.systemComponentName(), systemComponentApi.environment());
            log.info(message);
            return Optional.of(new SystemComponentSecscanResult(
                    API_EMPTY, scanTimestamp, componentRef, systemComponentApi.environment(), message, List.of()));
        } else {
            return Optional.empty();
        }
    }

    private Optional<SystemComponentSecscanResult> isApiFilteredOut(SystemComponentHttpApi systemComponentApi,
                                                                    SystemComponentReference componentRef,
                                                                    ZonedDateTime scanTimestamp) {
        var shouldIgnoreApi = apiFilter.shouldIgnoreApi(systemComponentApi);
        if (shouldIgnoreApi.ignore()) {
            String message = """
                     Ignoring the system component '%s' in the environment '%s' in the HTTP API security scan. Reason: %s
                     """.formatted(systemComponentApi.systemComponentName(), systemComponentApi.environment(), shouldIgnoreApi.reason());
            log.info(message);
            return Optional.of(new SystemComponentSecscanResult(
                    API_IGNORED, scanTimestamp, componentRef, systemComponentApi.environment(), message, List.of()));
        } else {
            return Optional.empty();
        }
    }

    private Optional<SystemComponentSecscanResult> isApiScanDataUpToDate(SystemComponentHttpApi systemComponentApi,
                                                                         SystemComponentReference componentRef,
                                                                         ZonedDateTime scanTimestamp) {
        if (systemComponentApi.lastUpdated() != null) {
            Optional<SecscanState> currentState = secscanStateRepository.findBySystemComponentId(componentRef.getId());
            if (currentState.isPresent() && currentState.get().getScanTimestamp().isAfter(systemComponentApi.lastUpdated())) {
                String message = """
                     The security scan data of the system component '%s' for the environment '%s' is still up to date.
                     Last API update: %s, last security scan: %s. Skipping security scan.
                     """.formatted(systemComponentApi.systemComponentName(), systemComponentApi.environment(),
                        systemComponentApi.lastUpdated(), currentState.get().getScanTimestamp());
                log.info(message);
                return Optional.of(new SystemComponentSecscanResult(
                        API_SCAN_DATA_STILL_UP_TO_DATE, scanTimestamp, componentRef, systemComponentApi.environment(), message, List.of()));
            }
        }
        return Optional.empty();
    }

    private Optional<SystemComponentSecscanResult> scanEndpoints(SystemComponentHttpApi systemComponentApi,
                                                                  SystemComponentReference componentRef,
                                                                  ZonedDateTime scanTimestamp) {
        HttpApi httpApi = systemComponentApi.httpApi();
        String systemComponentName = systemComponentApi.systemComponentName();
        GovernanceServiceEnvironment environment = systemComponentApi.environment();
        List<SecscanFlaggedEndpoint> flaggedEndpoints = new ArrayList<>();
        for (HttpEndpoint endpoint : httpApi.endpoints()) {
            var shouldIgnoreEndpoint = apiFilter.shouldIgnoreEndpoint(systemComponentName, endpoint);
            if (shouldIgnoreEndpoint.ignore()) {
                log.info("Ignoring the endpoint '{}' of the system component '{}' in the environment '{}' in the HTTP API security scan. Reason: {}",
                        endpoint, systemComponentName, environment, shouldIgnoreEndpoint.reason());
                continue;
            }
            var result = checkSecurity(systemComponentName, httpApi, endpoint);
            if (result.failed()) {
                log.info("Flagging the endpoint '{}' of the system component '{}' in the environment '{}' in the HTTP API security scan. Reason: {}",
                        endpoint, systemComponentName, environment, result.reason());
                SecscanFlaggedEndpoint flaggedEndpoint = createFlaggedEndpoint(componentRef.getId(), endpoint, result.reason());
                flaggedEndpoints.add(flaggedEndpoint);
            }
        }
        if (flaggedEndpoints.isEmpty()) {
            String message = "Security scan for the system component '%s' in the environment '%s' did not flag any endpoint."
                    .formatted(systemComponentName, environment);
            return Optional.of(new SystemComponentSecscanResult(
                    API_SCANNED_OK, scanTimestamp, componentRef, environment, message, flaggedEndpoints));
        } else {
            String message = "Security scan for the system component '%s' in the environment '%s' flagged %s endpoints."
                    .formatted(systemComponentName, environment, flaggedEndpoints.size());
            return Optional.of(new SystemComponentSecscanResult(
                    API_SCANNED_FLAGGED, scanTimestamp, componentRef, environment, message, flaggedEndpoints));
        }
    }

    private HttpEndpointSecurityChecker.Result checkSecurity(String systemComponentName, HttpApi httpApi, HttpEndpoint endpoint) {
        log.debug("Checking security for the system component '{}' and the endpoint '{}'.", systemComponentName, endpoint);
        try {
            return endpointSecurityChecker.check(httpApi.url(), endpoint);
        } catch (Exception e) {
            log.warn("Failed to check the security for the system component '{}' and endpoint '{}' because of '{}'.",
                    systemComponentName, endpoint, e.getMessage(), e);
            return  new HttpEndpointSecurityChecker.Result(
                    false, "Not flagging the endpoint, the check was not able to access it at all.");
        }
    }

    private SecscanFlaggedEndpoint createFlaggedEndpoint(long systemComponentId, HttpEndpoint endpoint, String scanMessage) {
        return SecscanFlaggedEndpoint.builder()
                .systemComponentId(systemComponentId)
                .path(endpoint.path())
                .method(endpoint.method())
                .scanMessage(scanMessage)
                .scanTimestamp(ZonedDateTime.now())
                .build();
    }

    private SecscanState createSecScanState(long systemComponentId, String scanMessage, ZonedDateTime scanTimestamp) {
        return SecscanState.builder()
                .systemComponentId(systemComponentId)
                .scanMessage(scanMessage)
                .scanTimestamp(scanTimestamp)
                .build();
    }

}
