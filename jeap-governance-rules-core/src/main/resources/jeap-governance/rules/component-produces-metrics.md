Checks that metrics of the component are available in Prometheus, i.e. that at least one Prometheus time series exists for the component.

## Why

Metrics are the basis for operating a component: they enable dashboards and alerting. Several other governance rules also evaluate metrics reported by the component, so they can only assess a component whose metrics are available.

## How the rule is evaluated

During the scheduled data import, the governance service queries Prometheus for the standard jEAP metrics of each component. The rule passes if at least one time series was found for the component and fails otherwise. The rule is evaluated for all component types.

## How to comply

- Add the `jeap-spring-boot-monitoring-starter` dependency to the component.
- Make sure the monitoring configuration works, i.e. the metrics are scraped and stored in the Prometheus instance the governance service queries, under the component's name as the `service` label.
- Make sure the component is deployed and running in the environment the governance service evaluates.

## Exemptions

The rule has no parameters. A component that cannot produce metrics can be exempted from the rule (permanently or until a given date) by the governance service instance's configuration.
