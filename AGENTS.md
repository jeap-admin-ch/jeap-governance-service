# AGENTS.md

This file provides guidance to coding agents when working with code in this repository.

## Project Overview

jEAP Governance Service is a library for building governance dashboards that track system and service compliance with
defined policies. It imports data from external sources (Architecture Repository, Deployment Log, Prometheus, Reaction
Observer, security API scans) on a schedule, evaluates governance rules against components, calculates compliance
scores, persists results in PostgreSQL, exposes them via REST API, and reports trends to Confluence. Downstream projects
create their own service instances by depending on `jeap-governance-service-instance` and implementing plugins via the
plugin API.

Built on Java 25 and the `jeap-spring-boot-parent` (Spring Boot 4).

## Build Commands

```bash
# Build entire project
./mvnw install

# Run tests
./mvnw verify

# Build specific module
./mvnw install -pl jeap-governance-archrepo

# Build specific module without running tests
./mvnw install -pl jeap-governance-archrepo -Dmaven.test.skip=true

# Run tests for a single module
./mvnw test -pl jeap-governance-persistence

# Run single test class
./mvnw test -pl jeap-governance-domain -Dtest=StateTest

# Run single test method
./mvnw test -pl jeap-governance-domain -Dtest=StateTest#testMerge

# Skip tests
./mvnw install -Dmaven.test.skip=true
```

## Architecture

**Hexagonal Architecture (Ports & Adapters)**:

- `jeap-governance-domain/` - Core domain model (`System`, `SystemComponent`, `ComponentType`), port interfaces (
  `SystemRepository`, `SystemComponentRepository`), domain services, and **plugin interfaces** in the `domain.plugin`
  package (`DataSourceImporter`, `ComponentDeletionListener`, `Rule`). Also contains the rule evaluation engine (`rule/`
  package) and scoring model (`score/` package).
- `jeap-governance-persistence/` - JPA adapter implementing domain repository ports. Contains Flyway core schema
  migration (`V1_0_0`), Spring Data JPA repositories (`JpaSystemRepository`, `JpaComponentRepository`), and their
  adapter wrappers.
- `jeap-governance-dataimport/` - Scheduled import orchestration. `DataImportScheduler` runs on a cron schedule with
  ShedLock for distributed locking. `DataImporter` iterates all `DataSourceImporter` beans in `@Order` sequence with
  per-importer error handling and Micrometer metrics.
- `jeap-governance-rules/` - Rule evaluation and scoring infrastructure. `ScoringScheduler` runs on a cron schedule with
  ShedLock, evaluates all rules for each system's components, calculates component/system scores, and updates
  conformance rates. `RuleConfigurationProperties` defines active rules, weights, and component exemptions via YAML.
  `RuleRepositoryImpl` resolves rule activation states (ACTIVE, EXEMPTED, EXEMPTED_UNTIL).
- `jeap-governance-rules-core/` - Built-in governance rule implementations: `ComponentNamingConventionRule` (validates
  `{system-name}-{context}-{type-id}` naming) and `ComponentProducesMetricsRule` (verifies Prometheus metrics exist).
  Included transitively via `jeap-governance-rules`.
- `jeap-governance-rules-dependency/` - Built-in dependency rule: `ComponentDependenciesVersionsRule` (checks component
  dependency versions against semantic-version constraints).
- `jeap-governance-rules-messaging/` - Built-in messaging rules: `ComponentConsumesSignedMessagesRule`,
  `ComponentProducesSignedMessagesRule`, `ComponentDefinesMessagingContractsRule`.
- `jeap-governance-rules-pact/` - Built-in PACT consumer-driven-contract rules: `ConsumerContractWithinSystemRule`,
  `ConsumerContractBetweenSystemsRule`.
- `jeap-governance-archrepo/` - ArchRepo integration adapter. REST client (`ArchRepoConnector`), importers ordered by
  `ImportOrder` constants, synchronizers for each entity type, own Flyway migration (`V1000_0_0`).
- `jeap-governance-deploymentlog/` - Deployment Log integration adapter. REST client with Basic Auth, conditional on
  `jeap.governance.deploymentlog.enabled`, own Flyway migration (`V1100_0_0`).
- `jeap-governance-prometheus/` - Prometheus integration adapter. Queries AWS Amazon Managed Prometheus for standard
  jEAP metrics per service component. Entities: `PromTimeSeries`, `PromTimeSeriesSample`. Conditional on
  `jeap.governance.prometheus.enabled`, own Flyway migration (`V1200_0_0`).
- `jeap-governance-secscan/` - Security scan adapter. Discovers each component's HTTP APIs (`ApiDiscoveryClient`),
  probes endpoints for missing protection (`DefaultHttpEndpointSecurityChecker`), persists flagged endpoints. Registers
  a `ComponentDeletionListener`. Own Flyway migration (`V1300_0_0`).
- `jeap-governance-reactionobserver/` - Reaction Observer adapter. Imports each component's last observed reaction date
  via `ReactionObserverConnector` and provides `ComponentObservesReactionsRule`. Own Flyway migration (`V1400_0_0`).
- `jeap-governance-reporting/` - Generates governance rule/score reports with trend indicators to Confluence (
  `ReportingScheduler` -> `ReportingService` -> `ConfluenceAdapter`). Replaces the former `docgen` placeholder.
- `jeap-governance-web/` - Spring Boot application entry point (`GovernanceApplication`), REST controllers (
  `ManagementController` at `/api/management`), security config, OpenAPI config.
- `jeap-governance-service-instance/` - POM-only module; downstream projects inherit from this to create their own
  governance service instances.

`jeap-governance-docgen/` and `jeap-governance-pactbroker/` directories exist but are NOT registered in the root
`<modules>` and are not built.

**Core Flows**:

1. **Data Import**: Scheduled cron -> `DataImportScheduler.update()` -> `DataImporter.importData()` -> each
   `DataSourceImporter` in `@Order` sequence -> Synchronizers diff and update PostgreSQL.
2. **Scoring**: Scheduled cron -> `ScoringScheduler.updateScores()` -> for each system:
   `ScoringService.updateSystemScore()` -> `RuleEvaluationService` evaluates rules per component ->
   `ComponentScoreCalculator` and `SystemScoreCalculator` compute scores -> `RuleConformanceRateService` calculates
   per-rule conformance rates across all components.

## Key Domain Patterns

- **JPA SEQUENCE ID generation**: All entities use `@GeneratedValue(strategy=SEQUENCE)` with INCREMENT BY 50. IDs are
  assigned by the database on persist.
- **`@EntityGraph` for eager loading**: `JpaSystemRepository.findAll()` and `findByName()` use
  `@EntityGraph(attributePaths={"systemComponents"})` to avoid N+1 queries.
- **Cascade from System to SystemComponent**: `@OneToMany(cascade=ALL, orphanRemoval=true)`. Components are managed
  through the parent `System` entity.
- **Immutable collections**: `System.getSystemComponents()` returns `Collections.unmodifiableList()`. Aliases are copied
  via `copyAliases()`.

## Rule Engine

- **Rule evaluation states** (`State` enum): `OK` (compliant), `FAIL` (non-compliant), `PAUSED` (temporary exemption
  with end date), `DISABLED` (permanent exemption).
- **Rule activation states** (`RuleActivationState` enum): `ACTIVE`, `EXEMPTED` (permanent), `EXEMPTED_UNTIL` (temporary
  with expiry date).
- **Component score formula**: `score = 100 * (sum(weight)[state == OK] / sum(weight)[state != DISABLED])`. Components
  with no enabled rules score 100.
- **Conformance rate**: Per-rule percentage of OK results across all evaluated (non-DISABLED) components.
- **System score**: Average of component scores for all components in the system.
- **Exemption parameters take precedence**: `RuleParameters.of()` merges rule and exemption parameters, with exemption
  values overriding on key conflicts.

## Plugin API

Plugin interfaces live in `jeap-governance-domain/.../domain/plugin/`. Downstream projects implement these as Spring
beans:

- `DataSourceImporter` - `void importData()`. Use `@Order` to control execution sequence.
- `ComponentDeletionListener` - `void preComponentDeletion(long systemComponentId)`. Called before component removal.
  Optional `@Order`.
- `Rule` - `RuleMetadata metadata()` + `RuleResult evaluate(SystemComponent, RuleParameters)`. Automatically included in
  scheduled scoring. Return `RuleResult.ok()`, `RuleResult.failed()`, or `RuleResult.failed(comment)`.

## Auto-Configuration

Each module registers via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

- `DomainConfiguration`, `DataImportConfiguration`, `ArchRepoConfiguration`, `DeploymentLogConfiguration`,
  `PersistenceConfiguration`, `GovernanceWebConfig`, `RulesAutoConfiguration`, `CoreRulesAutoConfiguration`,
  `DependencyRulesAutoConfiguration`, `MessagingRulesAutoConfiguration`, `PactRulesAutoConfiguration`,
  `PrometheusAutoconfiguration`, `SecscanAutoconfiguration`, `ReactionObserverConfiguration`,
  `ReportingAutoConfiguration`

`DeploymentLogConfiguration` uses
`@ConditionalOnProperty(name="jeap.governance.deploymentlog.enabled", havingValue="true", matchIfMissing=true)`.

## Database

- PostgreSQL for production, PostgreSQL via TestContainers for tests
- Flyway with out-of-order migrations enabled
- **Migration version ranges**:
    - `V1_*` - Core schema (persistence module)
    - `V1000_*` - ArchRepo schema (archrepo module, prefix: `ar_`)
    - `V1100_*` - DeploymentLog schema (deploymentlog module, prefix: `dl_`)
    - `V1200_*` - Prometheus schema (prometheus module)
    - `V1300_*` - Security scan schema (secscan module)
    - `V1400_*` - Reaction Observer schema (reactionobserver module)
    - `V2000_*` and higher - Reserved for downstream service instances
- Migration naming: `V{range}_{sequence}__{description-with-hyphens}.sql`
- ShedLock table in core schema for distributed lock management

## Testing

- **Unit tests** (`*Test.java`): JUnit 5, Mockito (`@ExtendWith(MockitoExtension.class)`), AssertJ. Domain tests are
  pure logic with no Spring context.
- **JPA/Persistence tests**: `@DataJpaTest` extending `PostgresTestContainerBase` (static PostgreSQL 16-alpine container
  via TestContainers, properties injected via `@DynamicPropertySource`). Each module with persistence tests has its own
  copy of `PostgresTestContainerBase`.
- **Integration tests** (`*IT.java`): `@SpringBootTest` in `jeap-governance-web` module. Extend
  `GovernanceIntegrationTestBase` which sets up PostgreSQL TestContainer + WireMock servers for ArchRepo and
  DeploymentLog APIs. Use `@DirtiesContext(classMode=AFTER_CLASS)`.
- **HTTP mocking**: WireMock with dynamic ports. `GovernanceIntegrationTestBase` provides helper methods like
  `stubArchRepoModel()`, `stubArchRepoApiDocVersions()`, `stubDeploymentLogDeploymentLogComponentVersions()`.
- Mocks: Prefer as little Mockito usage as possible. Avoid tests that mostly only contain Mockito statements, prefer
  real domain objects instead when possible. It's ok to mock repositories.
- ALWAYS cover new code with tests. Follow existing testing patterns.

## Import Order Constants

Defined in `ImportOrder` classes within archrepo and deploymentlog modules:

- `SYSTEM_IMPORT_ORDER = HIGHEST_PRECEDENCE` (must run first)
- `REST_API_RELATION_IMPORT_ORDER = 10`
- `API_DOC_VERSION_IMPORT_ORDER = 11`
- `DATABASE_SCHEMA_VERSION_ORDER = 13`
- `REACTION_GRAPHS_LAST_MODIFIED_AT_ORDER = 14`
- `DEPLOYMENT_LOG_COMPONENT_VERSION_IMPORT_ORDER = 20`

## REST API

- `POST /api/management` - Triggers jobs (`DATA_IMPORT` or `SCORING`). Requires role `governance:admin`. Async and
  transactional.
- Swagger UI at `/swagger-ui/index.html?urls.primaryName=Governance-Service-API`
- Context path: `/jeap-governance-service`

## Versioning and Commits

- Commit Message: Use the JIRA ID from the branch name as a prefix (if available), do not use conventional commit
  messages. Example: `JIRA-1234 Implement feature X`.
- Semantic Versioning; all changes documented in [CHANGELOG.md](./CHANGELOG.md) (Keep a Changelog format).
- `setPomVersions.sh` updates the version across all module POMs.
- When working on a feature branch, increase the version to `x.y.z-SNAPSHOT` in the POMs.
- When bumping the version, also update the changelog, and update version/date in `publiccode.yml`.
- Changelog: Add a new section for the updated version, add a "### Changed" section beneath it, describe the changes on
  the feature branch, and set today's date for the new version
- When the version on a feature branch has not yet been bumped compared to master, ask the user if a major, minor or
  patch version bump should be performed, and update the version accordingly.

## Documentation

- Make sure to update README.md for changes and new features. Follow existing patterns and style.
- Update AGENTS.md if there are new patterns, architectural changes, or important information for coding agents.
- Do not reference JIRA issues in comments or documentation, as these may not be accessible to all users. Instead,
  describe the rationale and context directly in the code or documentation.
- Use Javadoc sparingly, focusing on public APIs and complex logic. Follow existing Javadoc style when adding new
  comments. Keep Javadoc comments concise and to the point.
