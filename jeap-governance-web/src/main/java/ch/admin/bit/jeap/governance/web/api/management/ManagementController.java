package ch.admin.bit.jeap.governance.web.api.management;

import ch.admin.bit.jeap.governance.dataimport.DataImportScheduler;
import ch.admin.bit.jeap.governance.rules.ScoringScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/management")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Management", description = "Manage the governance service")
public class ManagementController {

    private final DataImportScheduler dataImportScheduler;
    private final ScoringScheduler scoringScheduler;
    private final AsyncJobRunner asyncJobRunner;

    @PostMapping
    @Operation(
            summary = "Create a new job",
            description = "Starts a data import or a scoring run in the background, under the same lock as the scheduled job",
            security = {@SecurityRequirement(name = "OIDC")}
    )
    @ApiResponse(responseCode = "200", description = "Job started, it runs in the background")
    @ApiResponse(responseCode = "400", description = "No job type given")
    @ApiResponse(responseCode = "409", description = "Job not started: its lock is held, as the job is running on an " +
                                                     "instance or a run that did not end still holds it")
    @PreAuthorize("hasRole('governance','admin')")
    public void triggerUpdate(@RequestBody JobDto jobDto) {
        if (jobDto == null || jobDto.type() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job type must be provided");
        }

        boolean started = switch (jobDto.type()) {
            case DATA_IMPORT -> dataImportScheduler.startImport(asyncJobRunner::run);
            case SCORING -> scoringScheduler.startScoring(asyncJobRunner::run);
        };
        if (!started) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Job " + jobDto.type() + " not started: its lock is held, the job is already running");
        }
        log.info("Job {} started", jobDto.type());
    }
}
