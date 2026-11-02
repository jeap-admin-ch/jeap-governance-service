Checks that a component publishes its OpenAPI specification to the architecture repository, and that the published specification belongs to the currently deployed version of the component.

## Why

A published, up-to-date OpenAPI specification documents the REST API of a component. It enables consumers to discover and integrate the API, and allows the API landscape and the relations between components to be analysed.

## How the rule is evaluated

The rule only applies to backend services and self-contained systems; other component types pass as not applicable.

1. If no OpenAPI specification for the component is found in the architecture repository, the rule fails.
2. If the deployment log knows the deployed version of the component, the version of the published specification must be equal to it; otherwise the rule fails.
3. Otherwise the rule passes.

## How to comply

- Publish the OpenAPI specification to the architecture repository as part of the application, so that each deployed version uploads its specification.
- Make sure the published specification version matches the version of the deployed component.

## Exemptions

The rule has no parameters. A component that does not provide a REST API can be exempted from the rule (permanently or until a given date) by the governance service instance's configuration.

## See

- [jEAP OpenAPI Specification Publisher](https://jeap-admin-ch.github.io/docs/building-blocks/spring-boot-starters/jeap-open-api-publisher-starter/)
