package ch.admin.bit.jeap.governance.web.api.docs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every resource of the docs API is behind the role {@code governance-documentation} / {@code read}, on an instance
 * configuring nothing for the Doc Service, and {@code /api/management} behaves as before.
 */
class DocsApiSecurityIT extends DocsApiTestBase {

    private static final String SCORING_JOB = "{\"type\":\"SCORING\"}";

    @Autowired
    private MockMvc mockMvc;

    static Stream<String> everyDocsApiRoute() {
        return Stream.of("/docs-api/systems", "/docs-api/systems/jme", "/docs-api/rules", "/docs-api/rules/docs-rule-a");
    }

    @ParameterizedTest(name = "{0} without a token is 401")
    @MethodSource("everyDocsApiRoute")
    void withoutAToken_isUnauthorized(String route) throws Exception {
        mockMvc.perform(get(route))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{0} with an invalid token is 401")
    @MethodSource("everyDocsApiRoute")
    void withAnInvalidToken_isUnauthorized(String route) throws Exception {
        mockMvc.perform(get(route).header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{0} with a token without roles is 403")
    @MethodSource("everyDocsApiRoute")
    void withoutTheRole_isForbidden(String route) throws Exception {
        mockMvc.perform(get(route).with(authentication(tokenWithoutRoles())))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0} with governance-documentation/write is 403")
    @MethodSource("everyDocsApiRoute")
    void withTheWrongOperation_isForbidden(String route) throws Exception {
        mockMvc.perform(get(route).with(authentication(tokenWithDocumentationWrite())))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0} with the role of another system is 403")
    @MethodSource("everyDocsApiRoute")
    void withTheRoleOfAnotherSystem_isForbidden(String route) throws Exception {
        mockMvc.perform(get(route).with(authentication(tokenOfAnotherSystem())))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0} with the management role is 403")
    @MethodSource("everyDocsApiRoute")
    void withTheManagementRole_isForbidden(String route) throws Exception {
        mockMvc.perform(get(route).with(authentication(tokenWithGovernanceAdmin())))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0} with governance-documentation/read passes")
    @MethodSource("everyDocsApiRoute")
    void withTheRole_isAuthorized(String route) throws Exception {
        // The database may be empty, so a resource naming a system or rule may answer 404; never 401, 403 or 5xx
        mockMvc.perform(get(route).with(authentication(tokenWithDocumentationRead())))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (!(status == 200 || status == 404)) {
                        throw new AssertionError("Expected 200 or 404 for " + route + ", but was " + status);
                    }
                });
    }

    // The management API stays on the chain of the jEAP security starter. A request with a bearer token passes
    // its CSRF protection; the tokens below are set without one, so they carry a CSRF token instead.

    @Test
    void management_withAnInvalidToken_isStillUnauthorized() throws Exception {
        mockMvc.perform(post("/api/management").contentType(MediaType.APPLICATION_JSON).content(SCORING_JOB)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void management_withTheDocsApiRole_isForbidden() throws Exception {
        mockMvc.perform(post("/api/management").contentType(MediaType.APPLICATION_JSON).content(SCORING_JOB)
                        .with(authentication(tokenWithDocumentationRead())).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void management_withTheManagementRole_startsTheJob() throws Exception {
        mockMvc.perform(post("/api/management").contentType(MediaType.APPLICATION_JSON).content(SCORING_JOB)
                        .with(authentication(tokenWithGovernanceAdmin())).with(csrf()))
                .andExpect(status().isOk());
    }
}
