package ch.admin.bit.jeap.governance.web.api.management;

import ch.admin.bit.jeap.governance.dataimport.DataImportScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static ch.admin.bit.jeap.governance.web.api.management.JobType.DATA_IMPORT;

@RestController
@RequestMapping("/api/management")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Management", description = "Manage the governance service")
public class ManagementController {

    @Autowired
    private DataImportScheduler dataImportScheduler;

    @PostMapping
    @Operation(
            summary = "Create a new job",
            description = "Create a new job to trigger rule update or imports",
            security = {@SecurityRequirement(name = "OIDC")}
    )
    @ApiResponse(responseCode = "200", description = "Job successfully created and started asynchronously")
    @Async
    @Transactional
    @PreAuthorize("hasRole('governance','admin')")
    public void triggerUpdate(@RequestBody JobDto jobDto) {
        if (jobDto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job type must be provided");
        }

        if (DATA_IMPORT.equals(jobDto.getType())) {
            dataImportScheduler.update();
        }
    }

}
