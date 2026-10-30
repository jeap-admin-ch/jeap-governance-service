package ch.admin.bit.jeap.governance.web.api.docs;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.rule.RuleConformanceRate;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import ch.admin.bit.jeap.governance.domain.rule.RuleState;
import ch.admin.bit.jeap.governance.domain.rule.State;
import ch.admin.bit.jeap.governance.domain.rule.SystemRuleConformanceRate;
import ch.admin.bit.jeap.governance.domain.scheduler.SchedulerRunRepository;
import ch.admin.bit.jeap.governance.domain.score.ComponentScore;
import ch.admin.bit.jeap.governance.domain.score.SystemScore;
import ch.admin.bit.jeap.governance.reportdata.ReportingRulesPreparation;
import ch.admin.bit.jeap.governance.reportdata.ReportingSystemsPreparation;
import ch.admin.bit.jeap.governance.reporting.confluence.model.ComponentScoreReportModel;
import ch.admin.bit.jeap.governance.reporting.confluence.model.ModelTransformer;
import ch.admin.bit.jeap.governance.reporting.confluence.model.RuleReportModel;
import ch.admin.bit.jeap.governance.reporting.confluence.model.SystemScoreReportModel;
import ch.admin.bit.jeap.governance.web.api.docs.model.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.cfg.DateTimeFeature;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.util.ReflectionTestUtils.setField;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The docs API on test data in a real database: every field of every resource, the entity tags and conditional
 * requests, and that the values equal the ones of the Confluence reports, which are prepared from the same data.
 */
class DocsApiIT extends DocsApiTestBase {

    private static final String INACTIVE_RULE = "component-cdc-contractwithinsystem";
    private static final String INACTIVE_RULE_NAME = "Consumer Contract Within System";
    private static final LocalDate TODAY = LocalDate.now();
    private static final LocalDateTime SCORED_AT = LocalDateTime.of(2026, 10, 29, 10, 47, 12);
    private static final ZonedDateTime NOW = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    private static final ZonedDateTime B_FAILING_SINCE = NOW.minusDays(3);
    private static final ZonedDateTime A_VIOLATION_DETECTED_AT = NOW.minusDays(1);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private SchedulerRunRepository schedulerRunRepository;
    @Autowired
    private ReportingSystemsPreparation systemsPreparation;
    @Autowired
    private ReportingRulesPreparation rulesPreparation;

    @BeforeEach
    void createTestData() {
        transactionTemplate.executeWithoutResult(_ -> {
            deleteAll();
            persistTestData();
        });
        schedulerRunRepository.saveLastRunDateTime("scoring", SCORED_AT);
    }

    @Test
    void systemIndex_listsEveryScoredSystemWithTheTimeOfTheLastScoringRun() throws Exception {
        SystemIndexDto index = read("/docs-api/systems", SystemIndexDto.class);

        assertThat(index.scoredAt()).isEqualTo(SCORED_AT.atZone(ZoneId.systemDefault()).toOffsetDateTime());
        assertThat(index.trendPeriodDays()).isEqualTo(30);
        // The system "other" has no score and is not listed
        assertThat(index.systems()).singleElement().satisfies(entry -> {
            assertThat(entry.name()).isEqualTo("jme");
            assertThat(entry.score()).isEqualTo(65);
            assertThat(entry.trend()).isEqualTo(Trend.DOWN);
            assertThat(entry.url()).isEqualTo("/docs-api/systems/jme");
            assertThat(entry.etag()).startsWith("\"sha256:").endsWith("\"");
        });
    }

    @Test
    void system_deliversScoresHistoriesAndEveryRuleState() throws Exception {
        SystemDto system = read("/docs-api/systems/jme", SystemDto.class);

        assertThat(system.name()).isEqualTo("jme");
        assertThat(system.score()).isEqualTo(65);
        assertThat(system.trend()).isEqualTo(Trend.DOWN);
        assertThat(system.history()).containsExactly(
                new DailyScoreDto(TODAY.minusDays(2), 80),
                new DailyScoreDto(TODAY.minusDays(1), 70),
                new DailyScoreDto(TODAY, 65));
        // The gateway is ignored for governance
        assertThat(system.components()).extracting(ComponentDto::name).containsExactly("jme-a-service", "jme-b-service");

        ComponentDto a = system.components().getFirst();
        assertThat(a.score()).isEqualTo(90);
        assertThat(a.trend()).isEqualTo(Trend.STABLE);
        assertThat(a.history()).hasSize(3);
        assertThat(a.ruleStates()).containsExactly(
                new RuleStateDto(RULE_A, "Docs Rule A", RuleStateValue.OK, null, List.of(), offset(NOW)),
                new RuleStateDto(RULE_B, "Docs Rule B", RuleStateValue.OK, "Outdated message contracts:",
                        List.of("FirstEvent uses 1.0.0, latest is 2.0.0", "SecondEvent uses 1.1.0, latest is 3.0.0"),
                        offset(A_VIOLATION_DETECTED_AT)));

        ComponentDto b = system.components().get(1);
        assertThat(b.score()).isEqualTo(40);
        assertThat(b.trend()).isEqualTo(Trend.UNKNOWN);
        assertThat(b.history()).containsExactly(new DailyScoreDto(TODAY, 40));
        assertThat(b.ruleStates()).containsExactly(
                new RuleStateDto("component-naming-convention", "Component Naming Convention", RuleStateValue.PAUSED,
                        "Paused until the migration", List.of(), offset(NOW)),
                new RuleStateDto(RULE_A, "Docs Rule A", RuleStateValue.FAIL, "Unprotected endpoints:",
                        List.of("'GET /a'", "'POST /b'"), offset(B_FAILING_SINCE)),
                new RuleStateDto(RULE_B, "Docs Rule B", RuleStateValue.DISABLED, "**not markdown** <b>nor html</b>",
                        List.of(), offset(NOW)));
    }

    @Test
    void unknownSystem_isNotFoundAsProblemDocument() throws Exception {
        mockMvc.perform(get("/docs-api/systems/unknown").with(authentication(tokenWithDocumentationRead())))
                .andExpect(status().isNotFound());
    }

    @Test
    void ruleIndex_listsEveryKnownRuleWithWeightShareAndConformance() throws Exception {
        RuleIndexDto index = read("/docs-api/rules", RuleIndexDto.class);

        assertThat(index.scoredAt()).isEqualTo(SCORED_AT.atZone(ZoneId.systemDefault()).toOffsetDateTime());
        // The rules of the modules this test runs with, and the two of this test
        assertThat(index.rules()).extracting(RuleIndexEntryDto::ruleId).containsExactly(
                "component-cdc-contractbetweensystems", "component-cdc-contractwithinsystem",
                "component-naming-convention", RULE_A, RULE_B);
        assertThat(index.rules()).extracting(RuleIndexEntryDto::ruleId).isSorted();

        assertThat(entry(index, RULE_A)).isEqualTo(new RuleIndexEntryDto(RULE_A, "Docs Rule A", true, 10,
                new BigDecimal("50.0"), 60, Trend.DOWN, 3, entry(index, RULE_A).etag(), "/docs-api/rules/" + RULE_A));
        assertThat(entry(index, RULE_B)).isEqualTo(new RuleIndexEntryDto(RULE_B, "Docs Rule B", true, 5,
                new BigDecimal("25.0"), 50, Trend.UNKNOWN, 1, entry(index, RULE_B).etag(), "/docs-api/rules/" + RULE_B));
        // Active, but not scored yet
        assertThat(entry(index, "component-naming-convention")).satisfies(rule -> {
            assertThat(rule.active()).isTrue();
            assertThat(rule.share()).isEqualByComparingTo("25");
            assertThat(rule.conformanceRate()).isNull();
            assertThat(rule.trend()).isEqualTo(Trend.NO_DATA);
            assertThat(rule.exemptionCount()).isZero();
        });
        assertThat(entry(index, INACTIVE_RULE)).isEqualTo(new RuleIndexEntryDto(INACTIVE_RULE,
                INACTIVE_RULE_NAME, false, null, null, null, null, null,
                entry(index, INACTIVE_RULE).etag(), "/docs-api/rules/" + INACTIVE_RULE));

        assertThat(index.rules().stream().map(RuleIndexEntryDto::share).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("100");
    }

    @Test
    void rule_deliversExplanationExemptionsAndConformance() throws Exception {
        String body = readBody("/docs-api/rules/" + RULE_A);
        RuleDto rule = readValue(body, RuleDto.class);

        assertThat(rule.ruleId()).isEqualTo(RULE_A);
        assertThat(rule.name()).isEqualTo("Docs Rule A");
        assertThat(rule.active()).isTrue();
        assertThat(rule.explanation()).isEqualTo("""
                Checks something important.

                <b>Raw HTML</b> stays as written.
                """);
        assertThat(rule.weight()).isEqualTo(10);
        assertThat(rule.share()).isEqualByComparingTo("50");
        assertThat(rule.violationDelay()).isEqualTo("PT0S");
        assertThat(rule.gracePeriodConfigured()).isFalse();
        assertThat(rule.parameters()).isEmpty();
        assertThat(rule.exemptions()).containsExactly(
                new ExemptionDto("exemption-both", "jme-b-service", "Exempted from both rules", null,
                        ExemptionEffect.DISABLED, List.of()),
                new ExemptionDto("exemption-paused", "jme-c-service", "Migrating", LocalDate.of(2099, 12, 31),
                        ExemptionEffect.PAUSED, List.of()),
                new ExemptionDto("exemption-expired", "jme-d-service", "Was migrating", LocalDate.of(2020, 1, 1),
                        ExemptionEffect.EXPIRED, List.of(new ParameterDto("scope", "internal"))));
        assertThat(rule.conformanceRate()).isEqualTo(60);
        assertThat(rule.trend()).isEqualTo(Trend.DOWN);
        assertThat(rule.systems()).containsExactly(new SystemConformanceDto("jme", 70));
        assertThat(rule.nonCompliantComponents()).containsExactly(
                new NonCompliantComponentDto("jme", "jme-b-service", B_FAILING_SINCE.toLocalDate()));
        assertThat(rule.gracePeriodComponents()).isEmpty();
        // The deprecated documentation link of the configuration is not delivered
        assertThat(body).doesNotContain("documentation", "wiki.example.com");
    }

    @Test
    void ruleWithGracePeriod_deliversTheComponentsWithinIt() throws Exception {
        RuleDto rule = read("/docs-api/rules/" + RULE_B, RuleDto.class);

        assertThat(rule.explanation()).isNull();
        assertThat(rule.violationDelay()).isEqualTo(Duration.ofDays(7).toString());
        assertThat(rule.gracePeriodConfigured()).isTrue();
        assertThat(rule.parameters()).containsExactly(
                new ParameterDto("exempt-paths", "/api/public"),
                new ParameterDto("ignored-service-names", "jme-legacy-service"));
        // The exemption naming two rules is listed under both
        assertThat(rule.exemptions()).extracting(ExemptionDto::id).containsExactly("exemption-both");
        assertThat(rule.gracePeriodComponents()).containsExactly(new GracePeriodComponentDto("jme", "jme-a-service",
                "Outdated message contracts:",
                List.of("FirstEvent uses 1.0.0, latest is 2.0.0", "SecondEvent uses 1.1.0, latest is 3.0.0"),
                offset(A_VIOLATION_DETECTED_AT), offset(A_VIOLATION_DETECTED_AT.plusDays(7))));
    }

    @Test
    void inactiveRule_deliversItsNameAndExplanationOnly() throws Exception {
        RuleDto rule = read("/docs-api/rules/" + INACTIVE_RULE, RuleDto.class);

        assertThat(rule.active()).isFalse();
        assertThat(rule.name()).isEqualTo(INACTIVE_RULE_NAME);
        // A built-in rule ships its explanation
        assertThat(rule.explanation()).isNotBlank();
        assertThat(rule).isEqualTo(new RuleDto(INACTIVE_RULE, INACTIVE_RULE_NAME, false,
                rule.explanation(), null, null, null, false, List.of(), List.of(), null, null, List.of(), List.of(),
                List.of()));
    }

    @Test
    void unknownRule_isNotFound() throws Exception {
        mockMvc.perform(get("/docs-api/rules/unknown").with(authentication(tokenWithDocumentationRead())))
                .andExpect(status().isNotFound());
    }

    @Test
    void everyIndexEntry_carriesTheEntityTagOfItsResource() throws Exception {
        for (SystemIndexEntryDto system : read("/docs-api/systems", SystemIndexDto.class).systems()) {
            assertThat(etagOf(system.url())).isEqualTo(system.etag());
        }
        for (RuleIndexEntryDto rule : read("/docs-api/rules", RuleIndexDto.class).rules()) {
            assertThat(etagOf(rule.url())).isEqualTo(rule.etag());
        }
    }

    @Test
    void everyResource_isStronglyTaggedAndNotModifiedForAMatchingIfNoneMatch() throws Exception {
        for (String path : List.of("/docs-api/systems", "/docs-api/systems/jme", "/docs-api/rules",
                "/docs-api/rules/" + RULE_A)) {
            MvcResult first = mockMvc.perform(get(path).with(authentication(tokenWithDocumentationRead())))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andReturn();
            String etag = first.getResponse().getHeader(HttpHeaders.ETAG);
            assertThat(etag).matches("\"sha256:[0-9a-f]{64}\"");

            MvcResult notModified = mockMvc.perform(get(path)
                            .header(HttpHeaders.IF_NONE_MATCH, etag)
                            .with(authentication(tokenWithDocumentationRead())))
                    .andExpect(status().isNotModified())
                    .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache"))
                    .andReturn();
            assertThat(notModified.getResponse().getContentAsByteArray()).isEmpty();
        }
    }

    @Test
    void aChangedResource_isDeliveredWithANewEntityTag() throws Exception {
        String etagBefore = etagOf("/docs-api/systems/jme");
        String rulesEtagBefore = etagOf("/docs-api/rules/" + RULE_B);

        transactionTemplate.executeWithoutResult(_ -> entityManager.persist(ComponentScore.builder()
                .systemComponent(findComponent("jme-b-service")).score(30).day(TODAY.minusDays(1)).build()));

        mockMvc.perform(get("/docs-api/systems/jme")
                        .header(HttpHeaders.IF_NONE_MATCH, etagBefore)
                        .with(authentication(tokenWithDocumentationRead())))
                .andExpect(status().isOk());
        assertThat(etagOf("/docs-api/systems/jme")).isNotEqualTo(etagBefore);
        // The scoring time is part of the indexes only, so a resource is not changed by it
        schedulerRunRepository.saveLastRunDateTime("scoring", SCORED_AT.plusMinutes(45));
        assertThat(etagOf("/docs-api/rules/" + RULE_B)).isEqualTo(rulesEtagBefore);
    }

    @Test
    void theValuesEqualThoseOfTheConfluenceReports() throws Exception {
        ModelTransformer confluence = new ModelTransformer("", "");
        SystemScoreReportModel confluenceSystem = systemsPreparation.prepareAllSystemsScores(TODAY.minusDays(30), TODAY)
                .stream().map(confluence::toConfluenceModel).findFirst().orElseThrow();
        SystemDto system = read("/docs-api/systems/jme", SystemDto.class);

        assertThat(system.score()).isEqualTo(confluenceSystem.getScore());
        assertThat(system.trend().name()).isEqualTo(trendName(confluenceSystem.getTrend().name()));
        assertThat(system.history()).extracting(DailyScoreDto::score)
                .containsExactlyElementsOf(confluenceSystem.getScores().stream().map(s -> s.getScore()).toList());
        for (ComponentScoreReportModel confluenceComponent : confluenceSystem.getComponentScores()) {
            ComponentDto component = system.components().stream()
                    .filter(c -> c.name().equals(confluenceComponent.getComponentName())).findFirst().orElseThrow();
            assertThat(component.score()).isEqualTo(confluenceComponent.getScore());
            assertThat(component.trend().name()).isEqualTo(trendName(confluenceComponent.getTrend().name()));
            assertThat(component.ruleStates()).extracting(RuleStateDto::ruleId).containsExactlyInAnyOrderElementsOf(
                    confluenceComponent.getRuleStates().stream().map(s -> s.getRuleId()).toList());
        }

        for (RuleReportModel confluenceRule : rulesPreparation.prepareAllRules(TODAY.minusDays(30), TODAY).stream()
                .map(confluence::toConfluenceModel).toList()) {
            RuleDto rule = read("/docs-api/rules/" + confluenceRule.getRuleId(), RuleDto.class);
            assertThat(rule.name()).isEqualTo(confluenceRule.getName());
            assertThat(rule.conformanceRate()).isEqualTo(confluenceRule.getConformanceRate());
            assertThat(rule.trend().name()).isEqualTo(trendName(confluenceRule.getTrend().name()));
            assertThat(rule.gracePeriodConfigured()).isEqualTo(confluenceRule.isViolationGracePeriodConfigured());
            assertThat(rule.systems()).extracting(SystemConformanceDto::conformanceRate).containsExactlyInAnyOrderElementsOf(
                    confluenceRule.getSystems().stream().map(s -> s.getConformanceRate()).toList());
            assertThat(rule.nonCompliantComponents()).extracting(NonCompliantComponentDto::component)
                    .containsExactlyInAnyOrderElementsOf(confluenceRule.getNonCompliantComponents().stream().map(c -> c.getName()).toList());
            assertThat(rule.gracePeriodComponents()).extracting(GracePeriodComponentDto::component)
                    .containsExactlyInAnyOrderElementsOf(confluenceRule.getGracePeriodComponents().stream().map(c -> c.getName()).toList());
        }
    }

    /**
     * The Confluence reports call a stable trend "even".
     */
    private static String trendName(String confluenceTrend) {
        return confluenceTrend.equals("EVEN") ? "STABLE" : confluenceTrend;
    }

    private <T> T read(String path, Class<T> type) throws Exception {
        return readValue(readBody(path), type);
    }

    /**
     * Keeps the offsets as delivered, instead of adjusting them to UTC, so that they can be compared as records.
     */
    private <T> T readValue(String body, Class<T> type) {
        return objectMapper.rebuild()
                .disable(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .build()
                .readValue(body, type);
    }

    private String readBody(String path) throws Exception {
        return mockMvc.perform(get(path).with(authentication(tokenWithDocumentationRead())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String etagOf(String path) throws Exception {
        return mockMvc.perform(get(path).with(authentication(tokenWithDocumentationRead())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(HttpHeaders.ETAG);
    }

    private static RuleIndexEntryDto entry(RuleIndexDto index, String ruleId) {
        return index.rules().stream().filter(rule -> rule.ruleId().equals(ruleId)).findFirst().orElseThrow();
    }

    private static OffsetDateTime offset(ZonedDateTime dateTime) {
        return dateTime.withZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private SystemComponent findComponent(String name) {
        return entityManager.createQuery("SELECT c FROM SystemComponent c WHERE c.name = :name", SystemComponent.class)
                .setParameter("name", name).getSingleResult();
    }

    private void persistTestData() {
        SystemComponent a = SystemComponent.builder().name("jme-a-service").type(ComponentType.BACKEND_SERVICE).build();
        SystemComponent b = SystemComponent.builder().name("jme-b-service").type(ComponentType.SELF_CONTAINED_SYSTEM).build();
        SystemComponent gateway = SystemComponent.builder().name("jme-edge-gateway").type(ComponentType.GATEWAY).build();
        System jme = System.builder().name("jme").systemComponents(List.of(a, b, gateway)).aliases(Set.of()).build();
        entityManager.persist(jme);
        SystemComponent otherComponent = SystemComponent.builder().name("other-x-service").type(ComponentType.BACKEND_SERVICE).build();
        entityManager.persist(System.builder().name("other").systemComponents(List.of(otherComponent)).aliases(Set.of()).build());

        entityManager.persist(SystemScore.builder().system(jme).score(80).day(TODAY.minusDays(2)).build());
        entityManager.persist(SystemScore.builder().system(jme).score(70).day(TODAY.minusDays(1)).build());
        entityManager.persist(SystemScore.builder().system(jme).score(65).day(TODAY).build());
        for (int daysAgo = 2; daysAgo >= 0; daysAgo--) {
            entityManager.persist(ComponentScore.builder().systemComponent(a).score(90).day(TODAY.minusDays(daysAgo)).build());
            entityManager.persist(ComponentScore.builder().systemComponent(gateway).score(10).day(TODAY.minusDays(daysAgo)).build());
        }
        entityManager.persist(ComponentScore.builder().systemComponent(b).score(40).day(TODAY).build());

        entityManager.persist(RuleConformanceRate.builder().ruleId(RULE_A).conformanceRate(80).day(TODAY.minusDays(1)).build());
        entityManager.persist(RuleConformanceRate.builder().ruleId(RULE_A).conformanceRate(60).day(TODAY).build());
        entityManager.persist(RuleConformanceRate.builder().ruleId(RULE_B).conformanceRate(50).day(TODAY).build());
        entityManager.persist(SystemRuleConformanceRate.builder().systemId(jme.getId()).ruleId(RULE_A).conformanceRate(70).day(TODAY).build());
        entityManager.persist(SystemRuleConformanceRate.builder().systemId(jme.getId()).ruleId(RULE_B).conformanceRate(100).day(TODAY).build());

        persistRuleState(a, RULE_A, State.OK, null, NOW, null);
        persistRuleState(a, RULE_B, State.OK, "Outdated message contracts:\nFirstEvent uses 1.0.0, latest is 2.0.0\n\nSecondEvent uses 1.1.0, latest is 3.0.0",
                A_VIOLATION_DETECTED_AT, A_VIOLATION_DETECTED_AT);
        persistRuleState(b, RULE_A, State.FAIL, "Unprotected endpoints:\n'GET /a'\n'POST /b'", B_FAILING_SINCE, null);
        persistRuleState(b, RULE_B, State.DISABLED, "**not markdown** <b>nor html</b>", NOW, null);
        persistRuleState(b, "component-naming-convention", State.PAUSED, "Paused until the migration", NOW, null);
        persistRuleState(gateway, RULE_A, State.FAIL, "Ignored for governance", NOW, null);
        entityManager.flush();
    }

    private void persistRuleState(SystemComponent component, String ruleId, State state, String comment,
                                  ZonedDateTime modifiedAt, ZonedDateTime violationDetectedAt) {
        RuleState ruleState = RuleState.builder().systemComponent(component).ruleId(RuleId.of(ruleId)).state(state)
                .ruleStateComment(comment).build();
        setField(ruleState, "modifiedAt", modifiedAt);
        setField(ruleState, "violationDetectedAt", violationDetectedAt);
        entityManager.persist(ruleState);
    }

    private void deleteAll() {
        for (String table : List.of("rule_state", "rule_conformance_rate", "system_rule_conformance_rate",
                "component_score", "system_score", "system_component", "system_aliases", "system")) {
            entityManager.createNativeQuery("DELETE FROM " + table).executeUpdate();
        }
    }
}
