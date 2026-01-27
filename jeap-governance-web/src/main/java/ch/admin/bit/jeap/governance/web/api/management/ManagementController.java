package ch.admin.bit.jeap.governance.web.api.management;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/management")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Management", description = "Manage the governance service")
public class ManagementController {


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
        log.info("Got {}, won't do anything a the moment.", jobDto);
    }

}
