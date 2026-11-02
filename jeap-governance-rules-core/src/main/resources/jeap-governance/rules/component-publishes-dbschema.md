Checks that a component using a database publishes its database schema to the architecture repository, and that the published schema belongs to the currently deployed version of the component.

## Why

A published, up-to-date database schema documents the data a component stores. It makes data structures visible to architects, data protection and other teams without access to the database, and keeps the documentation in sync with what is actually deployed.

## How the rule is evaluated

The rule only applies to backend services and self-contained systems; other component types pass as not applicable.

1. If the component is listed in the `ignored-service-names` parameter, the rule passes.
2. If Prometheus reports no active JDBC connections (metric `jdbc_connections_active`) for the component, it is assumed to use no database and the rule passes.
3. If no database schema for the component is found in the architecture repository, the rule fails.
4. If the deployment log knows the deployed version of the component, the version of the published schema must be equal to it; otherwise the rule fails.

## How to comply

- Publish the database schema to the architecture repository as part of the application, so that each deployed version uploads its schema.
- Make sure the published schema version matches the version of the deployed component.

## Parameters

- `ignored-service-names`: list of component names for which the rule is skipped, e.g. components that do not need to publish a schema. A component is skipped if its name contains one of the listed values.

## Exemptions

Besides the `ignored-service-names` parameter, a component can be exempted from the rule as a whole (permanently or until a given date) by the governance service instance's configuration.

## See

- [jEAP DB Schema Publisher](https://jeap-admin-ch.github.io/docs/building-blocks/libraries/jeap-db-schema-publisher/)
