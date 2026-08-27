# Architecture

`jeap-governance-service` is a **service template**: a set of Maven modules that a concrete governance
service instance depends on, configures, and extends with plugin implementations. This page describes the
module layout, the domain model, and the database schema.

## Overview

A governance service instance is built from the `jeap-governance-service` starter, configured with governance
config and rules (built-in and/or project-specific), imports data from various sources (Architecture Repository,
Deployment Log, Reaction Observer, Prometheus, PACT Broker, and optionally a project-specific source), and
publishes its evaluation results to Confluence.

```mermaid
flowchart LR

%% ---------- Data sources ----------
    ARCH["jEAP Arch Repo Service"]
    DEPLOY["jEAP Deployment Log Service"]
    REACT["jEAP Reaction Observer Service"]
    PROM["Prometheus"]
    PSPEC["Project Specific Resources"]

%% ---------- Configuration & rules ----------
    CONFIG["Governance Config"]
    RULES["jEAP Governance Rules"]

%% ---------- Core ----------
    STARTER["jEAP Governance Service Starter"]
    GOV["jEAP Governance Service"]

%% ---------- Output ----------
    CONF["Confluence"]
    PAGE["Confluence Page"]

%% ---------- Relations ----------
    GOV -. "instance of" .-> STARTER

    GOV -- "uses" --> CONFIG
    GOV -- "uses" --> RULES
    GOV -- "uses" --> PSPEC

    ARCH -- "data" --> GOV
    DEPLOY -- "data" --> GOV
    REACT -- "data" --> GOV
    PROM -- "data" --> GOV
    PSPEC -. "data" .-> GOV

    GOV -- "writes" --> CONF
    CONF --- PAGE

%% ---------- Styling ----------
    classDef jeap fill:#b4d3ef,stroke:#4a7ebb,stroke-width:1px,color:#1f3b57
    classDef service fill:#f6ccaa,stroke:#c8792e,stroke-width:1px,color:#4a2c10
    classDef external fill:#e2e2e2,stroke:#5a5a5a,stroke-width:1px,color:#2b2b2b
    classDef optional fill:#f7f7f7,stroke:#9a9a9a,stroke-width:1px,stroke-dasharray:5 4,color:#5a5a5a

    class CONFIG,RULES,STARTER,GOV,PAGE jeap
    class ARCH,DEPLOY,REACT service
    class CONF,PROM external
    class PSPEC optional

    linkStyle 0,1,2,3 stroke:#5a5a5a,stroke-width:1px
    linkStyle 4,5,6,7,8,9,10 stroke:#4a7ebb,stroke-width:2px
```

## Module Overview

| Module                             | Description                                                                                                                                                                                                   | Notes                                                                                     |
|-------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------|
| `jeap-governance-archrepo`         | Handles integration with the Architecture Repository. Loads the core model and persists it to the database. Can optionally load and persist: ApiDocVersion, DatabaseSchemaVersion, RestApiRelationWithoutPact | See [Configuration](configuration.md)                                                      |
| `jeap-governance-dataimport`       | Schedules the data import process                                                                                                                                                                             | See [Plugin mechanism](plugin-mechanism.md) and [Configuration](configuration.md)          |
| `jeap-governance-deploymentlog`    | Handles integration with the Deployment Log. Loads the deployment log component versions and persists it to the database.                                                                                     | See [Configuration](configuration.md)                                                      |
| `jeap-governance-reporting`        | Generates governance rule evaluation outcomes to Confluence                                                                                                                                                   | See [Reporting](reporting.md)                                                              |
| `jeap-governance-domain`           | Contains the core model, specifically System and SystemComponent, including data access interfaces                                                                                                            | -                                                                                          |
| `jeap-governance-persistence`      | Provides flyway migration scripts and JPA repositories                                                                                                                                                        | -                                                                                          |
| `jeap-governance-prometheus`       | Handles integration with a Prometheus server. Queries the latest samples of selected standard jEAP metrics for the system's service components and persists them to the database.                             | See [Configuration](configuration.md)                                                      |
| `jeap-governance-reactionobserver` | Retrieves the last observed reaction date of the system components.                                                                                                                                           | See [Configuration](configuration.md)                                                      |
| `jeap-governance-secscan`          | Scans the known HTTP APIs of the system components for unprotected endpoints and persists the flagged endpoints in the database.                                                                              | See [Configuration](configuration.md)                                                      |
| `jeap-governance-rules`            | Provides the infrastructure to evaluate governance rules and score services/systems on a regular basis                                                                                                        | Instances can define this as their parent                                                  |
| `jeap-governance-rules-core`       | Built-in governance rules shipped with the service                                                                                                                                                            | Included transitively via `jeap-governance-rules`; see [Rules](rules.md)                  |
| `jeap-governance-rules-dependency` | Built-in governance dependencies rules shipped with the service                                                                                                                                               | Included transitively via `jeap-governance-rules`; see [Rules](rules.md)                  |
| `jeap-governance-rules-messaging`  | Built-in governance messaging rules shipped with the service                                                                                                                                                  | Included transitively via `jeap-governance-rules`; see [Rules](rules.md)                  |
| `jeap-governance-rules-pact`       | Built-in governance consumer-driven-contract (CDC/PACT) rules shipped with the service                                                                                                                        | Included transitively via `jeap-governance-rules`; see [Rules](rules.md)                  |
| `jeap-governance-service-instance` | Module for easily creating an instance of the governance service                                                                                                                                              | Instances can define this as their parent                                                  |
| `jeap-governance-web`              | Contains REST interfaces and the application itself                                                                                                                                                           | -                                                                                          |

## Domain Model

### Core Model

The core model consists of Systems and their associated System Components.

```mermaid
classDiagram
    class System {
        <<Entity>>
        -Long id
        -String name
        -Set~String~ aliases
        -List~SystemComponent~ systemComponents
        -State state
        -ZonedDateTime createdAt
        +getSystemComponents() List
        +setAliases(Set~String~) void
        +addSystemComponent(SystemComponent) void
        +deleteSystemComponent(SystemComponent) void
        +getSystemComponentByName(String) Optional
    }
    class SystemComponent {
        <<Entity>>
        -Long id
        -String name
        -State state
        -System system
        -ComponentType type
        -ZonedDateTime createdAt
        +update(ComponentType) void
        +setState(State) void
        +setSystem(System) void
    }
    class State {
        <<Enumeration>>
        OK
        FAIL
        IGNORE
        UNKNOWN
        +merge(State, State) State
    }
    class ComponentType {
        <<Enumeration>>
        BACKEND_SERVICE
        FRONTEND
        MOBILE_APP
        SELF_CONTAINED_SYSTEM
        UNKNOWN
    }
    System "1" --> "*" SystemComponent : systemComponents
    SystemComponent "*" --> "1" System : system
    System ..> State : uses
    SystemComponent ..> State : uses
    SystemComponent ..> ComponentType : uses

    %% Package: ch.admin.bit.jeap.governance.domain
```

### Scoring and Rule Models

The scoring model consists of ComponentScore and SystemScore, which are used to capture the results of governance rule
evaluations. The rule model consists of the RuleState of a single system component, and the RuleConformanceRate, which
captures the overall conformance of a rule across all evaluated components.

The ComponentScoreCalculator and SystemScoreCalculator are responsible for calculating the scores based on the
active governance rules and their evaluation results.

A rule for a component can be in any of the following four rule states after evaluation:

- OK: The rule is active and the component complies with the rule.
- FAIL: The rule is active but the component does not comply with the rule.
- PAUSED: The rule is temporarily paused due to a temporary exemption.
- DISABLED: The rule is disabled due to an indefinite exemption.

A component's score is defined as the percentage of active rules that are in the OK state, weighted by the importance
of each rule. A rule is disabled if it has an indefinite exemption. The formula is as follows:

```
score = 100 * (sum(ruleWeight)[ruleState == OK] / sum(ruleWeight)[ruleState != DISABLED])
```

The system score is calculated as the average of the component scores of all components belonging to the system.

```mermaid
classDiagram
    class System {
        <<Entity>>
        %% From domain package (simplified view)
    }
    class SystemComponent {
        <<Entity>>
        %% From domain package (simplified view)
    }
    class SystemScore {
        -Long id
        -System system
        -int score "percent"
        -LocalDate day
        -ZonedDateTime createdAt
    }
    class ComponentScore {
        -Long id
        -SystemComponent systemComponent
        -int score "percent"
        -LocalDate day
        -ZonedDateTime createdAt
    }
    class RuleState {
        -Long id
        -SystemComponent systemComponent
        -String ruleId
        -State state "OK, FAIL, PAUSED, DISABLED"
        -LocalDate day
        -ZonedDateTime createdAt
        -ZonedDateTime modifiedAt "last state change"
    }
    class RuleConformanceRate {
        -Long id
        -String ruleId
        -int conformanceRate "percent"
        -LocalDate day
        -ZonedDateTime createdAt
    }
    class SystemRuleConformanceRate {
        -Long id
        -String ruleId
        -Long systemId
        -int conformanceRate "percent"
        -LocalDate day
        -ZonedDateTime createdAt
    }
    System "1" o-- "*" SystemScore
    SystemComponent "1" o-- "*" ComponentScore
    SystemComponent "1" o-- "*" RuleState

    %% Packages: ch.admin.bit.jeap.governance.archrepo.domain.score, ch.admin.bit.jeap.governance.archrepo.domain.rule
```

### ArchRepo Model

The ArchRepo model consists of ApiDocVersion, DatabaseSchemaVersion, and RestApiRelationWithoutPact.

```mermaid
classDiagram
    class SystemComponent {
        <<Entity>>
        %% From domain package (simplified view)
    }
    class ApiDocVersion {
        -Long id
        -String version
        -SystemComponent systemComponent
        -ZonedDateTime createdAt
    }
    class DatabaseSchemaVersion {
        -Long id
        -String version
        -SystemComponent systemComponent
        -ZonedDateTime createdAt
    }
    class RestApiRelationWithoutPact {
        -Long id
        -String method
        -String path
        -SystemComponent providerSystemComponent
        -SystemComponent consumerSystemComponent
        -ZonedDateTime createdAt
    }
    ApiDocVersion "*" --> "1" SystemComponent
    DatabaseSchemaVersion "*" --> "1" SystemComponent
    RestApiRelationWithoutPact "*" --> "1" SystemComponent : provider
    RestApiRelationWithoutPact "*" --> "1" SystemComponent : consumer

    %% Package: ch.admin.bit.jeap.governance.archrepo.domain
```

### DeploymentLog Model

The DeploymentLog model consists of DeploymentLogComponentVersion.

```mermaid
classDiagram
    class SystemComponent {
        <<Entity>>
        %% From domain package (simplified view)
    }
    class DeploymentLogComponentVersion {
        -Long id
        -String version
        -SystemComponent systemComponent
        -ZonedDateTime createdAt
    }
    DeploymentLogComponentVersion "*" --> "1" SystemComponent

    %% Package: ch.admin.bit.jeap.governance.deploymentlog.domain
```

### ReactionObserver Model

The ReactionObserver model consists of ReactionObserverComponentLastObservationDate.

```mermaid
classDiagram
    class SystemComponent {
        <<Entity>>
        %% From domain package (simplified view)
    }
    class ReactionObserverComponentLastObservationDate {
        -Long id
        -LocalDate lastObservationDate
        -SystemComponent systemComponent
        -ZonedDateTime createdAt
    }
    ReactionObserverComponentLastObservationDate "*" --> "1" SystemComponent

    %% Package: ch.admin.bit.jeap.governance.reactionobserver.domain
```

### Prometheus Time Series Model

The Prometheus domain model records time series samples per service component and query type. Since import queries are
designed to capture the current state of a component aspect (not to retrieve extended time series) only a limited number
of time series samples is expected. Consequently, the model intentionally remains denormalized to keep the domain and
implementation straightforward.

```mermaid
classDiagram
    class PromQueryType {
        <<Enum>>
    }
    class PromTimeSeries {
        <<Entity>>
        -Long id
        -String systemComponentName
        -PromQueryType prometheusQueryType
        -ZonedDateTime queryTimestamp
        -PromTimeSeriesSample sample
    }
    class PromTimeSeriesSample {
        <<JSON>>
        -Map~String,String~ metric
        -List~String~ value
    }
    PromTimeSeries "*" --> "1" PromQueryType
    PromTimeSeries "1" *-- "1" PromTimeSeriesSample : sample

    %% Package: ch.admin.bit.jeap.governance.prometheus.domain
```

### Security Scan Model

The secscan module implements a security scan of known system component APIs to identify unprotected endpoints. Known
APIs of a system component are identified via the API discovery service. This service queries a jEAP architecture
repository instance for a system component's REST endpoints. Those endpoints are then checked for security by executing
HTTP requests on them. Endpoints accepting and not rejecting the requests are flagged as unprotected. The scan
result details for flagged endpoints are persisted to SecscanFlaggedEndpoint entities. In addition, a summarizing scan
message is persisted per system component in a SecscanState entity. A configurable endpoint filter allows to filter out
certain APIs and endpoints from the scan, e.g. to exclude APIs or endpoints that are expected to be unprotected.

```mermaid
classDiagram
    class SecscanState {
        <<Entity>>
        -Long id
        -long systemComponentId
        -String scanMessage
        -ZonedDateTime scanTimestamp
    }
    class SecscanFlaggedEndpoint {
        <<Entity>>
        -Long id
        -long systemComponentId
        -String path
        -String method
        -String scanMessage
        -ZonedDateTime scanTimestamp
    }

    %% Package: ch.admin.bit.jeap.governance.secscan.domain
```

## Database

### Flyway Migration Strategy

We use major version namespaces to clearly separate database migrations for the core and for each plugin/service instance.

#### Version Ranges

| Range                   | Purpose                                                            |
|-------------------------|--------------------------------------------------------------------|
| `V1_*`                  | Core schema - Shared database structure used by all services       |
| `V1000_*` to `V1999_*'  | Reserved for modules within the jeap-governance-service reposiotry |
| `V2000_*` and higher    | Service/plugin-specific migrations                                 |

#### Flyway Configuration

We enable out-of-order migrations to allow independent evolution of plugins and services:

```yaml
spring:
  flyway:
    out-of-order: true
```

### Core Schema

```mermaid
erDiagram
    system {
        BIGINT id PK
        VARCHAR name UK
        VARCHAR state
        TIMESTAMPTZ created_at
    }
    system_aliases {
        BIGINT system_id PK, FK
        VARCHAR aliases PK
    }
    system_component {
        BIGINT id PK
        VARCHAR name UK
        BIGINT system_id FK
        VARCHAR state
        VARCHAR type
        TIMESTAMPTZ created_at
    }
    shedlock {
        VARCHAR name PK "64"
        TIMESTAMP lock_until "3"
        TIMESTAMP locked_at "3"
        VARCHAR locked_by "255"
    }
    system ||--o{ system_aliases : "system_id"
    system ||--o{ system_component : "system_id"
```

Notes:
- `system.name` and `system_component.name` have `UNIQUE` constraints.
- `system_aliases` has a composite primary key `(system_id, aliases)`.
- `shedlock` is used for distributed locking (ShedLock library) and is unrelated to the domain tables above.
- Indices: `system_name ON (name)`, `system_aliases_system_id ON (system_id)`, `system_component_name ON (name)`.

### Scoring Schema

```mermaid
erDiagram
    system {
        BIGINT id PK
    }
    system_component {
        BIGINT id PK
    }
    system_score {
        BIGINT id PK
        INTEGER score
        BIGINT system_id FK_UQ
        DATE day UQ
        TIMESTAMPTZ created_at
    }
    system_component_score {
        BIGINT id PK
        INTEGER score
        BIGINT system_component_id FK_UQ
        DATE day UQ
        TIMESTAMPTZ created_at
    }
    rule_state {
        BIGINT id PK
        VARCHAR rule_id
        BIGINT system_component_id FK_UQ
        VARCHAR state UQ
        VARCHAR state_comment
        TIMESTAMPTZ created_at
        TIMESTAMPTZ modified_at
    }
    rule_conformance_rate {
        BIGINT id PK
        VARCHAR rule_id UQ
        INTEGER conformance_rate
        DATE day UQ
        TIMESTAMPTZ created_at
    }
    system_rule_conformance_rate {
        BIGINT id PK
        VARCHAR rule_id UQ
        BIGINT system_id
        INTEGER conformance_rate
        DATE day UQ
        TIMESTAMPTZ created_at
    }
    system ||--o{ system_score : "system_id"
    system_component ||--o{ system_component_score : "system_component_id"
    system_component ||--o{ rule_state : "system_component_id"
```

Notes:
- `(system_component_id, day)` on `system_component_score`, `(system_id, day)` on `system_score`, `(system_component_id,
  rule_id, day)` on `rule_state`, `(rule_id, day)` on `rule_conformance_rate`, and `(rule_id, system_id, day)` on
  `system_rule_conformance_rate` are unique — one row per entity/rule per day.
- `rule_state.modified_at` tracks the last state change, distinct from `created_at`.

### Scheduler Run Table

The `scheduler_run` table persists the last successful run timestamp per scheduler job (data-import, scoring,
reporting).
This ensures that the `jeap_governance_service_*_last_run_from` metrics report correctly even after application restarts
or redeployments.

| Column        | Type        | Description                                       |
|---------------|-------------|---------------------------------------------------|
| `job_name`    | `VARCHAR`   | Primary key, identifies the scheduler job         |
| `last_run_at` | `TIMESTAMP` | Timestamp of the last successful run for this job |

### ArchRepo Schema

```mermaid
erDiagram
    system_component {
        BIGINT id PK
    }
    ar_api_doc_version {
        BIGINT id PK
        VARCHAR version
        BIGINT system_component_id FK
        TIMESTAMPTZ created_at
    }
    ar_database_schema_version {
        BIGINT id PK
        VARCHAR version
        BIGINT system_component_id FK
        TIMESTAMPTZ created_at
    }
    ar_reaction_graph {
        BIGINT id PK
        TIMESTAMPTZ last_modified_at
        BIGINT system_component_id FK
        TIMESTAMPTZ created_at
    }
    ar_rest_api_relation_without_pact {
        BIGINT id PK
        VARCHAR method
        VARCHAR path
        BIGINT provider_system_component_id FK
        BIGINT consumer_system_component_id FK
        TIMESTAMPTZ created_at
    }
    system_component ||--o{ ar_api_doc_version : "system_component_id"
    system_component ||--o{ ar_database_schema_version : "system_component_id"
    system_component ||--o{ ar_reaction_graph : "system_component_id"
    system_component ||--o{ ar_rest_api_relation_without_pact : "provider_system_component_id"
    system_component ||--o{ ar_rest_api_relation_without_pact : "consumer_system_component_id"
```

Notes:
- All FK relationships reference `system_component.id` from the domain package.
- All archrepo tables use `TIMESTAMP WITH TIME ZONE` for temporal columns.
- Indices are created on all foreign key columns for optimal query performance, e.g.
  `ar_rest_api_relation_without_pact_provider_system_component_id` and
  `ar_rest_api_relation_without_pact_consumer_system_component_id`.

### DeploymentLog Schema

```mermaid
erDiagram
    system_component {
        BIGINT id PK
    }
    dl_component_version {
        BIGINT id PK
        VARCHAR version
        BIGINT system_component_id FK
        TIMESTAMPTZ created_at
    }
    system_component ||--o{ dl_component_version : "system_component_id"
```

Notes:
- All FK relationships reference `system_component.id` from the domain package.
- All deploymentlog tables use `TIMESTAMP WITH TIME ZONE` for temporal columns.
- Indices are created on all foreign key columns for optimal query performance (e.g.
  `dl_component_version_system_component_id`).

### ReactionObserver Schema

```mermaid
erDiagram
    system_component {
        BIGINT id PK
    }
    ro_component_last_observation_date {
        BIGINT id PK
        DATE last_observation_date
        BIGINT system_component_id FK
        TIMESTAMPTZ created_at
    }
    system_component ||--o{ ro_component_last_observation_date : "system_component_id"
```

Notes:
- All FK relationships reference `system_component.id` from the domain package.
- Index: `ro_component_last_observation_date_system_component_id`.

### Prometheus Time Series Schema

The Prometheus database schema is designed around the assumption that import queries capture the current state of a
component aspect rather than extended time series. As a result, only a limited number of samples is expected to be
stored. Consequently, the schema intentionally remains denormalized (avoiding additional normalization of sample
storage) to keep both the data model and the implementation straightforward.

```mermaid
erDiagram
    system_component {
        BIGINT id PK
        VARCHAR name UK
    }
    prom_time_series {
        BIGINT id PK
        VARCHAR prometheus_query_type
        TIMESTAMPTZ query_timestamp
        VARCHAR system_component_name FK
        JSONB sample
    }
    system_component ||--o{ prom_time_series : "system_component_name"
```

%% Database Schema: ch.admin.bit.jeap.governance.prometheus.domain

### Security Scan Schema

```mermaid
erDiagram
    secscan_state {
        BIGINT id PK
        BIGINT systemComponentId FK
        VARCHAR scanMessage
        TIMESTAMPTZ scanTimestamp
    }
    secscan_flagged_endpoint {
        BIGINT id PK
        BIGINT systemComponentId FK
        VARCHAR path
        VARCHAR method
        VARCHAR scanMessage
        TIMESTAMPTZ scanTimestamp
    }
```

%% Database Schema: ch.admin.bit.jeap.governance.secscan.domain — `systemComponentId` in both tables
%% references `system_component.id` from the domain package.

## See also

- [Configuration](configuration.md) — configuration properties for the modules described above.
- [Rules](rules.md) — the built-in governance rules and how to write custom ones.
- [Plugin mechanism](plugin-mechanism.md) — data import and data deletion extension points.
- [Reporting](reporting.md) — Confluence report generation.
- [Metrics](metrics.md) — Prometheus metrics and recommended alerts.
