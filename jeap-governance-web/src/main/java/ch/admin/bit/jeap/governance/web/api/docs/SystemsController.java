package ch.admin.bit.jeap.governance.web.api.docs;

import ch.admin.bit.jeap.governance.web.api.docs.DocsApiReader.SystemsData;
import ch.admin.bit.jeap.governance.web.api.docs.exception.SystemNotFoundException;
import ch.admin.bit.jeap.governance.web.api.docs.model.SystemDto;
import ch.admin.bit.jeap.governance.web.api.docs.model.SystemIndexDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

@RestController
@RequestMapping(value = DocsApiPaths.SYSTEMS, produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@SecurityRequirement(name = "OIDC")
@Tag(name = "docs-api-systems", description = "The system and component scores, for the jEAP Doc Service")
public class SystemsController {

    private final DocsApiReader reader;
    private final DocsApiDtoFactory dtoFactory;
    private final DocsApiEtagSupport etagSupport;

    @GetMapping
    @RequiresGovernanceDocumentationRead
    @Operation(summary = "Every scored system with its score, trend and entity tag",
            description = "Also carries the time of the last completed scoring run and the trend period.")
    @ApiResponse(responseCode = "200", description = "The systems",
            content = @Content(schema = @Schema(implementation = SystemIndexDto.class)))
    @ApiResponse(responseCode = "304", description = "If-None-Match matched", content = @Content)
    @ApiResponse(responseCode = "401", description = "No or invalid token", content = @Content)
    @ApiResponse(responseCode = "403", description = "The token lacks the governance-documentation read role", content = @Content)
    public ResponseEntity<byte[]> getSystems(WebRequest request) {
        return etagSupport.respond(request, dtoFactory.systemIndex(reader.readSystems()));
    }

    @GetMapping("/{system}")
    @RequiresGovernanceDocumentationRead
    @Operation(summary = "One system with its score history and its components with their rule states",
            description = "The system is matched by its name as the governance service stores it.")
    @ApiResponse(responseCode = "200", description = "The system",
            content = @Content(schema = @Schema(implementation = SystemDto.class)))
    @ApiResponse(responseCode = "304", description = "If-None-Match matched", content = @Content)
    @ApiResponse(responseCode = "404", description = "No scored system of that name")
    public ResponseEntity<byte[]> getSystem(@PathVariable("system") String systemName, WebRequest request) {
        SystemsData data = reader.readSystems();
        SystemDto system = dtoFactory.system(data, systemName)
                .orElseThrow(() -> SystemNotFoundException.systemNotFound(systemName));
        return etagSupport.respond(request, system);
    }
}
