Checks that the REST endpoints of a component are protected, by actively calling them without credentials and flagging endpoints that do not reject the request.

## Why

Unprotected endpoints can expose data or functionality to unauthenticated callers. Unlike the monitoring-based rule `endpoints-protected-by-jwt`, this rule does not depend on the endpoints actually being called in the running system: it also finds open endpoints that nobody has called yet.

## How the rule is evaluated

The rule works in two phases:

1. **Scan (data import):** the governance service discovers the HTTP API of each component in the target environment configured by the instance, and sends an unauthenticated request to each endpoint, using `test` as value for path variables. An endpoint is flagged if it answers with a status other than `401`, `403`, `502`, `503` or `504`. Endpoints that cannot be reached at all are not flagged. A component is rescanned only when its API has changed since the last scan.
2. **Evaluation:** the rule fails if flagged endpoints exist that are not exempted; the rule comment lists each of them with the observed status. The rule also fails if no scan data exists for the component, e.g. because its scan failed. If nothing is flagged, or the component has no API to scan, the rule passes.

The following endpoints are ignored by default:

- Actuator endpoints (path containing `/actuator`)
- API docs and Swagger UI (paths starting with `/api-docs` or `/swagger-ui`), on the `ref` environment only
- Frontend wildcard endpoints (`/**` and `/`)
- Configuration API calls, e.g. `/api/configuration` or `/ui-api/app-configuration`

## How to comply

- Require authentication for all REST endpoints, so that unauthenticated requests are rejected with `401` or `403`.
- Make sure the published API description of the component is up to date, so that removed endpoints are no longer scanned.
- If an endpoint must be public by design, ask for it to be exempted using the parameters below.

## Parameters

The instance can configure the following parameters. Paths and component names support a single `*` wildcard at the start (suffix match, e.g. `*-service`) or at the end (prefix match, e.g. `/api/*`).

- `exempt-component-names`: components that are not checked at all.
- `exempt-environments`: environments whose APIs are not scanned (case-insensitive).
- `exempt-api-url-not-containing`: an API is not scanned if its URL contains none of the listed values.
- `exempt-api-url-containing`: an API is not scanned if its URL contains one of the listed values.
- `exempt-methods`: HTTP methods that are ignored, e.g. `OPTIONS` (case-insensitive).
- `exempt-paths`: endpoint paths that are ignored.
- `exempt-endpoints`: method and path pairs in the form `METHOD:PATH` that are ignored, e.g. `GET:/api/public/*`.
- `disable-default-exemptions`: set to `true` to also check the endpoints that are ignored by default.

## Exemptions

Besides the parameters above, a component can be exempted from the rule as a whole (permanently or until a given date) by the governance service instance's configuration. An exempted component's endpoints are not scanned.
