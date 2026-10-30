Checks that the message contracts of the deployed component use the latest version of each message type it produces or consumes.

## Why

Message types evolve. Components that stay on old message versions block the retirement of these versions, increase the effort for producers that must keep supporting them, and miss new content. Keeping contracts on the latest version keeps the messaging landscape maintainable.

## How the rule is evaluated

The rule only applies to backend services and self-contained systems; other component types pass as not applicable.

During the scheduled data import, the governance service imports from the Message Contract Service, for the environment configured by the instance, the version status of each deployed message contract: the message version used by the contract compared with the latest version of the message type in its message type registry.

- The rule fails if at least one contract of the component uses an outdated version. The rule comment lists each outdated contract with message type, role, topic, used version and latest version.
- Contracts whose latest version cannot be determined (e.g. because the message type is missing from the registry) are listed in the rule comment but do not make the rule fail.
- If no version status data is available for the component, the rule passes.

The instance can configure a grace period (violation delay) for this rule: during the grace period, an outdated contract is reported in the rule comment but the component is still considered compliant. Once the grace period has expired, it counts as a failure. A compliant evaluation resets the grace period.

## How to comply

- Update the message contracts of the component to the latest version of each message type it produces or consumes, and redeploy the component.
- For contracts listed with an unknown latest version, make sure the message type exists in its message type registry.

## Exemptions

The rule has no parameters. A component can be exempted from the rule (permanently or until a given date) by the governance service instance's configuration.
