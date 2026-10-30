Checks that a messaging component is observed by the Reaction Observer, i.e. that reactions of the component to messages have been observed recently.

## Why

The Reaction Observer records how components react to the messages they receive. The resulting reaction graphs document the actual behaviour of message-driven processes and help analyse their flow and detect anomalies. They are only meaningful if every messaging component is observed.

## How the rule is evaluated

The rule only applies to backend services and self-contained systems; other component types pass as not applicable.

1. If the component is listed in the `ignored-service-names` parameter, the rule passes.
2. If Prometheus reports no messaging metrics (`jeap_messaging_total`) for the component, no messaging library is detected and the rule passes.
3. If the Reaction Observer has no reaction graph for the component, the rule fails.
4. If the last observation of the component is older than the allowed delay (7 days by default), the rule fails.
5. Otherwise the rule passes; the rule comment shows the last observation date.

The last observation date of each component is imported from the Reaction Observer during the scheduled data import.

## How to comply

- Configure the component so that its message reactions are reported to the Reaction Observer.
- Make sure the component processes messages regularly in the evaluated environment, so that reactions are observed within the allowed delay.

## Parameters

- `observation-max-delay-in-days`: maximum age in days of the last observed reaction. Defaults to `7`.
- `ignored-service-names`: list of component names for which the rule is skipped. A component is skipped if its name contains one of the listed values.

## Exemptions

Besides the `ignored-service-names` parameter, a component can be exempted from the rule as a whole (permanently or until a given date) by the governance service instance's configuration.
