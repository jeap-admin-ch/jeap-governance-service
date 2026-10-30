Checks that every REST API relation in which the component calls a provider of another system is backed by a consumer-driven contract (PACT).

## Why

Consumer-driven contracts make the expectations of a consumer on a provider's API explicit and testable. The provider can verify the contracts before a release and detect changes that would break a consumer before they are deployed, instead of discovering them in a running environment.

## How the rule is evaluated

During the scheduled data import, the governance service imports from the architecture repository the REST API relations that are not covered by a PACT contract. The rule looks at the relations where the component is the consumer and the provider belongs to a different system. Relations within the same system are checked by the rule `component-cdc-contractwithinsystem`.

The rule fails with one message per relation without a contract, naming the provider and the called method and path. If there is no such relation, the rule passes.

## How to comply

- Write a PACT consumer test for each REST API the component calls, and publish the resulting contract.
- Make sure the provider verifies the contract, so that the architecture repository recognises the relation as covered.

## Parameters

The instance can configure the following parameters:

- `services-to-ignore`: list of component name fragments; relations where the consumer or the provider name contains one of them are ignored.
- `relations-to-ignore`: list of relations in the form `METHOD path`, e.g. `GET /foo/bar`, that are ignored regardless of the components involved. The value must match the relation's method and path exactly.

## Exemptions

Besides the parameters above, a component can be exempted from the rule as a whole (permanently or until a given date) by the governance service instance's configuration.
