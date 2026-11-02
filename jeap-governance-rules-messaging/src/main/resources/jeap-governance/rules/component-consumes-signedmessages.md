Checks that a component consuming messages enforces message signatures, i.e. only accepts messages that carry a valid signature.

## Why

Verifying message signatures ensures that consumed messages come from the expected, authorised producer and have not been tampered with. Signature verification only provides this protection if unsigned messages are rejected.

## How the rule is evaluated

The rule evaluates metrics reported by the jEAP messaging library and queried from Prometheus during the scheduled data import.

- If the component reports no messaging metrics (`jeap_messaging_total`), no messaging library is detected and the rule passes.
- If the component reports no `jeap_messaging_signature_required_state` metric, it is not a message consumer and the rule passes.
- If the metric reports that signatures are not required for consumption, the rule fails.
- Otherwise signature enforcement is enabled and the rule passes.

After signature enforcement has been disabled, the rule may only fail once the Prometheus query lookback window has elapsed; after enabling it, the rule passes as soon as the new value is reported.

## How to comply

- Use a version of the jEAP messaging library that supports message signing.
- Configure the component to require signatures for consumed messages, so that unsigned messages are rejected.

## Exemptions

The rule has no parameters. A component can be exempted from the rule (permanently or until a given date) by the governance service instance's configuration.

## See

- [jEAP Messaging Library - Sign Messages](https://jeap-admin-ch.github.io/docs/building-blocks/libraries/jeap-messaging/signing-messages)
