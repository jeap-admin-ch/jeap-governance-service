Checks that a component producing messages signs all messages it publishes.

## Why

Signed messages allow consumers to verify that a message comes from the expected, authorised producer and has not been tampered with. Consumers can only enforce signatures if all producers sign their messages.

## How the rule is evaluated

The rule evaluates the metric `jeap_messaging_total` reported by the jEAP messaging library and queried from Prometheus during the scheduled data import.

- If the component reports no messaging metrics, no messaging library is detected and the rule passes.
- If any message was published by the component as producer without a signature, the rule fails.
- Otherwise all messages are signed and the rule passes.

Components that only consume messages pass the rule.

## How to comply

- Use a version of the jEAP messaging library that supports message signing.
- Configure message signing for the component, so that every published message is signed.

## Exemptions

The rule has no parameters. A component can be exempted from the rule (permanently or until a given date) by the governance service instance's configuration.

## See

- [jEAP Messaging Library - Sign Messages](https://jeap-admin-ch.github.io/docs/building-blocks/libraries/jeap-messaging/signing-messages)
