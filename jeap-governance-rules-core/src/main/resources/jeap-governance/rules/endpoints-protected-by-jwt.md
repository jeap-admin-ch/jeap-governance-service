Checks that the REST endpoints of a component are protected by a JWT bearer token, based on the metric `jeap_rest_endpoint_without_jwt_total` that the component reports when one of its endpoints is called without a JWT bearer token.

## Why

Unprotected endpoints can expose data or functionality to unauthenticated callers. Detecting such calls in the running system reveals endpoints that are accidentally left open.

## How the rule is evaluated

During the scheduled data import, the governance service queries Prometheus for the metric `jeap_rest_endpoint_without_jwt_total` of each component (responses with status `404` are ignored). The rule fails if a call without JWT bearer token is reported for an endpoint that is not exempted; the rule comment lists each such endpoint (method and path) together with the environments where it was detected. If no such call is reported, the rule passes.

An endpoint is reported as long as the component exports the metric for it. After an endpoint has been fixed or removed, it disappears once the component has been redeployed and the Prometheus query lookback window has elapsed.

The following endpoints are ignored by default:

- Actuator endpoints (path containing `/actuator`)
- API docs and Swagger UI (paths starting with `/api-docs` or `/swagger-ui`), on the `ref` environment only
- Frontend wildcard endpoints (`/**` and `/`)
- Configuration API calls, e.g. `/api/configuration` or `/ui-api/app-configuration`

## How to comply

- Require a valid JWT bearer token for all REST endpoints of the component, e.g. using the jEAP security configuration.
- If an endpoint must be public by design, ask for it to be exempted using the parameters below.

## Parameters

The instance can configure the following parameters. Paths and component names support a single `*` wildcard at the start (suffix match, e.g. `*-service`) or at the end (prefix match, e.g. `/api/*`).

- `exempt-component-names`: components that are not checked at all.
- `exempt-methods`: HTTP methods that are ignored, e.g. `OPTIONS` (case-insensitive).
- `exempt-paths`: endpoint paths that are ignored.
- `exempt-endpoints`: method and path pairs in the form `METHOD:PATH` that are ignored, e.g. `GET:/api/public/*`.
- `disable-default-exemptions`: set to `true` to also check the endpoints that are ignored by default.

## Exemptions

Besides the parameters above, a component can be exempted from the rule as a whole (permanently or until a given date) by the governance service instance's configuration.
