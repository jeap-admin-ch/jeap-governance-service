# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
