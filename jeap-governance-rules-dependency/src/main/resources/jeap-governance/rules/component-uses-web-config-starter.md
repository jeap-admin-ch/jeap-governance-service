Checks that a self-contained system uses the `jeap-spring-boot-web-config-starter` dependency.

## Why

A self-contained system serves its own web frontend. The `jeap-spring-boot-web-config-starter` provides the standard jEAP web configuration for this, so that self-contained systems behave consistently and benefit from a centrally maintained configuration instead of individual, diverging setups.

## How the rule is evaluated

The rule only applies to self-contained systems; all other component types pass as not applicable.

The rule evaluates the metric `jeap_dependency_version` reported by the component and queried from Prometheus during the scheduled data import. The rule passes if the reported dependencies contain `jeap-spring-boot-web-config-starter` (with or without group id) and fails otherwise.

## How to comply

Add the `jeap-spring-boot-web-config-starter` dependency to the self-contained system and redeploy it. Make sure the component reports its dependency versions as a metric.

## Exemptions

The rule has no parameters. A component can be exempted from the rule (permanently or until a given date) by the governance service instance's configuration.
