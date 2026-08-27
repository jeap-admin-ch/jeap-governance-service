# Configuration

All configuration properties support Spring Boot's standard configuration mechanisms (application.yml,
environment variables, etc.).

| Property                                                             | Description                                                                                                                                            | Default                       | Required                         |
|----------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------|----------------------------------|
| `jeap.governance.environment`                                        | Environment of the service(DEV, REF, ABN, PROD).                                                                                                       | -                             | Yes                              |
| `jeap.governance.archrepo.url`                                       | URL of the Architecture Repository                                                                                                                     | -                             | Yes                              |
| `jeap.governance.dataimport.cron-expression`                         | Cron expression to schedule the data import job                                                                                                        | `0 15 6,10,14,18 * * MON-FRI` | No                               |
| `jeap.governance.dataimport.lock-at-least`                           | Minimum duration for which the lock should be held during the data import job                                                                          | `PT30M`                       | No                               |
| `jeap.governance.dataimport.lock-at-most`                            | Maximum lock duration for the data import job                                                                                                          | `PT2H`                        | No                               |
| `jeap.governance.archrepo.timeout`                                   | Connection timeout for Architecture Repository integration                                                                                             | `PT5M`                        | No                               |
| `jeap.governance.archrepo.import.apidocversion.enabled`              | Enable/disable import of API documentation versions from ArchRepo                                                                                      | `true`                        | No                               |
| `jeap.governance.archrepo.import.databaseschemaversion.enabled`      | Enable/disable import of database schema versions from ArchRepo                                                                                        | `true`                        | No                               |
| `jeap.governance.archrepo.import.restapirelationwithoutpact.enabled` | Enable/disable import of REST API relations without Pact from ArchRepo                                                                                 | `true`                        | No                               |
| `jeap.governance.deploymentlog.enabled`                              | Enable/disable import of data from the of DeploymentLog                                                                                                | `true`                        | No                               |
| `jeap.governance.deploymentlog.url`                                  | URL of the DeploymentLog                                                                                                                               | -                             | Yes, if deploymentlog enabled    |
| `jeap.governance.deploymentlog.username`                             | Username to access the DeploymentLog                                                                                                                   | -                             | Yes, if deploymentlog enabled    |
| `jeap.governance.deploymentlog.password`                             | Password to access the DeploymentLog                                                                                                                   | -                             | Yes, if deploymentlog enabled    |
| `jeap.governance.deploymentlog.timeout`                              | DeploymentLog connection timeout duration.                                                                                                             | 'PT5M'                        | No                               |
| `jeap.governance.prometheus.enablede`                                | Enable/disable the import of time series from Prometheus                                                                                               | 'true'                        | No                               |
| `jeap.governance.prometheus.amp.host`                                | Amazon Managed Prometheus host URL                                                                                                                     | -                             | Yes, if enabled                  |
| `jeap.governance.prometheus.amp.workspace`                           | Amazon Managed Prometheus workspace id                                                                                                                 | -                             | Yes, if enabled                  |
| `jeap.governance.prometheus.amp.role-arn`                            | ARN of the role to assume for accessing the Amazon Managed Prometheus                                                                                  | -                             | Yes, if enabled                  |
| `jeap.governance.prometheus.amp.role-session-name`                   | Name of the session to be used for accessing the Amazon Managed Prometheus                                                                             | -                             | Yes, if enabled                  |
| `jeap.governance.prometheus.amp.query-lookback`                      | How far back the Prometheus queries look for the most recent sample of a service                                                                       | 'PT6H'                        | No                               |
| `jeap.governance.reactionobserver.enabled`                           | Enable/disable import of data from the of ReactionObserver                                                                                             | `true`                        | No                               |
| `jeap.governance.reactionobserver.url`                               | URL of the ReactionObserver                                                                                                                            | -                             | Yes, if ReactionObserver enabled |
| `jeap.governance.reactionobserver.username`                          | Username to access the ReactionObserver                                                                                                                | -                             | Yes, if ReactionObserver enabled |
| `jeap.governance.reactionobserver.password`                          | Password to access the ReactionObserver                                                                                                                | -                             | Yes, if ReactionObserver enabled |
| `jeap.governance.reactionobserver.timeout`                           | ReactionObserver connection timeout duration.                                                                                                          | 'PT5M'                        | No                               |
| `jeap.governance.secscan.enabled`                                    | Enable/disable the security scan of known system component APIs                                                                                        | true                          | No                               |
| `jeap.governance.secscan.dataimport.target-environment`              | Environment to perform the security scan on                                                                                                            | REF                           | No                               |
| `jeap.governance.secscan.apidiscovery.url-template`                  | URL template of the API discovery service containing the parameters `{env}` and `{systemComponentName}`                                                | true                          | No                               |
| `jeap.governance.secscan.apidiscovery.timeout`                       | Timout (specified as duration) for the connect and read timeouts when accessing the API discovery service                                              | true                          | No                               |
| `jeap.governance.secscan.httpcheck.connect-timeout`                  | Timout (specified as duration) for the connect timeout when checking an HTTP endpoint                                                                  | true                          | No                               |
| `jeap.governance.secscan.httpcheck.read-timeout`                     | Timout (specified as duration) for the read timeout when checking an HTTP endpoint                                                                     | true                          | No                               |
| `jeap.governance.rules.active[].id`                                  | Rule identifier, must match a known rule implementation                                                                                                | -                             | Yes                              |
| `jeap.governance.rules.active[].weight`                              | Rule weight for scoring (positive integer, >= 1)                                                                                                       | -                             | Yes                              |
| `jeap.governance.rules.active[].documentation-link`                  | Optional link to the documentation for this rule, used in governance reports                                                                           | -                             | No                               |
| `jeap.governance.rules.active[].parameters`                          | Optional key-value parameters passed to the rule                                                                                                       | `{}`                          | No                               |
| `jeap.governance.rules.component-exemptions[].id`                    | Unique exemption identifier                                                                                                                            | -                             | Yes                              |
| `jeap.governance.rules.component-exemptions[].component-name`        | Name of the component this exemption applies to                                                                                                        | -                             | Yes                              |
| `jeap.governance.rules.component-exemptions[].rule-id`               | List of rule IDs this exemption covers                                                                                                                 | -                             | Yes                              |
| `jeap.governance.rules.component-exemptions[].reason`                | Explanation for the exemption                                                                                                                          | -                             | Yes                              |
| `jeap.governance.rules.component-exemptions[].until`                 | Expiry date (ISO-8601 `yyyy-MM-dd`). If absent, the exemption is permanent                                                                             | -                             | No                               |
| `jeap.governance.rules.component-exemptions[].parameters`            | Optional key-value parameters to further scope the exemption                                                                                           | `{}`                          | No                               |
| `jeap.governance.scoring.cron-expression`                            | Cron expression to schedule the rule evaluation and scoring of components/systems                                                                      | `0 0,45 6-20 * * MON-FRI`     | No                               |
| `jeap.governance.scoring.lock-at-least`                              | Minimum duration for which the lock should be held during the data import job                                                                          | `PT1M`                        | No                               |
| `jeap.governance.scoring.lock-at-most`                               | Maximum lock duration for the data import job                                                                                                          | `PT15M`                       | No                               |
| `jeap.governance.reporting.enabled`                                  | Enables or disables the reporting job                                                                                                                  | `false`                       | No                               |
| `jeap.governance.reporting.cron-expression`                          | Cron expression for the reporting job. Defaults to 55 minutes past the hour to avoid overlap with the data import job (:15) and scoring job (:00, :45) | `0 55 6-20 * * MON-FRI`       | No                               |
| `jeap.governance.reporting.orphancleanup.cron-expression`            | Cron expression for the orphan page cleanup job                                                                                                        | `0 10 3 * * MON-FRI`          | No                               |
| `jeap.governance.reporting.lock-at-least`                            | Minimum lock duration for the reporting job (ISO-8601 duration)                                                                                        | `PT1M`                        | No                               |
| `jeap.governance.reporting.lock-at-most`                             | Maximum lock duration for the reporting job (ISO-8601 duration)                                                                                        | `PT20M`                       | No                               |
| `jeap.governance.reporting.trendPeriodDays`                          | Number of days used to calculate the trend                                                                                                             | `30`                          | No                               |
| `jeap.governance.reporting.confluence.url`                           | Base URL of the Confluence instance                                                                                                                    | —                             | Yes, if reporting is enabled     |
| `jeap.governance.reporting.confluence.space-key`                     | Confluence space key where pages will be created or updated                                                                                            | —                             | Yes, if reporting is enabled     |
| `jeap.governance.reporting.confluence.root-page-name`                | Name of the root page under which reports are published                                                                                                | —                             | Yes, if reporting is enabled     |
| `jeap.governance.reporting.confluence.ancestor-id`                   | ID of the ancestor page under which reports are published                                                                                              | —                             | Yes, if reporting is enabled     |
| `jeap.governance.reporting.confluence.username`                      | Username for Confluence authentication                                                                                                                 | —                             | Yes, if reporting is enabled     |
| `jeap.governance.reporting.confluence.password`                      | Password for Confluence authentication                                                                                                                 | —                             | Yes, if reporting is enabled     |

## Prometheus Query Lookback

All Prometheus queries return the most recent sample a service exported within the lookback window configured with
`jeap.governance.prometheus.amp.query-lookback` (default six hours).

The window is not a memory: once a service stops exporting a time series, that series drops out of the query results
after the window has elapsed, and the rules based on Prometheus data no longer see it. The window only tolerates a
service not being scraped for a while, for example while it is being redeployed.

This has two consequences when choosing the value:

- A shorter window makes the rules reflect a changed service configuration sooner - at the next data import rather than
  after the window has elapsed. For example, an endpoint that is no longer exposed disappears from the
  `endpoints-protected-by-jwt` rule once the redeployed service has stopped exporting the corresponding metric for
  longer than the window. Note that the metric value alone does not tell the rule whether a violation is still current:
  the counter behind `jeap_rest_endpoint_without_jwt_total` keeps its last value for as long as the service runs.
- A longer window keeps rules from failing for a service that is not permanently running or scraped. If components are
  regularly unavailable at data import time, either raise the lookback or configure a `violation-delay` for the
  affected rules (see [Rules](rules.md)).

## Example Configuration

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
        restapirelationwithoutpact:
          enabled: true
    deploymentlog:
      enabled: true
      url: https://deploymentlog.example.com
      username: deploymentlog_user
      password: securepassword
      timeout: PT15M
    reactionobserver:
      enabled: true
      url: https://reactionobserver.example.com
      username: reactionobserver_user
      password: securepassword
      timeout: PT15M
    prometheus:
      enabled: true
      amp:
        host: "https://aps-workspaces.eu-central-1.amazonaws.com"
        workspace: "ws-4f9f438a-efdf-4081-9745-fc4a0ad35f32b2"
        role-arn: "arn:aws:iam::892367255812:role/amp-read-assume-role"
        role-session-name: mySession
    secscan:
        enabled: true
        dataimport:
          target-environment: REF
        apidiscovery:
          url-template: "https://api-discovery.{env}-example.com/apis/{systemComponentName}"
    reporting:
      enabled: true
      cron-expression: "0 55 6-20 * * MON-FRI"
      orphancleanup:
        cron-expression: "0 10 3 * * MON-FRI"
      lock-at-least: PT1M
      lock-at-most: PT20M
      trendPeriodDays: 30
      confluence:
        url: https://confluence.example.com
        space-key: GOV
        root-page-name: Governance Reports
        username: confluence_user
        password: securepassword
    rules:
      active:
        - id: component-name
          weight: 10
        - id: enforce-oauth2
          weight: 10
          documentation-link: https://wiki.example.com/enforce-oauth2
        - id: custom-rule
          weight: 5
          parameters:
            threshold: 10
      component-exemptions:
        - id: my-exemption
          component-name: my-system-legacy-service
          rule-id:
            - enforce-oauth2
          reason: "Legacy service, migration planned"
          until: "2026-12-31"
```

## See also

- [Architecture](architecture.md) — modules, domain model and database schema.
- [Rules](rules.md) — the built-in governance rules and their parameters.
- [Reporting](reporting.md) — Confluence report generation.
