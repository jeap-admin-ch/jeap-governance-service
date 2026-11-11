# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [10.2.0] - 2026-11-11

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 41.34.0 → 41.36.0 (minor)

## [10.1.0] - 2026-11-09

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 41.29.1 → 41.34.0 (minor)

## [10.0.0] - 2026-11-03

### Added
- A docs API for the jEAP Doc Service under `/docs-api`: `GET /docs-api/systems`, `/docs-api/systems/{system}`,
  `/docs-api/rules` and `/docs-api/rules/{ruleId}` deliver everything the governance reports show, plus every known
  rule, active or not, with its explanation, weight, nominal share, parameters and component exemptions, and the time
  of the last completed scoring run. It is always available and requires the semantic role
  `governance-documentation` / `read`; every resource carries a strong `ETag` and answers `304` to a matching
  `If-None-Match`, an unknown system or rule is answered `404` as an RFC 9457 problem document. `/api/management` is
  unchanged. See `docs/docs-api.md`.
- A webhook call to the Doc Service after every completed scoring run, switched on with
  `jeap.governance.doc-service.webhook.enabled` (default `false`). When on, `jeap.governance.doc-service.webhook.url`
  and `jeap.governance.doc-service.webhook.client-registration` are required at startup. A failed call is logged as a
  warning and never fails the scoring.
- Each of the 15 built-in rules ships its explanation as the classpath resource `jeap-governance/rules/<rule-id>.md`;
  a test in each rule module fails the build for a rule without one. Custom rules can ship theirs under the same path,
  a rule without one is logged as a warning at startup.
- `RuleResult.failed(String summary, List<String> items)` and `RuleResult.summarize(String summary, List<RuleResult>
  results)` build rule comments in the documented format: plain text, summary on the first line, one item per line.
- The module `jeap-governance-report-data` with the report data shared by the Confluence reporting and the docs API.
- The module `jeap-governance-docservice` with the webhook client of the Doc Service, included through
  `jeap-governance-web`, which serves the docs API.

### Changed
- The rules that listed several violations in one line now write one violation per line under a summary line:
  `endpoints-protected-by-jwt`, `endpoints-protected`, `component-dependencies-versions`,
  `component-cdc-contractwithinsystem`, `component-cdc-contractbetweensystems` and `component-naming-convention`.
  `component-dependencies-versions` no longer lists the up-to-date dependencies of a compliant component.
- The last status change of a rule state (reported as "non-compliant since") only moves when the state changes, no
  longer when only the comment changes. The new comment format above therefore keeps the date of components that
  were already failing.
- The comment of `component-naming-convention` lists the valid type IDs sorted, so it no longer changes between
  restarts of the service.
- The time of a recorded scheduler run is kept in whole seconds.
- `RuleRepository.getActiveRuleWeights()` skips a rule configured as active that has no rule bean, like the other
  methods of the rule repository. The scoring is unaffected, as it never evaluated such a rule.
- The trend period is configured with `jeap.governance.report-data.trend-period-days` (default 30). It now covers
  exactly that many days, up to and including the day of the reporting or scoring run - one day less than before, so
  the histories of the Confluence reports and of the docs API are one day shorter on upgrade.
- The OpenAPI group of the management API is renamed from `Governance-Service-API` to
  `Governance-Service-Management-API`; the docs API has its own group `Governance-Service-Docs-API`. Swagger UI links
  and clients fetching `/api-docs/Governance-Service-API` must be updated.
- The report data classes moved from the package `ch.admin.bit.jeap.governance.reporting.preparation` of
  `jeap-governance-reporting` to `ch.admin.bit.jeap.governance.reportdata` of `jeap-governance-report-data`
  (e.g. `ReportingSystemsPreparation`, `ReportingRulesPreparation`, `TrendIndicatorUtility`). Code using them must
  update its imports.
- `SchedulerRunTracker.recordRun()` returns the recorded time instead of `void`. This is binary-incompatible: code
  calling it must be recompiled.
- `ScoringScheduler` takes an additional `ApplicationEventPublisher` constructor argument to publish the
  `ScoringRunCompletedEvent`.
- `RuleRepository` has the new abstract method `getKnownRules()`; custom implementations must implement it.
- The repository ports gained queries scoped to a single system or rule, used by the docs API to read only the
  resource it answers with: `SystemScoreRepository` and `ComponentScoreRepository`
  `findAllBySystemNameAndDayBetweenInclusive`, `RuleStateRepository` `findAllBySystemName` and
  `findNonCompliantSinceByRuleId`, `RuleConformanceRateRepository` `findAllByRuleIdAndDayBetweenInclusive` and
  `SystemRuleConformanceRateRepository` `findLatestPerSystemIdByRuleId`. Custom implementations of these ports must
  implement them.

### Deprecated
- `jeap.governance.reporting.trendPeriodDays`: use `jeap.governance.report-data.trend-period-days`. When set, the
  deprecated property still takes precedence.
- `jeap.governance.rules.active[].documentation-link`: each rule ships its explanation. The link is still used by the
  Confluence reporting, but is no longer delivered by the docs API.
- `RuleResult.summarize(List<RuleResult>)`, which joins comments with `; `: use `RuleResult.summarize(String, List)`.

## [9.17.1] - 2026-11-01

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 41.29.0 → 41.29.1 (patch)

## [9.17.0] - 2026-10-29

### Changed
- Reaction Observer requests now use OAuth2 client credentials instead of Basic authentication. Configure the required
  Spring Security OAuth2 client registration and set `jeap.governance.reactionobserver.client-registration`.

## [9.16.0] - 2026-10-28

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 41.27.0 → 41.29.0 (minor)

## [9.15.0] - 2026-10-27

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 41.25.0 → 41.27.0 (minor)

## [9.14.0] - 2026-10-27

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 41.23.0 → 41.25.0 (minor)

## [9.13.0] - 2026-10-21

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 41.21.0 → 41.23.0 (minor)

## [9.12.1] - 2026-10-21

### Fixed
- The message contract version import no longer fails as a whole because of a single contract whose message type cannot be resolved in its registry. Such contracts are reported by the Message Contract Service with status `UNKNOWN`; they are imported with a warning and do not cause `component-uses-latest-message-versions` to fail: the rule is compliant unless other contracts of the component are outdated, and the unknown contracts are listed in the rule comment. Responses of older Message Contract Service versions without a status are still interpreted via `upToDate`.

### Changed
- An HTTP 503 from the Message Contract Service is logged as a warning instead of an error; the previous snapshot is retained and the next import retries. Importers can signal such a temporarily unavailable data source with the new `DataSourceUnavailableException`; other import failures are still logged as errors.
- The Flyway migration `V1500_1_0` replaces the `up_to_date` column of `mc_version_status` with a `state` column and makes `latest_version` nullable. The table is a snapshot that the next import rebuilds completely. Because the column is dropped and the schema is validated on startup, version 9.12.0 no longer starts against the migrated schema: a rollback requires re-adding `up_to_date` (`BOOLEAN NOT NULL DEFAULT false`) by hand.

## [9.12.0] - 2026-10-18

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 41.20.0 → 41.21.0 (minor)

## [9.11.0] - 2026-10-14

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 41.19.0 → 41.20.0 (minor)
- **org.sahli.asciidoc.confluence.publisher:asciidoc-confluence-publisher-client**: 0.35.0 → 0.36.0 (minor)

## [9.10.0] - 2026-10-12

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 41.18.1 → 41.19.0 (minor)

## [9.9.0] - 2026-10-11

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 41.17.1 → 41.18.1 (minor)

## [9.8.0] - 2026-10-07

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 41.14.0 → 41.17.1 (minor)

## [9.7.0] - 2026-10-01

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 41.13.0 → 41.14.0 (minor)

## [9.6.0] - 2026-10-01

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 41.10.0 → 41.13.0 (minor)

## [9.5.0] - 2026-09-27

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 41.9.0 → 41.10.0 (minor)

## [9.4.1] - 2026-09-24

- Update documentation

## [9.4.0] - 2026-09-23

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 41.7.0 → 41.9.0 (minor)

## [9.3.0] - 2026-09-17

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 41.5.1 → 41.7.0 (minor)

## [9.2.1] - 2026-09-17

### Removed
- Removed the `org.wiremock.integrations:wiremock-spring-boot-standalone` version management from the root pom: the same version is already managed by `jeap-internal-spring-boot-parent`.
- Removed the redundant `testcontainers.version` property: it repeated the value the parent already sets and was not referenced anywhere.
- Removed the explicit shedlock versions in `jeap-governance-reporting`: both used the parent's own `${shedlock.version}` for artifacts the parent already manages.

## [9.2.0] - 2026-09-16

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 41.3.0 → 41.5.1 (minor)

## [9.1.0] - 2026-09-15

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 41.1.0 → 41.3.0 (minor)

## [9.0.0] - 2026-09-13

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 40.11.0 → 41.1.0 (major)

## [8.9.0] - 2026-09-09

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 40.9.2 → 40.11.0 (minor)
- **org.wiremock.integrations:wiremock-spring-boot-standalone**: 4.2.2 → 4.2.3 (patch)

## [8.8.0] - 2026-09-06

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 40.8.0 → 40.9.2 (minor)

## [8.7.0] - 2026-09-03

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 40.7.0 → 40.8.0 (minor)

## [8.6.0] - 2026-09-02

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 40.5.0 → 40.7.0 (minor)

## [8.5.0] - 2026-08-30

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 40.4.0 → 40.5.0 (minor)

## [8.4.1] - 2026-08-28

### Fixed
- Exclude both AWS SDK Apache HTTP clients when using the URL connection client.

## [8.4.0] - 2026-08-28

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 40.2.0 → 40.4.0 (minor)

## [8.3.1] - 2026-08-27

### Fixed
- Exclude the AWS SDK Apache 5 HTTP client when using the URL connection client.

## [8.3.0] - 2026-08-26

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 40.1.0 → 40.2.0 (minor)

## [8.2.1] - 2026-08-26

### Changed
- Systems without components are now ignored and no more imported into the governance database.

## [8.2.0] - 2026-08-24

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 40.0.0 → 40.1.0 (minor)

## [8.1.0] - 2026-08-21

### Changed
- Rule reports show components within configured violation grace periods, including rule comments and detection and expiry timestamps.
- Violation grace-period comments use the runtime's local timezone and a human-readable timestamp format.

## [8.0.0] - 2026-08-21

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 39.5.0 → 40.0.0 (major)

## [7.3.0] - 2026-08-19

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 39.0.1 → 39.5.0 (minor)

## [7.2.1] - 2026-08-18

### Changed
- Rendered multiline rule comments as readable bullet lists in Confluence reports.

## [7.2.0] - 2026-08-18

### Changed
- The Prometheus queries now look back six hours instead of two days for the most recent sample of a service. The
  lookback window is configurable with `jeap.governance.prometheus.amp.query-lookback`.
- Note that the window is not a memory: once a service stops exporting a time series, that series drops out of the
  query results after the window has elapsed. Shortening the window therefore lets the rules based on Prometheus data
  reflect a changed service configuration within one data import instead of after two days - for example the
  `endpoints-protected-by-jwt` rule, which kept reporting an endpoint as unprotected long after the exposure had been
  removed and the service redeployed. In turn, a service that is not scraped for longer than the window now fails
  these rules. Raise the lookback for components that are not running continuously, or configure a `violation-delay`
  for the affected rules.

## [7.1.1] - 2026-08-18

### Changed
- Increased the default Message Contract Service timeout from 10 to 30 seconds.

## [7.1.0] - 2026-08-17

### Changed
- Added delayed rule violations and a messaging rule that detects deployed contracts using outdated message versions.

## [7.0.0] - 2026-08-16

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 38.6.0 → 39.0.1 (major)

## [6.3.0] - 2026-08-13

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 38.1.0 → 38.6.0 (minor)

## [6.2.0]

### Changed
- Gateway components imported from the Architecture Repository are excluded from governance rule evaluation, scoring,
  and reporting.

## [6.1.0] - 2026-08-05

### Dependencies
- Updated dependencies

## [6.0.1] - 2026-08-02

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 38.0.0 → 38.0.1 (patch)

## [6.0.0] - 2026-07-29

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 37.6.0 → 38.0.0 (major)

## [5.5.0] - 2026-07-26

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 37.4.0 → 37.6.0 (minor)

## [5.4.0] - 2026-07-23

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 37.2.0 → 37.4.0 (minor)

## [5.3.0] - 2026-07-23

### Changed
- The messaging contracts rule now fails for components that silently ignore messages without a contract
  (`jeap.messaging.contract` metric switch `silentIgnoreWithoutContract`).

## [5.2.0] - 2026-07-22

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 37.0.0 → 37.2.0 (minor)

## [5.1.0] - 2026-07-21

### Changed
- Use the standalone WireMock Spring Boot integration without exposed Jetty dependencies.

## [5.0.0] - 2026-07-21

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 36.10.0 → 37.0.0 (major)

## [4.5.0] - 2026-07-18

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 36.9.0 → 36.10.0 (minor)

## [4.4.0] - 2026-07-15

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 36.7.0 → 36.9.0 (minor)
- **org.sahli.asciidoc.confluence.publisher:asciidoc-confluence-publisher-client**: 0.34.0 → 0.35.0 (minor)

## [4.3.0] - 2026-07-13

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 36.3.1 → 36.7.0 (minor)

## [4.2.0] - 2026-07-08

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 36.2.0 → 36.3.1 (minor)

## [4.1.0] - 2026-07-06

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 36.1.0 → 36.2.0 (minor)

## [4.0.0] - 2026-06-30

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 35.11.1 → 36.1.0 (major)

## [3.5.0] - 2026-06-29

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 35.11.0 → 35.11.1 (patch)
- **org.sahli.asciidoc.confluence.publisher:asciidoc-confluence-publisher-client**: 0.32.0 → 0.34.0 (minor)

## [3.4.0] - 2026-06-23

### Changed

- Update parent from 35.6.0 to 35.11.0

## [3.3.0] - 2026-06-12

### Changed

- Update parent from 35.3.0 to 35.6.0

## [3.2.1] - 2026-06-09

### Fixed

- Fix the `component-uses-web-config-starter` rule to match the actual dependency name reported by the Prometheus
  dependency-version metric (`jeap-spring-boot-web-config-starter` instead of `jeap-web-config-starter`).

## [3.2.0] - 2026-06-09

### Added

- New built-in dependency rule `component-uses-web-config-starter`: ensures that a self-contained system uses the
  `jeap-spring-boot-web-config-starter` dependency.

## [3.1.0] - 2026-06-04

### Changed

- Update parent from 35.0.0 to 35.3.0
- update asciidoc-confluence-publisher-client from 0.30.2 to 0.32.0

## [3.0.0] - 2026-05-28

### Changed

- Official release with spring boot 4

## [1.10.0] - 2026-05-07

### Changed

- Update parent from 33.3.0 to 33.8.0
- Removed unused jeap-governance-pactbroker

## [1.9.0] - 2026-04-16

### Changed

- Update parent from 33.2.0 to 33.3.0

## [1.8.0] - 2026-04-13

### Changed

- Update parent from 33.1.1 to 33.2.0

## [1.7.0] - 2026-04-09

### Changed

- Update parent from 33.1.0 to 33.1.1

## [1.6.0] - 2026-04-09

### Changed

- Update parent from 33.0.0 to 33.1.0

## [1.5.0] - 2026-04-02

### Changed

- Update parent from 32.0.0 to 33.0.0

## [1.4.1] - 2026-04-02

### Changed

- Update postgres images to version 17. 

## [1.4.0] - 2026-03-31

### Changed

- Update parent from 31.5.0 to 32.0.0

## [1.3.2] - 2026-03-30

### Fixed

- Skip REST APIs without a server URL during API discovery to prevent exceptions during security scanning

## [1.3.1] - 2026-03-27

### Changed

- Fix data import of rest apis without pact and delete all data before importing the new data

## [1.3.0] - 2026-03-26

### Changed

- Update parent from 31.4.0 to 31.5.0

## [1.2.0] - 2026-03-25

### Added

- Persist last scheduler run date/time in a new `scheduler_run` database table for all schedulers (data-import,
  scoring, reporting) so that the `jeap_governance_service_*_last_run_from` metrics report correctly even after
  restarts/redeployments

## [1.1.0] - 2026-03-23

### Changed

- Update parent from 31.0.0 to 31.4.0

## [1.0.1] - 2026-03-12

### Changed

- small bugfix in presentation (Confluence report)

## [1.0.0] - 2026-03-11

### Changed

- Initial release
- some bugfixes
- update parent to 31.0.0

## [0.0.19] - 2026-03-11

### Added

- update the rule `component-observes-reactions` with observation-max-delay-in-days parameter

## [0.0.18] - 2026-03-10

### Added

- added jeap-governance-reactionobserver module including dataimport and `component-observes-reactions` rule that validates if components observe reactions
- update parent to 30.20.0

### Removed

- removed the dataimport from ArchRepo for reaction graphs

## [0.0.17] - 2026-03-05

### Added

- added the `component-cdc-contractbetweensystems` and  `component-cdc-contractwithinsystem` rules that validate if components define consumer contracts

## [0.0.16] - 2026-03-04

### Changed

- Improved templates for better Dark Mode appearance
- Code refactoring and cleanup

## [0.0.15] - 2026-03-04

### Added

- added the `endpoints-protected` and  `endpoints-protected-by-jwt` rules that validate if endpoints are properly protected.

## [0.0.14] - 2026-03-03

### Added

- added the `component-dependencies-versions` rule that validates the dependencies versions of components

## [0.0.13] - 2026-03-02

### Added

- added Confluence reporting module: periodically generates pages for system scores, component scores, and rule conformance rates
- update parent to 30.18.0

## [0.0.12] - 2026-02-27

### Added

- added the `component-consumes-signedmessages` and `component-produces-signedmessages` rules that validates if the component consumes and produces signed messages

## [0.0.11] - 2026-02-27

### Added

- added the `component-defines-messagingcontracts` rule that validates if the component defines messaging contracts

## [0.0.10] - 2026-02-26

### Added

- added the `component-publishes-openapispec` rule that validates if the component publishes its OpenAPI specification

## [0.0.9] - 2026-02-26

### Added

- added the `component-publishes-dbschema` rule that validates if the component publishes its database schema

## [0.0.8] - 2026-02-24

### Added

- added security scan data import

## [0.0.7] - 2026-02-19

### Added

- calculate and persist rule conformance rate per system after scoring

## [0.0.6] - 2026-02-19

### Added

- added the `component-naming-convention` rule that validates component names follow the
  `{system-name}-{context}-{type-id}` convention
- added the `component-produces-metrics` rule that validates that components are imported from metrics
- minor refactoring and cleanup

## [0.0.5] - 2026-02-18

### Added

- added rule evaluation and scoring

## [0.0.4] - 2026-02-12

### Changed

- added Prometheus data import
- fixed a bug in the core model when deleting a system component

## [0.0.3] - 2026-02-10

### Changed

- add DeploymentLog import
- Some refactorings

## [0.0.2] - 2026-02-09

### Changed

- add ArchRepo import with core database model

## [0.0.1] - 2026-01-28

### Changed

- First pre version, skeleton created.
