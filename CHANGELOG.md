# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

> - Spring Boot 3 maintenance (bug fixes, patches, and regular updates) continues on branch `release/springboot3`.

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
