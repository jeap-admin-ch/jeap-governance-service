Checks that a component using messaging enforces messaging contracts, i.e. only publishes and consumes messages for which a contract is defined, and uses contracts from the master branch.

## Why

Messaging contracts document which component produces and consumes which message types and versions. They make the dependencies between components visible and allow detecting incompatible changes before they break a consumer. Contracts are only reliable if the component cannot bypass them.

## How the rule is evaluated

The rule evaluates the metric `jeap_messaging_contract` reported by the jEAP messaging library and queried from Prometheus during the scheduled data import. If the metric is not present, no messaging library is detected and the rule passes. Otherwise the rule fails if any of the following contract validation switches is enabled:

- `noMasterContracts`: contracts are loaded from a branch other than master.
- `consumeWithoutContract`: consuming messages without a contract is allowed.
- `publishWithoutContract`: publishing messages without a contract is allowed.
- `silentIgnoreWithoutContract`: messages without a contract are silently ignored.

## How to comply

- Define a contract for every message type the component publishes or consumes.
- Use contracts from the master branch.
- Do not enable any of the switches listed above in the deployed component's configuration.

## Exemptions

The rule has no parameters. A component can be exempted from the rule (permanently or until a given date) by the governance service instance's configuration.
