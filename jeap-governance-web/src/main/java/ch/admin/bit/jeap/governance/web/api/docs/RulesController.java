package ch.admin.bit.jeap.governance.web.api.docs;

import ch.admin.bit.jeap.governance.web.api.docs.exception.RuleNotFoundException;
import ch.admin.bit.jeap.governance.web.api.docs.model.RuleDto;
import ch.admin.bit.jeap.governance.web.api.docs.model.RuleIndexDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

@RestController
@RequestMapping(value = DocsApiPaths.RULES, produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@SecurityRequirement(name = "OIDC")
@Tag(name = "docs-api-rules", description = "The rules and their conformance, for the jEAP Doc Service")
public class RulesController {

    private final DocsApiReader reader;
    private final DocsApiDtoFactory dtoFactory;
    private final DocsApiEtagSupport etagSupport;

    @GetMapping
    @RequiresGovernanceDocumentationRead
    @Operation(summary = "Every rule the governance service knows, active or not",
            description = "Active rules with weight, share, conformance rate, trend and number of exemptions. Also "
                          + "carries the time of the last completed scoring run.")
    @ApiResponse(responseCode = "200", description = "The rules",
            content = @Content(schema = @Schema(implementation = RuleIndexDto.class)))
    @ApiResponse(responseCode = "304", description = "If-None-Match matched", content = @Content)
    @ApiResponse(responseCode = "401", description = "No or invalid token", content = @Content)
    @ApiResponse(responseCode = "403", description = "The token lacks the governance-documentation read role", content = @Content)
    public ResponseEntity<byte[]> getRules(WebRequest request) {
        return etagSupport.respond(request, dtoFactory.ruleIndex(reader.readRules()));
    }

    @GetMapping("/{ruleId}")
    @RequiresGovernanceDocumentationRead
    @Operation(summary = "One rule with its explanation, configuration, exemptions and conformance")
    @ApiResponse(responseCode = "200", description = "The rule",
            content = @Content(schema = @Schema(implementation = RuleDto.class)))
    @ApiResponse(responseCode = "304", description = "If-None-Match matched", content = @Content)
    @ApiResponse(responseCode = "404", description = "No rule with that ID",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<byte[]> getRule(@PathVariable("ruleId") String ruleId, WebRequest request) {
        RuleDto rule = dtoFactory.rule(reader.readRule(ruleId), ruleId)
                .orElseThrow(() -> RuleNotFoundException.ruleNotFound(ruleId));
        return etagSupport.respond(request, rule);
    }
}
