# jeap-governance-service

Service which provides a quick overview of system and service compliance with defined policies.

## Installing / Getting started

Normally you will not use this project directly, but instead set up your own governance service depending on this common library. Check the documentation in confluence for details.

## Changes
This library needs to be versioned using [Semantic Versioning](http://semver.org/) and all changes need to be documented at [CHANGELOG.md](./CHANGELOG.md) following the format defined in [Keep a Changelog](http://keepachangelog.com/)

## Note

This repository is part of the open source distribution of jEAP. See [github.com/jeap-admin-ch/jeap](https://github.com/jeap-admin-ch/jeap)
for more information.

## License

This repository is Open Source Software licensed under the [Apache License 2.0](./LICENSE).

## Architecture & Implementation Guide

### Table of Contents

- [Module Overview](#module-overview)
- [Domain Model](#domain-model)
  - [Core Model](#core-model)
  - [ArchRepo Model](#archrepo-model)
  - [DeploymentLog Model](#deploymentlog-model)
  - [Prometheus Model](#prometheus-time-series-model)
- [Database Schema](#database-schema)
  - [Flyway Migration Strategy](#flyway-migration-strategy)
  - [Core Schema](#core-schema)
  - [ArchRepo Schema](#archrepo-schema)
  - [DeploymentLog Schema](#deploymentlog-schema)
  - [Prometheus Schema](#prometheus-time-series-schema)
- [Configuration](#configuration)
- [Plugin Mechanism](#plugin-mechanism)
  - [Data Import](#data-import)
  - [Data Deletion](#data-deletion)
- [Metrics](#metrics)

### Module Overview

| Module                             | Description                                                                                                                                                                                                                  | Notes                                                            |
|------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------|
| `jeap-governance-archrepo`         | Handles integration with the Architecture Repository. Loads the core model and persists it to the database. Can optionally load and persist: ApiDocVersion, DatabaseSchemaVersion, ReactionGraph, RestApiRelationWithoutPact | See [Configuration](#configuration)                              |
| `jeap-governance-dataimport`       | Schedules the data import process                                                                                                                                                                                            | See [Data Import plugin mechanism](#data-import) and [Configuration](#configuration) |
| `jeap-governance-deploymentlog`    | Handles integration with the Deployment Log. Loads the deployment log component versions and persists it to the database.                                                                                                    | See [Configuration](#configuration)                              |
| `jeap-governance-docgen`           | Generates governancen rule evaluation outcomes to Confluence                                                                                                                                                                 | See [Data Import plugin mechanism](#data-import) and [Configuration](#configuration) |
| `jeap-governance-domain`           | Contains the core model, specifically System and SystemComponent, including data access interfaces                                                                                                                           | -                                                                |
| `jeap-governance-pactbroker`       | Imports data from a PACT broker instance                                                                                                                                                                                     | -                                                                |
| `jeap-governance-persistence`      | Provides flyway migration scripts and JPA repositories                                                                                                                                                                       | -                                                                |
| `jeap-governance-plugin-api`       | Contains all interfaces for implementing custom logic in concrete instances of the governance service                                                                                                                        | -                                                                |
| `jeap-governance-prometheus`       | Handles integration with a Prometheus server. Queries the latest samples of selected standard jEAP metrics for the system's service components and persists them to the database.                                            | See [Configuration](#configuration)                              |
| `jeap-governance-rules`            | Provides the infrastructure to evaluate governance rules and score services/systems on a regular basis                                                                                                                       | Instances can define this as their parent                        |
| `jeap-governance-service-instance` | Module for easily creating an instance of the governance service                                                                                                                                                             | Instances can define this as their parent                        |
| `jeap-governance-web`              | Contains REST interfaces and the application itself                                                                                                                                                                          | -                                                                |

### Domain Model

#### Core Model

The core model consists of Systems and their associated System Components.

![Core Model Diagram](docs/images/governance-domain-model.png)

#### ArchRepo Model

The ArchRepo model consists of ApiDocVersion, DatabaseSchemaVersion, ReactionGraph, and RestApiRelationWithoutPact.

![ArchRepo Model Diagram](docs/images/archrepo-domain-model.png)

#### DeploymentLog Model

The DeploymentLog model consists of DeploymentLogComponentVersion.

![DeploymentLog Model Diagram](docs/images/deploymentlog-domain-model.png)

#### Prometheus Time Series Model

The Prometheus domain model records time series samples per service component and query type. Since import queries are
designed to capture the current state of a component aspect (not to retrieve extended time series) only a limited number
of time series samples is expected. Consequently, the model intentionally remains denormalized to keep the domain and
implementation straightforward.

![Prometheus Model Diagram](docs/images/prometheus-domain-model.png)

### Database

#### Flyway Migration Strategy

We use major version namespaces to clearly separate database migrations for the core and for each plugin/service instance.

##### Version Ranges

| Range                   | Purpose                                                            |
|-------------------------|--------------------------------------------------------------------|
| `V1_*`                  | Core schema - Shared database structure used by all services       |
| `V1000_*` to `V1999_*'  | Reserved for modules within the jeap-governance-service reposiotry |
| `V2000_*` and higher    | Service/plugin-specific migrations                                 |

##### Flyway Configuration

We enable out-of-order migrations to allow independent evolution of plugins and services:

```yaml
spring:
  flyway:
    out-of-order: true
```

#### Core Schema

![Core Schema Diagram](docs/images/governance-db-schema.png)

#### ArchRepo Schema

![ArchRepo Schema Diagram](docs/images/archrepo-db-schema.png)

#### DeploymentLog Schema

![DeploymentLog Schema Diagram](docs/images/deploymentlog-db-schema.png)

#### Prometheus Time Series Schema
The Prometheus database schema is designed around the assumption that import queries capture the current state of a
component aspect rather than extended time series. As a result, only a limited number of samples is expected to be
stored. Consequently, the schema intentionally remains denormalized (avoiding additional normalization of sample
storage) to keep both the data model and the implementation straightforward.

![Prometheus Time Series Schema Diagram](docs/images/prometheus-db-schema.png)

### Configuration

All configuration properties support Spring Boot's standard configuration mechanisms (application.yml, environment variables, etc.).

| Property                                                             | Description                                                                       | Default                       | Required               |
|----------------------------------------------------------------------|-----------------------------------------------------------------------------------|-------------------------------|------------------------|
| `jeap.governance.environment`                                        | Environment of the service(DEV, REF, ABN, PROD).                                  | -                             | Yes                    |
| `jeap.governance.archrepo.url`                                       | URL of the Architecture Repository                                                | -                             | Yes                    |
| `jeap.governance.dataimport.cron-expression`                         | Cron expression to schedule the data import job                                   | `0 15 6,10,14,18 * * MON-FRI` | No                     |
| `jeap.governance.dataimport.lock-at-least`                           | Minimum duration for which the lock should be held during the data import job     | `PT30M`                       | No                     |
| `jeap.governance.dataimport.lock-at-most`                            | Maximum lock duration for the data import job                                     | `PT2H`                        | No                     |
| `jeap.governance.archrepo.timeout`                                   | Connection timeout for Architecture Repository integration                        | `PT5M`                        | No                     |
| `jeap.governance.archrepo.import.apidocversion.enabled`              | Enable/disable import of API documentation versions from ArchRepo                 | `true`                        | No                     |
| `jeap.governance.archrepo.import.databaseschemaversion.enabled`      | Enable/disable import of database schema versions from ArchRepo                   | `true`                        | No                     |
| `jeap.governance.archrepo.import.reactiongraph.enabled`              | Enable/disable import of reaction graphs from ArchRepo                            | `true`                        | No                     |
| `jeap.governance.archrepo.import.restapirelationwithoutpact.enabled` | Enable/disable import of REST API relations without Pact from ArchRepo            | `true`                        | No                     |
| `jeap.governance.deploymentlog.enabled`                              | Enable/disable import of data from the of DeploymentLog                           | `true`                        | No                     |
| `jeap.governance.deploymentlog.url`                                  | URL of the DeploymentLog                                                          | -                             | Yes, if deploymentlog enabled |
| `jeap.governance.deploymentlog.username`                             | Username to access the DeploymentLog                                              | -                             | Yes, if deploymentlog enabled |
| `jeap.governance.deploymentlog.password`                             | Password to access the DeploymentLog                                              | -                             | Yes, if deploymentlog enabled |
| `jeap.governance.deploymentlog.timeout`                              | DeploymentLog connection timeout duration.                                        | 'PT5M'                        | No                     |
| `jeap.governance.prometheus.enablede`                                | Enable/disable the import of time series from Prometheus                          | 'true'                        | No                     |
| `jeap.governance.prometheus.amp.host`                                | Amazon Managed Prometheus host URL                                                | -                             | Yes, if enabled        |
| `jeap.governance.prometheus.amp.workspace`                           | Amazon Managed Prometheus workspace id                                            | -                             | Yes, if enabled        |
| `jeap.governance.prometheus.amp.role-arn`                            | ARN of the role to assume for accessing the Amazon Managed Prometheus             | -                             | Yes, if enabled        |
| `jeap.governance.prometheus.amp.role-session-name`                   | Name of the session to be used for accessing the Amazon Managed Prometheus        | -                             | Yes, if enabled        |
| `jeap.governance.scoring.cron-expression`                            | Cron expression to schedule the rule evaluation and scoring of components/systems | `0 0,45 6-20 * * MON-FRI`     | No                     |
| `jeap.governance.scoring.lock-at-least`                              | Minimum duration for which the lock should be held during the data import job     | `PT1M`                        | No                     |
| `jeap.governance.scoring.lock-at-most`                               | Maximum lock duration for the data import job                                     | `PT15M`                       | No                     |

#### Example Configuration

```yaml
jeap:
  governance:
    environment: DEV
    dataimport:
      cron-expression: "0 15 6,10,14,18 * * MON-FRI"
      lock-at-least: PT30M
      lock-at-most: PT2H
    scoring:
      cron-expression: "0 0,45 6-20 * * MON-FRI"
      lock-at-least: PT1M
      lock-at-most: PT15M
    archrepo:
      url: https://archrepo.example.com
      timeout: PT5M
      import:
        apidocversion:
          enabled: true
        databaseschemaversion:
          enabled: true
        reactiongraph:
          enabled: false
        restapirelationwithoutpact:
          enabled: true
    deploymentlog:
      enabled: true
      url: https://deploymentlog.example.com
      username: deploymentlog_user
      password: securepassword
      timeout: PT15M
    prometheus:
      enabled: true
      amp:
        host: "https://aps-workspaces.eu-central-1.amazonaws.com"
        workspace: "ws-4f9f438a-efdf-4081-9745-fc4a0ad35f32b2"
        role-arn: "arn:aws:iam::892367255812:role/amp-read-assume-role"
        role-session-name: mySession
```

### Plugin Mechanism

The governance service provides several extension points, which are explained in this chapter.

#### Data Import

Instances of the governance service can collect their own data. To do so, the governance service offers an interface:

```java
public interface DataSourceImporter {

    /**
     * Imports data from the external data source into the governance system.
     */
    void importData();

}
```

**Implementation:**
1. Implement the `DataSourceImporter` interface
2. Add the implementation as a Spring bean
3. It will be automatically included in the data import process

**Example:**

```java
@Component
public class CustomDataImporter implements DataSourceImporter {
    
    @Override
    public void importData() {
        // Your custom import logic here
    }
}
```

#### Data Deletion

If you hold data in instances of the governance service that reference SystemComponents, you can be notified before a SystemComponent is deleted. This is necessary to perform certain data cleanup operations. You implement an interface for this:

```java
/**
 * Should be annotated with {@code org.springframework.core.annotation.Order} to define the sort order
 */
public interface ComponentDeletionListener {

    /**
     * This method is called before a component identified by {@code systemComponentId} is deleted.
     * Implementations should perform any necessary cleanup or data removal related to the component.
     *
     * @param systemComponentId the unique identifier of the component to be deleted
     */
    void preComponentDeletion(Long systemComponentId);

}
```

**Implementation:**
1. Implement the `ComponentDeletionListener` interface
2. Optionally annotate with `@Order` to control execution sequence
3. Add the implementation as a Spring bean
4. It will be automatically included in the deletion process

**Example:**

```java
@Component
public class CustomCleanupListener implements ComponentDeletionListener {
    
    @Override
    public void preComponentDeletion(Long systemComponentId) {
        // Cleanup logic before component deletion
    }
}
```

### Rules

The governance service provides an infrastructure to evaluate governance rules and score services/systems on a regular
basis. Besides using predefined rules, you may also provide custom rules specific to your context.

**Implementation:**
1. Implement the `Rule` interface (see [Rule.java](./jeap-governance-plugin-api/src/main/java/ch/admin/jeap/governance/plugin/api/rule/Rule.java)
2. Inject any necessary repositories into your implementation to access the data you need for the rule evaluation
3. Provide the rule as a Spring bean
4. It will be automatically be evaluated during the regular rule evaluation process

### Metrics

The governance service provides the following Prometheus-compatible metrics for monitoring the data import process.

| Metric                                                              | Description                                                                                                                                                                                                                                       | Labels                                                                                                                                                                                                                                        | Example                                                                                                                             |
|---------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| `jeap_governance_service_data_import_duration_seconds_count`        | **Counter** tracking the total number of data import executions per data source connector. Use this to track how many times each import has been executed and monitor success/failure rates.                                                      | • `data_source_connector`: Import connector type (`ArchRepoSystemImport`, `ApiDocVersionImport`, `DatabaseSchemaVersionImport`, `ReactionGraphImport`, `RestApiRelationWithoutPactImport`)<br>• `success`: Execution status (`true`, `false`) | `jeap_governance_service_data_import_duration_seconds_count{data_source_connector="ApiDocVersionImport",success="true"} 2`          |
| `jeap_governance_service_data_import_duration_seconds_sum`          | **Summary** tracking the cumulative duration (in seconds) of all data import executions per data source connector. Use this to calculate average import durations (sum/count) and monitor performance trends over time.                           | • `data_source_connector`: Import connector type (`ArchRepoSystemImport`, `ApiDocVersionImport`, `DatabaseSchemaVersionImport`, `ReactionGraphImport`, `RestApiRelationWithoutPactImport`)<br>• `success`: Execution status (`true`, `false`) | `jeap_governance_service_data_import_duration_seconds_sum{data_source_connector="ApiDocVersionImport",success="true"} 0.748848553`  |
| `jeap_governance_service_data_import_duration_seconds_max`          | **Gauge** tracking the maximum observed duration (in seconds) for data import executions within the current observation window. Use this to identify performance outliers and detect import operations that take significantly longer than usual. | • `data_source_connector`: Import connector type (`ArchRepoSystemImport`, `ApiDocVersionImport`, `DatabaseSchemaVersionImport`, `ReactionGraphImport`, `RestApiRelationWithoutPactImport`)<br>• `success`: Execution status (`true`, `false`) | `jeap_governance_service_data_import_duration_seconds_max{data_source_connector="ArchRepoSystemImport",success="true"} 2.118972172` |
| `jeap_governance_service_data_import_last_run_from_minutes`         | **Gauge** indicating how many minutes have elapsed since the last successful data import execution (across all import connectors). Use this for alerting to detect when data imports have not run for an unexpectedly long time.                  | None                                                                                                                                                                                                                                          | `jeap_governance_service_data_import_last_run_from_minutes 4.0`                                                                     |
| `jeap_governance_service_prometheus_queries_duration_seconds_count` | **Counter** The number of queries made to Prometheus                                                                                                                                                                                              | see @Timed annotation                                                                                                                                                                                                                         | `jeap_governance_service_prometheus_query_count 97395.00`                                                                           | 
| `jeap_governance_service_prometheus_queries_duration_seconds_sum`   | **Summary** The cumaltive duration in seconds of the queries made to Prometheus                                                                                                                                                                   | see @Timed annotation                                                                                                                                                                                                                         | `jeap_governance_service_prometheus_query_sum 32431.0`                                                                              |
| `jeap_governance_service_prometheus_queries_duration_seconds_max`   | **Gauge** The maximum observed duration in seconds of a query made to Prometheus                                                                                                                                                                  | see @Timed annotation                                                                                                                                                                                                                         | `jeap_governance_service_prometheus_query_max 7.3`                                                                                  |
| `jeap_governance_service_scoring_seconds_count`                     | **Counter** The number of scoring runs executed                                                                                                                                                                                                   | see @Timed annotation                                                                                                                                                                                                                         | `jeap_governance_service_prometheus_query_count 97395.00`                                                                           | 
| `jeap_governance_service_scoring_seconds_sum`                       | **Summary** The cumaltive duration in seconds of scoring runs executed                                                                                                                                                                            | see @Timed annotation                                                                                                                                                                                                                         | `jeap_governance_service_prometheus_query_sum 32431.0`                                                                              |
| `jeap_governance_service_scoring_seconds_max`                       | **Gauge** The maximum observed duration in seconds of a scoring run                                                                                                                                                                               | see @Timed annotation                                                                                                                                                                                                                         | `jeap_governance_service_prometheus_query_max 7.3`                                                                                  |


### Recommended Alerts

Based on these metrics, consider setting up the following alerts:

1. **Import Failures:**
```promql
   rate(jeap_governance_service_data_import_duration_seconds_count{success="false"}[5m]) > 0
```

2. **Import Not Running:**
```promql
   jeap_governance_service_data_import_last_run_from_minutes > 120
```

3. **Slow Imports:**
```promql
   jeap_governance_service_data_import_duration_seconds_max > 300
```

