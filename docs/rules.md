# Rules

The governance service provides an infrastructure to evaluate governance rules and score services/systems on a
regular basis. Besides using predefined rules, you may also provide custom rules specific to your context.

**Implementation:**
1. Implement the `Rule` interface (see [Rule.java](../jeap-governance-plugin-api/src/main/java/ch/admin/jeap/governance/plugin/api/rule/Rule.java))
2. Inject any necessary repositories into your implementation to access the data you need for the rule evaluation
3. Provide the rule as a Spring bean
4. It will be automatically be evaluated during the regular rule evaluation process

## Built-in Rules

The `jeap-governance-rules-core` module ships the following built-in rules:

| Rule ID                           | Description                                                                                     |
|-----------------------------------|-------------------------------------------------------------------------------------------------|
| `component-naming-convention`     | Validates that component names follow the convention `{system-name}-{context}-{type-id}`.       |
| `component-produces-metrics`      | Checks that a component has Prometheus metrics data available.                                  |
| `component-publishes-dbschema`    | Checks that a component publishes its database schema in the architecture repository.           |
| `component-publishes-openapispec` | Checks that a component publishes its OpenAPI specification in the architecture repository.     |
| `endpoints-protected-by-jwt`      | Checks that REST endpoints are protected by a JWT bearer token based on a corresponding metric. |

**Component Naming Convention Rule** (`component-naming-convention`)

Splits the component name by `-` and validates three parts:

1. **System name** (first part) — must match `[a-z]+[a-z0-9_]*` and equal the owning system's name or one of its
   aliases (case-insensitive).
2. **Context** (middle parts joined by `-`) — must match `[a-z]+[a-z0-9-]*`.
3. **Type-id** (last part) — must be one of: `service`, `ui`, `scs`, `mobileapp`, `gateway`, `db`.

Names with fewer than 3 parts fail immediately.

**Component Produces Metrics Rule** (`component-produces-metrics`)

Verifies that at least one Prometheus time series exists for the component. This ensures that the
`jeap-spring-boot-monitoring-starter` dependency is added and the monitoring configuration is working correctly.
Requires the Prometheus module to be enabled (`jeap.governance.prometheus.enabled=true`).

**Component Publishes DB Schema Rule** (`component-publishes-dbschema`)

This rule verifies that a component publishes its database schema to the architecture repository.

Some components can be excluded from this check using the `ignored-service-names` parameter. This is useful for services that:

- Do not use a database
- Are not required to publish a database schema

Multiple service names can be specified as list elements.

Configuration Example:

```yaml
jeap:
  governance:
    rules:
      active:
        - id: component-publishes-dbschema
          weight: 3
          parameters:
            ignored-service-names: 
              - ignored-service
              - foobar-service

```

To use this rule, the following modules must be enabled:
- Prometheus module (`jeap.governance.prometheus.enabled=true`)
- DeploymentLog module (`jeap.governance.deploymentlog.enabled=true`)
- Database Schema Version in ArchRepo module (`jeap.governance.archrepo.import.databaseschemaversion.enabled=true`)

**Component Publishes OpenAPI Specification Rule** (`component-publishes-openapispec`)

This rule verifies that a component publishes its OpenAPI specification to the architecture repository.

To use this rule, the following modules must be enabled:
- DeploymentLog module (`jeap.governance.deploymentlog.enabled=true`)
- API Doc Version in ArchRepo module (`jeap.governance.archrepo.import.apidocversion.enabled=true`)

**REST Endpoint Security (Monitoring) Rule** (`endpoints-protected-by-jwt`)

This rule checks that REST endpoints of a component are protected by JWT authentication, based on Prometheus monitoring
data. Endpoints detected without JWT protection are reported as violations.

The rule applies several default exclusions, i.e. some endpoints are always ignored (unless disabled via the
`disable-default-exemptions` parameter):

- Actuator endpoints (any path containing `/actuator`)
- API docs and Swagger UI endpoints (`/api-docs`, `/swagger-ui`) on the `ref` environment only
- Wildcard frontend endpoints (`/**`, `/`)
- Configuration API calls (matching the pattern `/(ui-)?(ui)?(api)?(/)?(\w)*/((\\w)*-)?configuration(\S)*`)

The following exemption parameters can be used to exclude additional components or endpoints from being checked.
All list parameters support YAML list syntax. Wildcard matching is supported using `*` for prefix matching (e.g.,
`/api/*` matches all paths starting with `/api/`) and suffix matching (e.g., `*-service` matches all names ending
with `-service`).

| Parameter                    | Description                                                                 |
|------------------------------|-----------------------------------------------------------------------------|
| `exempt-component-names`     | List of component names to exclude entirely (wildcards supported).          |
| `exempt-methods`             | List of HTTP methods to exclude (e.g., `OPTIONS`). Case-insensitive.       |
| `exempt-paths`               | List of URL paths to exclude (wildcards supported).                         |
| `exempt-endpoints`           | List of `METHOD:PATH` pairs to exclude (e.g., `GET:/api/public/*`).        |
| `disable-default-exemptions` | Set to `true` to disable all default exemptions listed above.              |

Configuration Example:

```yaml
jeap:
  governance:
    rules:
      active:
        - id: endpoints-protected-by-jwt
          weight: 5
          parameters:
            exempt-component-names:
              - "*-testagent-service"
            exempt-methods:
              - OPTIONS
            exempt-paths:
              - /public/*
            exempt-endpoints:
              - GET:/api/public/*
              - POST:/api/webhooks/*
```

To use this rule, the following module must be enabled:
- Prometheus module (`jeap.governance.prometheus.enabled=true`)

An endpoint is reported for as long as the service exports the corresponding metric. A service that no longer exposes
an endpoint stops reporting it once it has been redeployed and the
[Prometheus query lookback](configuration.md#prometheus-query-lookback) has elapsed.

## Built-in Messaging Rules

The `jeap-governance-rules-messaging` module ships the following built-in rules:

| Rule ID                                | Description                                           |
|----------------------------------------|-------------------------------------------------------|
| `component-defines-messagingcontracts` | Validates that component defines messaging contracts. |
| `component-consumes-signedmessages`    | Validates that component consumes signed messages.    |
| `component-produces-signedmessages`    | Validates that component produces signed messages.    |
| `component-uses-latest-message-versions` | Validates that deployed contracts use the latest message versions. |

**Component Defines Messaging Contracts Rule** (`component-defines-messagingcontracts`)

This rule verifies that a component defines messaging contracts. It evaluates the `jeap_messaging_contract` metric
reported by the component and fails if any of the following contract validation switches is enabled:

| Switch                       | Failure reason                                        |
|------------------------------|-------------------------------------------------------|
| `noMasterContracts`          | Contracts are loaded from a branch other than master. |
| `consumeWithoutContract`     | Consuming messages without a contract is allowed.     |
| `publishWithoutContract`     | Publishing messages without a contract is allowed.    |
| `silentIgnoreWithoutContract`| Messages without a contract are silently ignored.     |

If the metric is not present (no messaging library detected), the rule passes.

To use this rule, the following modules must be enabled:
- Prometheus module (`jeap.governance.prometheus.enabled=true`)

**Component Uses Latest Message Versions** (`component-uses-latest-message-versions`)

This rule compares the versions in the currently deployed message contracts with the latest semantic versions in the
message type registry. Contract version data is imported from the Message Contract Service before rule evaluation.
Configure the endpoint URL as a URI template containing the `{environment}` placeholder. A violation delay can be used
to give teams a grace period before an outdated contract affects their governance score.

```yaml
jeap:
  governance:
    message-contract:
      enabled: true
      url: https://message-contract-service/api/contracts/version-status?env={environment}
      environment: PROD
      username: governance-reader
      password: ${MESSAGE_CONTRACT_PASSWORD}
      timeout: 30s
    rules:
      active:
        - id: component-uses-latest-message-versions
          weight: 5
          violation-delay: 70d
```

During `violation-delay`, a continuous violation is reported in the rule comment but remains compliant for scoring.
Once the delay expires it becomes a regular rule failure. A successful evaluation resets the delay.
Imported version status is persisted as a shared database snapshot so import and scoring can run on different service
replicas. If no version status data is available, the rule is compliant. A failed import retains the previous snapshot.
The configured user requires the `messagecontract-read` role.

**Component Consumes Signed Messages** (`component-consumes-signedmessages`)

This rule verifies that a component consumes signed messages.

To use this rule, the following modules must be enabled:
- Prometheus module (`jeap.governance.prometheus.enabled=true`)

**Component Produces Signed Messages** (`component-produces-signedmessages`)

This rule verifies that a component produces signed messages.

To use this rule, the following modules must be enabled:
- Prometheus module (`jeap.governance.prometheus.enabled=true`)

## Built-in Dependency Rules

The `jeap-governance-rules-dependency` module provides the following built-in rules:

| Rule ID                             | Description                                                                                   |
|-------------------------------------|-----------------------------------------------------------------------------------------------|
| `component-dependencies-versions`   | Ensures that a component uses defined minimum dependency versions.                            |
| `component-uses-web-config-starter` | Ensures that a self-contained system uses the `jeap-spring-boot-web-config-starter` dependency. |

**Component Dependencies Versions** (`component-dependencies-versions`)

This rule validates that a component depends on at least the configured minimum versions of specific libraries.

If a component uses a lower version than configured, the rule is violated.

Configuration:

The dependencies to validate must be provided as rule parameters using one of the following formats:
- groupId:artifactId:minimumVersion
- artifactId:minimumVersion (if groupId is not required)

Each entry defines the minimum allowed version for the dependency.

Configuration Example:

```yaml
jeap:
  governance:
    rules:
      active:
        - id: component-dependencies-versions
          weight: 3
          parameters:
            versions:
              - "ch.admin.bit.jeap:jeap-spring-boot-application-starter:18.2.0"
              - "ch.admin.bit.jeap:jeap-messaging:9.3.1"
              - "ch.admin.bit.jeap:jeap-messaging-outbox:9.3.1"
              - "ch.admin.bit.jeap:jeap-messaging-sequential-inbox:9.3.1"
              - "ch.admin.bit.jeap:jeap-crypto:4.2.0"
              - "ch.admin.bit.jeap:jeap-error-handling-service:14.0.0"
              - "ch.admin.bit.jeap:jeap-process-context-scs:13.25.0"
              - "ch.admin.bit.jeap:jeap-process-archive-service:9.5.0"
              - "spring.boot:3.5.6"
```

To use this rule, the following modules must be enabled:
- Prometheus module (`jeap.governance.prometheus.enabled=true`)

**Component Uses Web Config Starter** (`component-uses-web-config-starter`)

This rule validates that a self-contained system uses the `jeap-spring-boot-web-config-starter` dependency.

The rule only applies to components of type `SELF_CONTAINED_SYSTEM`; all other component types are considered not
applicable and pass automatically. A self-contained system that does not declare the `jeap-spring-boot-web-config-starter`
dependency violates the rule.

The rule takes no parameters.

Configuration Example:

```yaml
jeap:
  governance:
    rules:
      active:
        - id: component-uses-web-config-starter
          weight: 3
```

To use this rule, the following modules must be enabled:
- Prometheus module (`jeap.governance.prometheus.enabled=true`)

## Built-in PACT/CDC Rules

The `jeap-governance-rules-pact` module provides the following built-in rules, checking that REST API relations
discovered by the ArchRepo module are backed by a consumer-driven contract (PACT):

| Rule ID                                | Description                                                                                          |
|-----------------------------------------|-------------------------------------------------------------------------------------------------------------|
| `component-cdc-contractwithinsystem`    | Flags REST API consumer/provider relations *within the same system* that have no corresponding PACT contract. |
| `component-cdc-contractbetweensystems`  | Flags REST API consumer/provider relations *between different systems* that have no corresponding PACT contract. |

Both rules evaluate the REST relations where the checked system component is the consumer, and fail with one message
per relation missing a contract. They accept the same two optional parameters:

- `services-to-ignore`: a list of component name substrings; relations where the consumer or provider name contains
  one of these strings are ignored.
- `relations-to-ignore`: a list of `<METHOD> <path>` strings (e.g. `GET /foo/bar`) identifying specific relations to
  ignore regardless of the components involved.

Configuration Example:

```yaml
jeap:
  governance:
    rules:
      active:
        - id: component-cdc-contractwithinsystem
          weight: 2
          parameters:
            services-to-ignore:
              - legacy-component
        - id: component-cdc-contractbetweensystems
          weight: 3
          parameters:
            relations-to-ignore:
              - "GET /health"
```

To use these rules, the following modules must be enabled:
- ArchRepo module (`jeap.governance.archrepo.enabled=true`), since REST API relation data is sourced from it

## Built-in Security Scan Rules

The `jeap-governance-secscan` module provides the following built-in rules:

| Rule ID               | Description                                                                                       |
|-----------------------|-----------------------------------------------------------------------------------------------------------|
| `endpoints-protected` | Checks that REST endpoints are properly protected, based on active HTTP security scanning.        |

**REST Endpoint Security (Scanner) Rule** (`endpoints-protected`)

This rule checks that REST endpoints of a component are properly protected by actively scanning them via HTTP requests.
Unlike the monitoring-based `endpoints-protected-by-jwt` rule, this rule does not rely on Prometheus metrics but instead
performs actual HTTP requests against the discovered API endpoints.

The rule consists of two phases:

1. **Scanning (data import):** During the scheduled data import, the security scanner discovers the HTTP APIs of all
   system components and performs HTTP requests against each endpoint to check if it is properly protected. Endpoints
   that respond unexpectedly (e.g., HTTP 200 without authentication) are flagged and stored in the database. The
   scanning targets a configurable environment (default: `REF`). Exemption parameters are applied during scanning to
   skip entire APIs, components, or individual endpoints.
2. **Rule evaluation:** During the scheduled rule evaluation, the rule checks the stored scan results. If flagged
   endpoints exist that are not covered by exemptions, they are reported as violations.

The rule applies several default exclusions, i.e. some endpoints are always ignored (unless disabled via the
`disable-default-exemptions` parameter):

- Actuator endpoints (any path containing `/actuator`)
- API docs and Swagger UI endpoints (`/api-docs`, `/swagger-ui`) on the `ref` environment only
- Wildcard frontend endpoints (`/**`, `/`)
- Configuration API calls (matching the pattern `/(ui-)?(ui)?(api)?(/)?(\w)*/((\\w)*-)?configuration(\S)*`)

The following exemption parameters can be used to exclude additional components, APIs, or endpoints from being checked.
All list parameters support YAML list syntax. Wildcard matching is supported using `*` for prefix matching (e.g.,
`/api/*` matches all paths starting with `/api/`) and suffix matching (e.g., `*-service` matches all names ending
with `-service`).

| Parameter                      | Description                                                                           | Applied during          |
|---------------------------------|----------------------------------------------------------------------------------------|--------------------------|
| `exempt-component-names`       | List of component names to exclude entirely (wildcards supported).                    | Scanning & Evaluation   |
| `exempt-environments`          | List of environment names to exclude (e.g., `ABN`). Case-insensitive.                 | Scanning                |
| `exempt-api-url-not-containing`| List of strings — exempt an API if its URL does **not** contain **any** of them.      | Scanning                |
| `exempt-api-url-containing`    | List of strings — exempt an API if its URL **does** contain any of them.              | Scanning                |
| `exempt-methods`               | List of HTTP methods to exclude (e.g., `OPTIONS`). Case-insensitive.                  | Scanning & Evaluation   |
| `exempt-paths`                 | List of URL paths to exclude (wildcards supported).                                   | Scanning & Evaluation   |
| `exempt-endpoints`             | List of `METHOD:PATH` pairs to exclude (e.g., `GET:/api/public/*`).                   | Scanning & Evaluation   |
| `disable-default-exemptions`   | Set to `true` to disable all default exemptions listed above.                         | Scanning & Evaluation   |

Configuration Example:

```yaml
jeap:
  governance:
    secscan:
      enabled: true
      dataimport:
        target-environment: REF
    rules:
      active:
        - id: endpoints-protected
          weight: 5
          parameters:
            exempt-component-names:
              - "*-testagent-service"
            exempt-methods:
              - OPTIONS
            exempt-paths:
              - /public/*
            exempt-endpoints:
              - GET:/api/public/*
              - POST:/api/webhooks/*
            exempt-api-url-not-containing:
              - our-platform-name
            exempt-environments:
              - prod
              - abn
```

on a dedicated component:

```yaml
jeap:
  governance:
    secscan:
      enabled: true
      dataimport:
        target-environment: REF
    rules:
      active:
        - id: endpoints-protected
          weight: 5
      component-exemptions:
        - id: my-service-paths-exemption
          component-name: my-service
          reason: "API EOL"
          parameters:
            exempt-paths:
              - "/api/myresource"
          rule-id:
            - endpoints-protected
```

To use this rule, the following module must be enabled:
- Security Scan module (`jeap.governance.secscan.enabled=true`)

## Built-in ReactionObserver Rules

The `jeap-governance-reactionobserver` module ships the following built-in rules:

| Rule ID                         | Description                                  |
|---------------------------------|------------------------------------------------|
| `component-observes-reactions` | Validates that component observes reactions. |

**Component Observes Reactions Rule** (`component-observes-reactions`)

This rule verifies that a component has observed reactions within a defined time window.

By default, the rule checks whether reactions have been observed within the last 7 days.
The time window can be configured using the parameter `observation-max-delay-in-days`.


Configuration Example:

```yaml
jeap:
  governance:
    rules:
      active:
        - id: component-observes-reactions
          weight: 3
          parameters:
            observation-max-delay-in-days: 4
```

In this example, the rule requires that reactions are observed within the last 4 days.

Certain components can be excluded from this rule using the `ignored-service-names` parameter.
Multiple services can be specified as a list.

Configuration Example:

```yaml
jeap:
  governance:
    rules:
      active:
        - id: component-observes-reactions
          weight: 3
          parameters:
            ignored-service-names: 
              - ignored-service
              - foobar-service
```

To use this rule, the following modules must be enabled:
- ReactionObserver module (`jeap.governance.reactionobserver.enabled=true`)
- Prometheus module (`jeap.governance.prometheus.enabled=true`)

## See also

- [Configuration](configuration.md) — the `jeap.governance.rules.*` and related module properties.
- [Plugin mechanism](plugin-mechanism.md) — implementing custom data importers.
- [Architecture](architecture.md) — the rule and scoring model.
