Checks that a component uses at least the minimum versions of the dependencies (libraries) configured for this rule.

## Why

Outdated libraries miss security fixes, bug fixes and features that other components and the platform rely on. Defining minimum versions keeps components on supported versions and makes lagging components visible.

## How the rule is evaluated

The rule evaluates the metric `jeap_dependency_version` reported by the component and queried from Prometheus during the scheduled data import. It lists the dependencies the component uses together with their versions.

The following dependencies are checked:
- jeap-spring-boot-application-starter
- jeap-messaging
- jeap-crypto
- spring-boot
- jeap-error-handling-service
- jeap-process-context-scs
- jeap-process-archive-service

For each dependency configured with a minimum version:

- If the component does not use the dependency, it is not checked.
- If the used version is equal to or higher than the minimum version, the dependency is compliant.
- If the used version is lower than the minimum version, the rule fails.
- If the used version is a `-SNAPSHOT` version or cannot be parsed, the rule fails.

Versions are compared by major, minor and patch number. The rule comment lists the result for each checked dependency. If no checked dependency is used, the rule passes.

## How to comply

Update the dependencies listed in the rule comment to at least the required minimum version and redeploy the component. Do not deploy snapshot versions.

## Parameters

- `versions`: list of minimum versions, each in the form `groupId:artifactId:minimumVersion`, or `name:minimumVersion` for dependencies reported without a group id.

## Exemptions

A component can be exempted from the rule as a whole (permanently or until a given date) by the governance service instance's configuration.
